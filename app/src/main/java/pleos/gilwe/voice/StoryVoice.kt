package pleos.gilwe.voice

import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import ai.pleos.playground.tts.TextToSpeech as PleosTts

/** 지금 재생 중인 낭독 (자막·진행 막대용) */
data class Playback(
    val id: String,
    val lines: List<String>,
    val index: Int,
    val elapsedMs: Int,
    val totalMs: Int,
    val paused: Boolean,
) {
    val fraction get() = if (totalMs <= 0) 0f else (elapsedMs.toFloat() / totalMs).coerceIn(0f, 1f)
    val line get() = lines.getOrNull(index).orEmpty()
    val nextLine get() = lines.getOrNull(index + 1)
}

/**
 * 이야기 낭독.
 * 1순위: 미리 만든 음성 파일 (assets/voice, 한국어·영어) — 문장 단위로 재생
 * 2순위: Pleos TextToSpeech SDK (ON_DEVICE) — 녹음이 없는 언어
 * 3순위: Android 표준 TextToSpeech
 *
 * 내비 안내 우선 (Figma 04B): 재생할 때 오디오 포커스를 잠깐(transient) 요청하고,
 * 내비 안내가 포커스를 가져가면 멈춘 뒤 [waitingForNavi] 를 켠다. 포커스가 돌아오면 멈춘 문장의 처음부터 다시 읽는다.
 */
class StoryVoice(context: Context) {

    private val ctx = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    val clips = VoiceClips(ctx)

    private val _speakingId = MutableStateFlow<String?>(null)
    /** 지금 읽고 있는(또는 잠시 멈춘) 낭독 id (이야기 id 또는 "intro:코스id") */
    val speakingId: StateFlow<String?> = _speakingId.asStateFlow()

    private val _playback = MutableStateFlow<Playback?>(null)
    val playback: StateFlow<Playback?> = _playback.asStateFlow()

    private val _waitingForNavi = MutableStateFlow(false)
    /** 내비 안내가 말하는 중이라 이야기를 멈춰 둔 상태 (Figma 04B) */
    val waitingForNavi: StateFlow<Boolean> = _waitingForNavi.asStateFlow()

    /** 이야기 낭독 오디오: 내비 안내(USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)와 구분되는 미디어 음성 */
    private val storyAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    // ---------- 오디오 포커스 ----------

