package pleos.gilwe.voice

import android.content.Context
import android.util.Log
import org.json.JSONObject

/** 미리 만들어 둔 낭독 음성 한 문장 */
data class Clip(val text: String, val file: String, val ms: Int)

/**
 * 앱에 들어 있는 낭독 음성 파일 목록 (assets/voice/manifest.json, tools/make_voice.py 로 생성).
 * 코스 소개·이야기마다 한국어/영어 문장 단위 파일이 있다. 앱 안에 들어 있으니 터널처럼 신호가 없어도 재생된다.
 */
class VoiceClips(context: Context) {

    private val map: Map<String, Map<String, List<Clip>>> = runCatching {
        val json = JSONObject(context.assets.open("voice/manifest.json").bufferedReader().use { it.readText() })
        json.keys().asSequence().associateWith { id ->
            val langs = json.getJSONObject(id)
            langs.keys().asSequence().associateWith { lang ->
                val arr = langs.getJSONArray(lang)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Clip(o.getString("text"), o.getString("file"), o.optInt("ms"))
                }
            }
        }
    }.onFailure { Log.w("VoiceClips", "manifest load failed", it) }.getOrDefault(emptyMap())

    /** 이 언어의 파일 묶음 키 (한국어·영어만 녹음) */
    fun langKey(language: String): String? = when (language) { "ko" -> "ko"; "en" -> "en"; else -> null }

    fun clips(itemId: String, language: String): List<Clip>? = langKey(language)?.let { map[itemId]?.get(it) }

    /** 코스를 고를 때 이 코스 음성이 모두 준비돼 있는지 (앱에 들어 있어 내려받기 없이 바로 재생) */
    fun ready(itemIds: List<String>, language: String): Boolean = itemIds.all { clips(it, language)?.isNotEmpty() == true }
}
