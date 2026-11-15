package pleos.gilwe.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StampRecord(val courseId: String, val startId: String, val km: Int, val timestamp: Long)

/** 스탬프(방문 기록)와 설정을 로컬에 저장한다. 서버 없이도 시연이 끊기지 않게 SharedPreferences만 쓴다. */
class StampStore(context: Context) {
    private val prefs = context.getSharedPreferences("gilwe", Context.MODE_PRIVATE)

    private val _stamps = MutableStateFlow(load())
    val stamps: StateFlow<List<StampRecord>> = _stamps.asStateFlow()

    var screenLang: Lang?
        get() = prefs.getString("screenLang", null)?.let { c -> Lang.entries.firstOrNull { it.code == c } }
        set(v) = prefs.edit().putString("screenLang", v?.code).apply()

    var audioLang: Lang?
        get() = prefs.getString("audioLang", null)?.let { c -> Lang.entries.firstOrNull { it.code == c } }
        set(v) = prefs.edit().putString("audioLang", v?.code).apply()

    fun add(record: StampRecord) {
        val next = _stamps.value + record
        _stamps.value = next
        prefs.edit().putString("stamps", next.joinToString("\n") { "${it.courseId}|${it.startId}|${it.km}|${it.timestamp}" }).apply()
    }

    private fun load(): List<StampRecord> =
        prefs.getString("stamps", "").orEmpty().lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 4) null else StampRecord(p[0], p[1], p[2].toIntOrNull() ?: 0, p[3].toLongOrNull() ?: 0L)
        }
}