    private val audio = ctx.getSystemService(AudioManager::class.java)
    private var focusHeld = false
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        main.post {
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    focusHeld = true
                    if (_waitingForNavi.value) { _waitingForNavi.value = false; restartSentence() }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // 내비 안내 등: 멈추고 기다렸다가 같은 문장부터
                    if (_playback.value != null || ttsBusy) { pauseEngines(); _waitingForNavi.value = true }
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    focusHeld = false
                    if (_playback.value != null) { pauseEngines(); _playback.value = _playback.value?.copy(paused = true) }
                }
            }
        }
    }
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(storyAttrs)
        .setWillPauseWhenDucked(true)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(focusListener)
        .build()

    /** @return true 면 바로 재생해도 됨, false 면 포커스가 돌아올 때까지 대기 */
    private fun requestFocus(): Boolean {
        if (focusHeld) return true
        return when (runCatching { audio.requestAudioFocus(focusRequest) }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> { focusHeld = true; true }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> { _waitingForNavi.value = true; false }
            else -> { _waitingForNavi.value = true; false }
        }
    }

    private fun abandonFocus() {
        if (!focusHeld) return
        runCatching { audio.abandonAudioFocusRequest(focusRequest) }
        focusHeld = false
        _waitingForNavi.value = false
    }

    // ---------- 1순위: 음성 파일 ----------

    private var player: MediaPlayer? = null
    private var fileClips: List<Clip> = emptyList()

    private val ticker = object : Runnable {
        override fun run() {
            val pb = _playback.value ?: return
            val pos = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
            val before = fileClips.take(pb.index).sumOf { it.ms }
            _playback.value = pb.copy(elapsedMs = (before + pos).coerceAtMost(pb.totalMs))
            main.postDelayed(this, 200)
        }
    }

    private fun playClip(index: Int) {
        releasePlayer()
        val pb = _playback.value ?: return
        val clip = fileClips.getOrNull(index)
        if (clip == null) { finished(pb.id); return }
        _playback.value = pb.copy(index = index, elapsedMs = fileClips.take(index).sumOf { it.ms }, paused = false)
        if (!requestFocus()) return // 포커스가 오면 restartSentence()
        runCatching {
            val afd = ctx.assets.openFd("voice/${clip.file}")
            player = MediaPlayer().apply {
                setAudioAttributes(storyAttrs)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnCompletionListener { main.post { if (_playback.value?.id == pb.id) playClip(index + 1) } }
                setOnErrorListener { _, w, e -> Log.w(TAG, "clip error $w/$e"); main.post { playClip(index + 1) }; true }
                prepare()
                start()
            }
            main.removeCallbacks(ticker); main.post(ticker)
        }.onFailure { Log.w(TAG, "clip play failed ${clip.file}", it); main.post { playClip(index + 1) } }
    }

    private fun releasePlayer() {
        main.removeCallbacks(ticker)
        player?.let { p -> runCatching { p.stop() }; runCatching { p.release() } }
        player = null
    }

    private fun finished(id: String) {
        releasePlayer()
        if (_speakingId.value == id) _speakingId.value = null
        _playback.value = null
        abandonFocus()
    }

    // ---------- 2·3순위: TTS ----------

    private enum class Engine { CONNECTING, PLEOS, ANDROID, NONE }
    @Volatile private var engine = Engine.CONNECTING
    private var pending: Triple<String, String, Locale>? = null
    @Volatile private var pleosCurrent: String? = null
    private var ttsBusy = false
    private var ttsLast: Triple<String, String, Locale>? = null

    private val pleos: PleosTts? = runCatching { PleosTts(ctx, Mode.ON_DEVICE) }
        .onFailure { Log.w(TAG, "Pleos TTS create failed", it) }.getOrNull()

    private val pleosListener = object : EventListener {
        override fun onReady() {}
        override fun onStart() { pleosCurrent?.let { _speakingId.value = it } }
        override fun onDone() = main.post { ttsFinished() }.let {}
        override fun onStop() {}
        override fun onError(errorType: ErrorType) { Log.w(TAG, "Pleos TTS error $errorType"); main.post { ttsFinished() } }
        override fun onUpdatedRms(rms: Double) {}
    }

    private fun ttsFinished() {
        if (_waitingForNavi.value) return // 내비 때문에 멈춘 것: 포커스가 오면 다시 읽는다
        if (_speakingId.value == pleosCurrent || pleosCurrent == null) _speakingId.value = null
        pleosCurrent = null
        ttsBusy = false
        abandonFocus()
    }

    private var android: TextToSpeech? = null

    private fun startAndroidFallback() {
        android = TextToSpeech(ctx) { status ->
            Log.i(TAG, "Android TTS init status=$status")
            engine = if (status == TextToSpeech.SUCCESS) Engine.ANDROID else Engine.NONE
            onEngineSettled()
        }.apply {
            setAudioAttributes(storyAttrs)
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { _speakingId.value = utteranceId }
                override fun onDone(utteranceId: String?) { main.post { if (_speakingId.value == utteranceId) ttsFinished() } }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { main.post { if (_speakingId.value == utteranceId) ttsFinished() } }
            })
        }
    }

    init {
        val p = pleos
        if (p == null) startAndroidFallback()
        else runCatching {
            p.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    Log.i(TAG, "Pleos TTS ready (ON_DEVICE)")
                    runCatching { p.addEventListener(pleosListener); p.setAudioAttributes(storyAttrs) }
                    engine = Engine.PLEOS
                    main.post { onEngineSettled() }
                }
                override fun onFailure(errorType: ErrorType) {
                    Log.w(TAG, "Pleos TTS init failed $errorType → Android TTS")
                    main.post { startAndroidFallback() }
                }
            })
        }.onFailure { Log.w(TAG, "Pleos TTS init threw", it); startAndroidFallback() }
    }

    private fun onEngineSettled() {
        if (engine == Engine.NONE) { pending = null; if (_playback.value == null) _speakingId.value = null; return }
        pending?.let { (id, text, loc) -> pending = null; speakTts(id, text, loc) }
    }

    private fun speakTts(id: String, text: String, locale: Locale) {
        ttsLast = Triple(id, text, locale)
        _speakingId.value = id
        when (engine) {
            Engine.CONNECTING -> pending = Triple(id, text, locale)
            Engine.PLEOS -> {
                ttsBusy = true
                if (!requestFocus()) return
                runCatching {
                    val p = pleos!!
                    pleosCurrent = id
                    p.setLocale(locale)
                    p.speak(text, true)
                }.onFailure { Log.w(TAG, "Pleos speak failed", it); ttsFinished() }
            }
            Engine.ANDROID -> {
                ttsBusy = true
                if (!requestFocus()) return
                runCatching {
                    val t = android!!
                    t.language = if (t.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) locale else Locale.US
                    t.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
                }.onFailure { Log.w(TAG, "speak failed", it); ttsFinished() }
            }
            Engine.NONE -> _speakingId.value = null
        }
    }

    // ---------- 공개 API ----------

    /**
     * 낭독 시작. 녹음 파일이 있으면 그걸, 없으면 TTS 로 [text] 를 읽는다.
     * @param startLine 녹음 파일 재생 시 시작할 문장 (다시 듣기에서 앞뒤 이동)
     */
    fun speak(id: String, text: String, locale: Locale, startLine: Int = 0) {
        stopEngines()
        val files = clips.clips(id, locale.language)
        _speakingId.value = id
        if (!files.isNullOrEmpty()) {
            fileClips = files
            _playback.value = Playback(id, files.map { it.text }, startLine, 0, files.sumOf { it.ms }, paused = false)
            playClip(startLine.coerceIn(0, files.lastIndex))
        } else {
            _playback.value = null
            speakTts(id, text, locale)
        }
    }

    /** 일시정지 (다시 들을 때는 멈춘 문장 처음부터) */
    fun pause() {
        val pb = _playback.value
        if (pb != null) { releasePlayer(); _playback.value = pb.copy(paused = true) } else stopTtsOnly()
        abandonFocus()
    }

    fun resume() {
        val pb = _playback.value
        if (pb != null) playClip(pb.index) else ttsLast?.let { (id, t, l) -> speakTts(id, t, l) }
    }

    /** 앞뒤 문장으로 이동 (다시 듣기 화면의 -10초 / +10초 버튼) */
    fun skipLine(delta: Int) {
        val pb = _playback.value ?: return
        val i = (pb.index + delta).coerceIn(0, pb.lines.lastIndex)
        playClip(i)
    }

    private fun restartSentence() {
        val pb = _playback.value
        if (pb != null) playClip(pb.index) else ttsLast?.let { (id, t, l) -> if (_speakingId.value == id) speakTts(id, t, l) }
    }

    private fun pauseEngines() {
        releasePlayer()
        stopTtsOnly(keepId = true)
    }

    private fun stopTtsOnly(keepId: Boolean = false) {
        if (pleosCurrent != null) runCatching { pleos?.stop() }
        runCatching { android?.stop() }
        if (!keepId) { pleosCurrent = null; ttsBusy = false }
    }

    private fun stopEngines() {
        pending = null
        releasePlayer()
        stopTtsOnly()
        _playback.value = null
    }

    fun stop() {
        stopEngines()
        _speakingId.value = null
        abandonFocus()
    }

    fun shutdown() {
        stop()
        runCatching { pleos?.removeEventListener(pleosListener); pleos?.release() }
        runCatching { android?.shutdown() }
    }

    private companion object { const val TAG = "StoryVoice" }
}
