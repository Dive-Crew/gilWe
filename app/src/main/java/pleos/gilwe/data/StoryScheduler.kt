package pleos.gilwe.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 위치 × 시간 조립 (기획 04): 이야기를 경로 위에 놓고, 그 자리에 닿는 시각에 튼다.
 *
 * 1. 장소 블록에 '들을 자리' — 좌표·반경·중요도 ([Extras.place])
 * 2. 지점별 도착 시각 — GPS 위치·속도로 다음 지점까지 남은 시간을 계속 다시 계산
 * 3. 사이는 자유 블록 — 다음 지점까지 시간이 충분하면 배경·인물 이야기로 채움
 * 4. 어긋나면 다시 짜기 — 늦게 닿으면 자유 블록을 끊고 장소 블록부터, 지나쳐 버린 건 '다시 들을 이야기'로
 * 예외 — 붙어 있는 지점은 중요도 순, 터널(위치 끊김)에선 마지막 속도·방향으로 위치 추정,
 *        경로를 벗어나도 좌표 기준이라 새 경로에서 그대로 다시 계산된다.
 * 고정 블록 — 오프닝(코스 소개)은 출발 때, 마무리(꼭 볼 3곳)는 목적지 2km 안에서.
 */
class StoryScheduler(private val course: Course, private val durationSec: (Story) -> Float) {

    sealed interface Decision {
        data object Wait : Decision
        /** @param interrupt 지금 나오는 자유 블록을 끊고 튼다 */
        data class Play(val story: Story, val interrupt: Boolean = false) : Decision
    }

    private data class Fix(val lat: Double, val lng: Double, val t: Long)

    private var last: Fix? = null
    private var prev: Fix? = null
    /** 부드럽게 만든 속도 (m/s) */
    var speed = 0.0
        private set
    /** 위치 신호가 끊겨 마지막 속도·방향으로 추정 중 (터널) */
    var deadReckoning = false
        private set

    val played = mutableSetOf<String>()
    /** 지나쳐서 못 들은 장소 이야기 → 주차 후 다시 듣기 */
    val missed = mutableSetOf<String>()
    private val closest = mutableMapOf<String, Double>()

    private val placeStories = course.stories.filter { Extras.place(it.id) != null }
    private val freeStories = course.stories.filter { Extras.place(it.id) == null }
    private val outro = Extras.outroStory(course)

    fun hasFix() = last != null

    fun onFix(lat: Double, lng: Double, t: Long) {
        val l = last
        if (l != null && l.lat == lat && l.lng == lng) { // 같은 자리: 정지 중
            if (t - l.t > 4000) { speed *= 0.5; last = l.copy(t = t) }
            return
        }
        if (l != null) {
            val dt = (t - l.t) / 1000.0
            if (dt > 0.2) {
                val v = dist(l.lat, l.lng, lat, lng) / dt
                if (v < 150) speed = if (speed == 0.0) v else speed * 0.6 + v * 0.4 // 540km/h 넘는 튐 값은 버림 (시연용 빠른 GPS 재생 허용)
            }
        }
        prev = l
        last = Fix(lat, lng, t)
    }

    /** 현재 위치. 신호가 8초 넘게 없으면 마지막 방향·속도로 앞으로 밀어 추정 (최대 3분) */
    fun position(now: Long): Pair<Double, Double>? {
        val l = last ?: return null
        val age = (now - l.t) / 1000.0
        val p = prev
        if (age < 8 || p == null || speed < 1.0) { deadReckoning = false; return l.lat to l.lng }
        deadReckoning = true
        val seg = dist(p.lat, p.lng, l.lat, l.lng)
        if (seg < 1) return l.lat to l.lng
        val k = min(age, 180.0) * speed / seg
        return (l.lat + (l.lat - p.lat) * k) to (l.lng + (l.lng - p.lng) * k)
    }

    /**
     * 이번 틱에 무엇을 틀지.
     * @param playing 지금 나오는 이야기 (끝났으면 null)
     * @param nearDestination 목적지 2km 안 (마무리 블록)
     */
    fun decide(now: Long, playing: Story?, nearDestination: Boolean): Decision {
        val pos = position(now) ?: return Decision.Wait
        val (lat, lng) = pos

        // 지나친 장소 판정: 가장 가까웠던 거리보다 반경 이상 멀어지면 놓친 것
        placeStories.filter { it.id !in played && it.id !in missed && it.id != playing?.id }.forEach { s ->
            val pl = Extras.place(s.id)!!
            val d = dist(lat, lng, pl.lat, pl.lng)
            val c = min(closest[s.id] ?: d, d)
            closest[s.id] = c
            if (c <= pl.radius * 1.8 && d > c + pl.radius) missed += s.id
        }

        val open = placeStories.filter { it.id !in played && it.id !in missed }
        val playingIsFree = playing != null && Extras.place(playing.id) == null && !playing.id.startsWith("intro:")
        // 반경 안 장소: 중요도 높은 것 먼저, 같으면 경로 순서. 뭔가 끝난 직후엔 조금 더 넓게(늦게라도 그 자리 근처에서)
        val window = if (playing == null) 1.8 else 1.0
        val due = open
            .map { it to Extras.place(it.id)!! }
            .filter { (_, pl) -> dist(lat, lng, pl.lat, pl.lng) <= pl.radius * window }
            .sortedWith(compareByDescending<Pair<Story, Extras.Place>> { it.second.importance }.thenBy { course.stories.indexOf(it.first) })
            .firstOrNull()?.first

        if (due != null) {
            if (playing == null) return Decision.Play(due)
            if (playingIsFree) return Decision.Play(due, interrupt = true) // 늦으면 자유 블록을 끊는다
            return Decision.Wait // 장소 이야기끼리는 끝날 때까지 기다림
        }
        if (playing != null) return Decision.Wait

        // 마무리 고정 블록
        if (nearDestination && outro != null && outro.id !in played) return Decision.Play(outro)

        // 자유 블록: 다음 장소까지 남은 시간이 자유 블록 길이 + 여유보다 길 때만
        val nextFree = freeStories.firstOrNull { it.id !in played } ?: return Decision.Wait
        val eta = etaSec(lat, lng, open.firstOrNull())
        return if (eta == null || eta > durationSec(nextFree) + 20) Decision.Play(nextFree) else Decision.Wait
    }

    /** 다음 장소까지 남은 시간(초). 직선거리 × 1.3(도로 굴곡) ÷ 속도. 서 있으면 null(시간 충분) */
    fun etaSec(lat: Double, lng: Double, next: Story?): Double? {
        val pl = next?.let { Extras.place(it.id) } ?: return null
        val d = max(0.0, dist(lat, lng, pl.lat, pl.lng) - pl.radius) * 1.3
        if (speed < 1.5) return null
        return d / speed
    }

    fun nextPlace(): Story? = placeStories.firstOrNull { it.id !in played && it.id !in missed }

    companion object {
        fun dist(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6_371_000.0
            val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
            val dp = p2 - p1; val dl = Math.toRadians(lng2 - lng1)
            val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
            return 2 * r * asin(sqrt(h))
        }
    }
}
