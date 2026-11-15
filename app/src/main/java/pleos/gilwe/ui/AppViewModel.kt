package pleos.gilwe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pleos.gilwe.GilweApp
import pleos.gilwe.data.Catalog
import pleos.gilwe.data.Category
import pleos.gilwe.data.Course
import pleos.gilwe.data.Extras
import pleos.gilwe.data.Lang
import pleos.gilwe.data.StampRecord
import pleos.gilwe.data.StartPoint
import pleos.gilwe.data.Story
import pleos.gilwe.data.StoryScheduler
import pleos.gilwe.navi.NaviEvent
import pleos.gilwe.navi.PleosNavi
import pleos.gilwe.vehicle.GearState

sealed interface Screen {
    data object Intro : Screen
    data class ScreenLanguage(val returnTo: Screen) : Screen
    data object Home : Screen
    data class StartSearch(val showResults: Boolean) : Screen
    data class RoutePreview(val courseId: String) : Screen
    data class AudioLanguage(val courseId: String) : Screen
    data object Drive : Screen
    data object Park : Screen
    data class StampCollected(val courseId: String) : Screen
    data object MyStamps : Screen
    data class RouteStories(val courseId: String) : Screen
    /** 06 이야기 듣기 · 전문 */
    data class StoryListen(val storyId: String) : Screen
    data class StoryFull(val storyId: String) : Screen
    /** 10 주변 장소 둘러보기 */
    data class Nearby(val courseId: String) : Screen
}

/** 진행 중인 경로 안내 */
data class Trip(
    val course: Course,
    val start: StartPoint,
    /** 0~1 경로 진행률 */
    val progress: Float = 0f,
    /** 지금 재생 중(또는 마지막으로 재생된) 이야기 index, 아직 없으면 -1 */
    val storyIndex: Int = -1,
    /** 현재 이야기 진행률 0~1 */
    val storyProgress: Float = 0f,
    val storyPaused: Boolean = false,
    /** 기어를 읽을 수 있고 아직 D단이 아니라 대기 중 */
    val waitingForDrive: Boolean = false,
    /** 기본 내비에 경로 안내를 요청했음 (NaviHelper 연결됨) */
    val naviGuided: Boolean = false,
    /** 내비가 알려 준 남은 거리(m)·시간(초). 있으면 화면에 이 값을 보여 준다 */
    val naviRemainMeters: Int? = null,
    val naviRemainSeconds: Int? = null,
    /** 내비 안내 음성 때문에 이야기를 멈춘 상태 (Figma 04B) */
    val waitingForNavi: Boolean = false,
    /** 내비 위치 신호가 끊겨 시간 기준으로 진행 중 (Figma 04C) */
    val locationLost: Boolean = false,
    /** 위치 × 시간 조립 모드: 지금 이야기 id (자유·마무리 블록 포함), 들은 이야기, 다음 장소 */
    val storyId: String? = null,
    val played: Set<String> = emptySet(),
    val nextId: String? = null,
    /** 위치 신호가 끊겨 마지막 속도로 추정 중 (터널) */
    val deadReckoning: Boolean = false,
) {
    private fun find(id: String): Story? = course.stories.firstOrNull { it.id == id }
        ?: Extras.outroStory(course)?.takeIf { it.id == id }
        ?: Extras.introStory(course).takeIf { it.id == id }
    val totalMinutes get() = (course.minutesFromSeoul + start.minutesDelta).coerceAtLeast(5)
    val totalKm get() = (course.kmFromSeoul + start.kmDelta).coerceAtLeast(2)
    val remainingMinutes get() = naviRemainSeconds?.let { (it + 59) / 60 } ?: (totalMinutes * (1f - progress)).toInt().coerceAtLeast(if (progress < 1f) 1 else 0)
    val remainingKm get() = naviRemainMeters?.let { it / 1000f } ?: totalKm * (1f - progress)
    val currentStory: Story? get() = storyId?.let { find(it) } ?: course.stories.getOrNull(storyIndex)
    val nextStory: Story? get() = nextId?.let { find(it) }
        // 위치 × 시간 모드: 아직 안 들은 이야기 (다 들었으면 없음). 진행률 모드: 목록의 다음 이야기
        ?: if (played.isNotEmpty()) course.stories.firstOrNull { it.id !in played && it.id != storyId } else course.stories.getOrNull(storyIndex + 1)
}

private const val LOCATION_ASK_MS = 2_000L
private const val LOCATION_TIMEOUT_MS = 20_000L

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val gilwe = app as GilweApp
    val vehicle = gilwe.vehicle.state
    val gearOverride = gilwe.vehicle.override
    val restricted = gilwe.restriction.restricted
    val stamps = gilwe.stamps.stamps
    val speakingId = gilwe.voice.speakingId
    val navi = gilwe.navi.state
    val playback = gilwe.voice.playback

    /** 내비 위 오버레이가 떠 있음: 이때는 같은 내용을 헤드업 알림으로 또 띄우지 않는다 */
    @Volatile var overlayShowing = false

    init {
        // 내비 이벤트: 도착하면 도착·체크인 화면으로
        viewModelScope.launch {
            gilwe.navi.events.collect { e ->
                if (e is NaviEvent.Arrived && _trip.value != null && _screen.value == Screen.Drive) {
                    driveJob?.cancel()
                    _trip.update { it?.copy(progress = 1f) }
                    go(Screen.Park)
                }
                // 운전자가 내비 화면에서 "안내 종료"를 누름: 길 위 안내도 끝낸다 (내비 화면은 그대로 두고 이야기·오버레이만 정리)
                if (e is NaviEvent.Cancelled && _trip.value?.naviGuided == true && _screen.value == Screen.Drive) {
                    endTrip(bringFront = false)
                    _screen.value = Screen.Home
                }
            }
        }
    }

    private val _screen = MutableStateFlow<Screen>(Screen.Intro)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _screenLang = MutableStateFlow(gilwe.stamps.screenLang ?: Lang.EN)
    val screenLang: StateFlow<Lang> = _screenLang.asStateFlow()

    private val _audioLang = MutableStateFlow(gilwe.stamps.audioLang ?: _screenLang.value)
    val audioLang: StateFlow<Lang> = _audioLang.asStateFlow()

    private val _category = MutableStateFlow(Category.HISTORY)
    val category: StateFlow<Category> = _category.asStateFlow()

    private val _start = MutableStateFlow(Catalog.startPoints.first())
    val start: StateFlow<StartPoint> = _start.asStateFlow()

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var driveJob: Job? = null
    /** 위치 × 시간 조립 (기획 04). 내비 위치가 들어오는 동안 이야기 선택을 맡는다 */
    private var scheduler: StoryScheduler? = null
    private var parkJob: Job? = null

    val hasHistory: Boolean get() = gilwe.stamps.screenLang != null

    // ---------- 이동 ----------

    fun go(screen: Screen) {
        // 미리보기·이야기 듣기·전문 사이를 오갈 때는 낭독을 끊지 않는다
        if (screen !is Screen.RoutePreview && screen !is Screen.StoryListen && screen !is Screen.StoryFull) gilwe.voice.stop()
        _screen.value = screen
        if (screen == Screen.Park) watchParking()
    }

    /** @return false 면 시스템 back 처리(앱 종료)에 맡긴다 */
    fun back(): Boolean {
        val target: Screen = when (val s = _screen.value) {
            Screen.Intro -> return false
            is Screen.ScreenLanguage -> s.returnTo
            Screen.Home -> return false
            is Screen.StartSearch -> if (s.showResults) Screen.StartSearch(false) else Screen.Home
            is Screen.RoutePreview -> Screen.Home
            is Screen.AudioLanguage -> Screen.RoutePreview(s.courseId)
            Screen.Drive -> { endTrip(); Screen.Home }
            Screen.Park -> Screen.Drive
            is Screen.StampCollected -> Screen.Home
            Screen.MyStamps -> Screen.Home
            is Screen.RouteStories -> Screen.MyStamps
            is Screen.StoryListen -> Screen.RouteStories(Extras.story(s.storyId)?.first?.id ?: Catalog.courses.first().id)
            is Screen.StoryFull -> Screen.StoryListen(s.storyId)
            is Screen.Nearby -> Screen.StampCollected(s.courseId)
        }
        go(target)
        return true
    }

    fun getStarted() = go(Screen.ScreenLanguage(returnTo = Screen.Intro))

    fun setScreenLang(lang: Lang) {
        _screenLang.value = lang
        gilwe.stamps.screenLang = lang
        if (gilwe.stamps.audioLang == null) _audioLang.value = lang
        val s = _screen.value
        go(if (s is Screen.ScreenLanguage && s.returnTo != Screen.Intro) s.returnTo else Screen.Home)
    }

    fun setCategory(c: Category) { _category.value = c }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun chooseStart(p: StartPoint) {
        _start.value = p
        _searchQuery.value = ""
        go(Screen.Home)
    }

    // ---------- 경로 미리보기 ----------

    fun toggleIntro(course: Course) {
        val id = "intro:${course.id}"
        if (speakingId.value == id) gilwe.voice.stop()
        else gilwe.voice.speak(id, course.introTitle.of(_audioLang.value) + ". " + course.intro.of(_audioLang.value), _audioLang.value.locale)
    }

    /** 소개가 낭독 중이면 첫 이야기를 소개가 끝날 때까지 미룬다 */
    private val introSpeaking get() = speakingId.value?.startsWith("intro:") == true

    /**
     * 경로 미리보기의 "소개 듣기": 바로 주행을 시작한다.
     * 기본 내비 안내를 걸고, 코스 소개를 먼저 들려준 뒤 이야기들이 이어진다. 오디오 언어는 마지막에 고른 언어.
     */
    fun startTripWithIntro(courseId: String) {
        startTrip(courseId, _audioLang.value)
    }

    fun startTrip(courseId: String, audio: Lang) {
        _audioLang.value = audio
        gilwe.stamps.audioLang = audio
        gilwe.voice.stop()
        // 기본 내비에 목적지를 넘겨 실제 경로 안내 시작 (연결 안 되면 시뮬레이션만)
        val guided = PleosNavi.targets[courseId]?.let { gilwe.navi.startGuidance(it) } ?: false
        _trip.value = Trip(Catalog.course(courseId), _start.value, naviGuided = guided)
        scheduler = StoryScheduler(Catalog.course(courseId)) { st ->
            gilwe.voice.clips.clips(st.id, audio.locale.language)?.sumOf { it.ms }?.div(1000f) ?: estimateSeconds(st.script.of(audio))
        }
        pleos.gilwe.voice.TripService.start(gilwe, Catalog.course(courseId).place.of(_screenLang.value))
        gilwe.gps.start()
        go(Screen.Drive)
        // 고정 블록(오프닝): 출발하면 코스 소개부터. 소개가 끝나야 장소·자유 블록이 시작된다
        val course = Catalog.course(courseId)
        gilwe.voice.speak("intro:$courseId", course.introTitle.of(audio) + ". " + course.intro.of(audio), audio.locale)
        runDrive()
    }

    /** 내비 화면을 앞으로. 이야기는 뒤에서 계속 재생된다 */
    fun showNavigation() = gilwe.navi.showNavigation()

    // ---------- 주행 ----------

    /**
     * 경로 진행 시뮬레이션.
     * 실제 내비 연동 전까지 시연용으로 시간 기반 진행률을 쓰되,
     * Vehicle SDK로 기어를 읽을 수 있으면 D단일 때만 진행한다 (차량 신호에 반응).
     */
    private fun runDrive() {
        driveJob?.cancel()
        driveJob = viewModelScope.launch {
            val tickMs = 250L
            var lastNaviProgress = -1f
            var naviStalledMs = 0L
            var sinceLocationAsk = LOCATION_ASK_MS
            var lastFixAt: Long? = null
            var lastGpsWall = 0L
            val startedAt = System.currentTimeMillis()
            while (isActive) {
                delay(tickMs)
                val t0 = _trip.value ?: break
                val nav = navi.value
                val now = System.currentTimeMillis()

                // 위치 신호 (Figma 04C): 내비에 현재 위치를 주기적으로 묻고, 일정 시간 답이 없으면 위치 신호 없음
                var locationLost = false
                if (t0.naviGuided) {
                    sinceLocationAsk += tickMs
                    if (sinceLocationAsk >= LOCATION_ASK_MS) { sinceLocationAsk = 0; gilwe.navi.requestLocation() }
                    locationLost = now - (nav.lastLocationAt ?: startedAt) > LOCATION_TIMEOUT_MS
                }
                // 내비 안내 우선 (Figma 04B): 내비 음성이 오디오 포커스를 가져가 이야기를 멈춘 상태
                val waitingNavi = gilwe.voice.waitingForNavi.value
                val pb = gilwe.voice.playback.value

                var t = t0.copy(locationLost = locationLost, waitingForNavi = waitingNavi)
                if (t.naviGuided) t = t.copy(naviRemainMeters = if (locationLost) null else nav.remainMeters, naviRemainSeconds = if (locationLost) null else nav.remainSeconds)
                // 지금 이야기 진행률: 녹음 파일이면 실제 재생 위치, TTS 면 추정, 낭독이 끝났으면 1
                t.currentStory?.let { s ->
                    t = when {
                        pb?.id == s.id -> t.copy(storyProgress = pb.fraction)
                        speakingId.value == s.id && !t.storyPaused && !waitingNavi ->
                            t.copy(storyProgress = (t.storyProgress + tickMs / 1000f / estimateSeconds(s.script.of(_audioLang.value))).coerceAtMost(0.98f))
                        speakingId.value != s.id && !t.storyPaused && !waitingNavi -> t.copy(storyProgress = 1f)
                        else -> t
                    }
                }
                if (_screen.value != Screen.Drive) { if (t != t0) _trip.value = t; continue }

                // ---- 위치 × 시간 조립: 내비 위치가 있으면 좌표·속도로 이야기를 고른다 ----
                val sc = scheduler
                if (sc != null && t.naviGuided) {
                    // 위치: 차량 GPS 가 최근(5초 안)이면 GPS, 아니면 내비가 알려 준 현재 위치
                    val g = gilwe.gps.fix.value
                    val gpsAt = gilwe.gps.receivedAt
                    if (g != null && now - gpsAt < 5_000) {
                        if (gpsAt != lastGpsWall) { lastGpsWall = gpsAt; sc.onFix(g.latitude, g.longitude, gpsAt) }
                    } else {
                        val fixAt = nav.lastLocationAt
                        if (fixAt != null && fixAt != lastFixAt && nav.lat != null && nav.lng != null) { lastFixAt = fixAt; sc.onFix(nav.lat, nav.lng, fixAt) }
                    }
                }
                if (sc != null && t.naviGuided && sc.hasFix()) {
                    var next = t.copy(progress = maxOf(t.progress, nav.progress ?: t.progress), waitingForDrive = false, deadReckoning = sc.deadReckoning)
                    if (!waitingNavi && !introSpeaking) {
                        val playing = t.currentStory?.takeIf { t.storyProgress < 1f && !t.storyPaused }
                        val target = PleosNavi.targets[t.course.id]
                        val pos = sc.position(now)
                        val near = (nav.remainMeters ?: Int.MAX_VALUE) < 2000 ||
                            (target != null && pos != null && StoryScheduler.dist(pos.first, pos.second, target.lat, target.lng) < 2000)
                        val d = sc.decide(now, playing, near)
                        if (d is StoryScheduler.Decision.Play) {
                            android.util.Log.i("GilweSched", "play ${d.story.id} interrupt=${d.interrupt} pos=$pos speed=${"%.1f".format(sc.speed)} dr=${sc.deadReckoning} missed=${sc.missed}")
                            sc.played += d.story.id
                            next = next.copy(storyId = d.story.id, storyIndex = t.course.stories.indexOf(d.story), storyProgress = 0f, storyPaused = false, played = t.played + d.story.id)
                            speakStory(d.story)
                        }
                    }
                    _trip.value = next.copy(nextId = sc.nextPlace()?.id)
                    continue
                }

                // 경로 진행률: 내비 남은 거리가 움직이면 그 값, 위치 신호가 없거나 15초 넘게 그대로면 시간 기준으로 진행
                val np = if (t.naviGuided && !locationLost) nav.progress else null
                var useNavi = false
                if (np != null) {
                    if (np > lastNaviProgress + 0.0005f) { lastNaviProgress = np; naviStalledMs = 0 } else naviStalledMs += tickMs
                    useNavi = naviStalledMs < 15_000L
                }
                var progress = t.progress
                if (useNavi) {
                    progress = maxOf(progress, np!!)
                } else {
                    val v = vehicle.value
                    if (v.gearReadable && !v.isDriving) { _trip.value = t.copy(waitingForDrive = true); continue }
                    // 시연 길이: 이야기당 약 22초 + 여유
                    progress = (progress + tickMs / 1000f / (t.course.stories.size * 22f + 16f)).coerceAtMost(1f)
                }
                var next = t.copy(progress = progress, waitingForDrive = false)

                // 다음 이야기 지점을 지났고, 지금 이야기·소개·내비 안내가 끝났으면 다음 이야기 낭독
                val upcoming = t.course.stories.getOrNull(t.storyIndex + 1)
                val currentDone = t.currentStory == null || t.storyProgress >= 1f || t.storyPaused
                if (upcoming != null && progress >= upcoming.at && currentDone && !introSpeaking && !waitingNavi) {
                    next = next.copy(storyIndex = t.storyIndex + 1, storyProgress = 0f, storyPaused = false)
                    speakStory(upcoming)
                }
                _trip.value = next

                if (progress >= 1f && upcoming == null && currentDone && !introSpeaking) {
                    go(Screen.Park)
                    break
                }
            }
        }
    }

    private fun estimateSeconds(text: String): Float = (text.length * if (_audioLang.value.isKorean) 0.16f else 0.065f).coerceIn(6f, 20f)

    private fun speakStory(story: Story) {
        val lang = _audioLang.value
        gilwe.voice.speak(story.id, story.title.of(lang) + ". " + story.script.of(lang), lang.locale)
        // 내비 화면이 앞에 있어도 지금 어떤 이야기인지 보이도록 헤드업 알림
        _trip.value?.let { t ->
            val i = t.course.stories.indexOfFirst { it.id == story.id }
            if (i >= 0 && !overlayShowing) gilwe.notifier.showStory(_screenLang.value.strings.storyOf(i + 1, t.course.stories.size), story.title.of(lang), t.course.place.of(lang))
        }
    }

    fun toggleStoryPause() {
        val t = _trip.value ?: return
        val story = t.currentStory ?: return
        if (t.storyPaused || t.waitingForNavi) {
            _trip.value = t.copy(storyPaused = false)
            // 녹음 파일이면 멈춘 문장 처음부터, 아니면 이야기 처음부터
            if (gilwe.voice.playback.value?.id == story.id) gilwe.voice.resume() else speakStory(story)
        } else {
            gilwe.voice.pause()
            _trip.value = t.copy(storyPaused = true)
        }
    }

    /** 04C "위치 다시 찾기" */
    fun retryLocation() = gilwe.navi.requestLocation()

    fun skipToNextStory() {
        val t = _trip.value ?: return
        val next = t.nextStory ?: return
        scheduler?.played?.add(next.id)
        _trip.value = t.copy(progress = maxOf(t.progress, next.at), storyId = next.id, storyIndex = t.course.stories.indexOf(next), storyProgress = 0f, storyPaused = false, played = t.played + next.id)
        speakStory(next)
    }

    /** 시연용: 도착 직전으로 이동 */
    fun skipToArrival() {
        val t = _trip.value ?: return
        gilwe.voice.stop()
        _trip.value = t.copy(progress = 1f, storyIndex = t.course.stories.lastIndex, storyProgress = 1f)
        gilwe.navi.cancelGuidance()
        driveJob?.cancel()
        go(Screen.Park)
    }

    private fun endTrip(bringFront: Boolean = true) {
        gilwe.gps.stop()
        pleos.gilwe.voice.TripService.stop(gilwe)
        gilwe.notifier.clear()
        val wasGuided = _trip.value?.naviGuided == true
        gilwe.navi.cancelGuidance()
        driveJob?.cancel()
        parkJob?.cancel()
        gilwe.voice.stop()
        _trip.value = null
        // 경로를 취소하면 내비 앱이 스스로 앞으로 나온다. 사용자가 길 위에서 안내를 끝낸 것이므로 길 위 홈을 다시 앞으로.
        if (wasGuided && bringFront) viewModelScope.launch {
            delay(800)
            runCatching {
                gilwe.startActivity(
                    android.content.Intent(gilwe, pleos.gilwe.MainActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                )
            }
        }
    }

    // ---------- 도착·체크인 ----------

    /** P단이 되면 잠시 뒤 자동 체크인 (Vehicle SDK Gear) */
    private fun watchParking() {
        parkJob?.cancel()
        parkJob = viewModelScope.launch {
            vehicle.collect { v ->
                if (v.gearReadable && v.isParked && _screen.value == Screen.Park) {
                    delay(1500)
                    if (vehicle.value.isParked && _screen.value == Screen.Park) checkIn()
                }
            }
        }
    }

    fun checkIn() {
        val t = _trip.value ?: return
        parkJob?.cancel()
        driveJob?.cancel()
        pleos.gilwe.voice.TripService.stop(gilwe)
        gilwe.notifier.clear()
        gilwe.stamps.add(StampRecord(t.course.id, t.start.id, t.totalKm, System.currentTimeMillis()))
        _trip.value = null
        go(Screen.StampCollected(t.course.id))
    }

    // ---------- 다시 듣기 ----------

    fun replay(story: Story) {
        val id = story.id
        if (speakingId.value == id) gilwe.voice.stop() else speakStory(story)
    }

    fun stopVoice() = gilwe.voice.stop()

    /**
     * 디버그 빌드 전용: 화면·언어를 바로 열어 레이아웃을 검수한다.
     * adb shell am start -n pleos.gilwe/.MainActivity --es qa_screen listen --es qa_id hs3 --es qa_lang KO
     */
    fun qaOpen(screen: String, id: String?, langCode: String?) {
        Lang.entries.firstOrNull { it.code == langCode }?.let { _screenLang.value = it; _audioLang.value = it }
        val sid = id ?: "hs3"
        val cid = id ?: "hwaseong"
        when (screen) {
            "home" -> go(Screen.Home)
            "preview" -> go(Screen.RoutePreview(cid))
            "listen" -> Extras.story(sid)?.let { openStory(it.second) }
            "full" -> go(Screen.StoryFull(sid))
            "stamp" -> go(Screen.StampCollected(cid))
            "nearby" -> go(Screen.Nearby(cid))
            "stamps" -> go(Screen.MyStamps)
            "stories" -> go(Screen.RouteStories(cid))
            "lang" -> go(Screen.ScreenLanguage(Screen.Home))
            "intro" -> go(Screen.Intro)
            // 11A 시연용: 사흘 전 여행 기록 두 개를 넣는다
            "seed" -> {
                val past = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
                gilwe.stamps.add(StampRecord("changdeokgung", "cityhall", 3, past))
                gilwe.stamps.add(StampRecord("namhansanseong", "cityhall", 30, past + 2 * 60 * 60 * 1000))
                go(Screen.MyStamps)
            }
            "drive", "drive_navi", "drive_loc" -> {
                val c = Catalog.course(cid)
                driveJob?.cancel()
                _trip.value = Trip(c, _start.value, storyIndex = 1, storyProgress = 0.4f, naviGuided = true,
                    waitingForNavi = screen == "drive_navi", locationLost = screen == "drive_loc")
                _screen.value = Screen.Drive
            }
        }
    }

    // ---------- 06 이야기 듣기 · 전문 ----------

    /** 이야기 듣기 화면을 열고 처음부터 읽는다 (주행 규제 중이면 화면이 잠금 상태로 보인다) */
    fun openStory(story: Story) {
        go(Screen.StoryListen(story.id))
        if (!restricted.value) speakStory(story)
    }

    fun toggleListen(story: Story) {
        val pb = gilwe.voice.playback.value
        when {
            pb?.id == story.id && !pb.paused -> gilwe.voice.pause()
            pb?.id == story.id -> gilwe.voice.resume()
            speakingId.value == story.id -> gilwe.voice.stop()
            else -> speakStory(story)
        }
    }

    fun skipLine(delta: Int) = gilwe.voice.skipLine(delta)

    // ---------- 시연 도구 ----------

    fun setGearOverride(g: GearState?) = gilwe.vehicle.setOverride(g)

    override fun onCleared() {
        driveJob?.cancel()
        parkJob?.cancel()
    }
}
