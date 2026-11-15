package pleos.gilwe.data

import pleos.gilwe.R

/** 도착 후 걸어서 둘러볼 곳 (Figma 10 주변 장소 둘러보기 · 기획서 6-6 "걸어서 한 바퀴") */
data class WalkSpot(
    val name: T,
    val desc: T,
    /** 주차장에서 걷는 거리(m). 모르면 null (섬·마을 안처럼 길찾기 좌표가 없는 곳) */
    val meters: Int? = null,
    val minutes: Int? = null,
    val photo: Int? = null,
)

object Extras {

    /** Figma 06 전문 화면 정보 (제목·질문·알아 두세요·출처). 본문은 이야기 원고(script) 그대로 */
    data class FullInfo(
        val title: T? = null,
        val question: T,
        val note: Pair<T, T>? = null,
        val sources: List<String> = emptyList(),
    )

    /** Figma 06 "Full transcript" 프레임에서 옮긴 내용. 영어 원문, 한국어는 번역 */
    private val full = mapOf(
        "intro:changdeokgung" to FullInfo(
            T("Changdeokgung Palace", "창덕궁"), T("Why does this palace follow the landscape?", "이 궁궐은 왜 지형을 따랐을까요?"),
            sources = listOf("UNESCO / Changdeokgung Palace Complex", "Korea Tourism Organization / UNESCO guide"),
        ),
        "cd2" to FullInfo(
            T("Donhwamun Gate", "돈화문"), T("How has this gate survived for centuries?", "이 문은 어떻게 수백 년을 견뎠을까요?"),
            T("Visitor entrance", "관람 출입구") to T("Donhwamun is under conservation. Check the current visitor entrance before visiting.", "돈화문은 보수 중이에요. 방문 전에 현재 관람 출입구를 확인하세요."),
            listOf("Annals of King Taejong / naming Donhwamun, 1412", "Korea Heritage Service / Donhwamun conservation announcement, 2026-11-09"),
        ),
        "cd3" to FullInfo(
            T("Injeongjeon Hall", "인정전"), T("Why did foreign envoys come to this hall?", "외국 사신들은 왜 이 전각으로 왔을까요?"),
            sources = listOf("Korea Heritage Agency / Changdeokgung"),
        ),
        "cd4" to FullInfo(
            T("Huwon, the Secret Garden", "후원"), T("What made this garden a royal retreat?", "이 정원은 어떻게 왕실의 쉼터가 되었을까요?"),
            T("Secret Garden access", "후원 관람") to T("Secret Garden access requires a separate reservation.", "후원은 별도 예약이 필요해요."),
            listOf("Korea Tourism Organization / Changdeokgung Palace and Huwon", "UNESCO / Changdeokgung Palace Complex"),
        ),
        "intro:namhansanseong" to FullInfo(
            T("Namhansanseong Fortress", "남한산성"), T("Why build an emergency capital on a mountain?", "왜 산 위에 비상 도읍을 지었을까요?"),
            sources = listOf("UNESCO / Namhansanseong", "Korea Heritage Service / Namhansanseong interpretive text"),
        ),
        "nh2" to FullInfo(
            T("Namhansanseong Fortress Walls", "남한산성 성곽"), T("Who helped build and defend these walls?", "이 성벽은 누가 쌓고 지켰을까요?"),
            sources = listOf("UNESCO / Namhansanseong"),
        ),
        "nh3" to FullInfo(
            T("Sueojangdae", "수어장대"), T("Did you know this is the last of five command posts?", "이곳이 다섯 지휘소 가운데 마지막 남은 곳이라는 걸 아셨나요?"),
            sources = listOf("Gyeonggi Namhansanseong World Heritage Center / cultural heritage", "Korea Tourism Organization / Sueojangdae"),
        ),
        "nh4" to FullInfo(
            T("Namhansanseong Temporary Palace", "남한산성 행궁"), T("Where did the royal court go in an emergency?", "위기 때 조정은 어디로 갔을까요?"),
            sources = listOf("Gyeonggi Namhansanseong World Heritage Center / Temporary Palace"),
        ),
        "intro:hwaseong" to FullInfo(
            T("Hwaseong Haenggung", "화성행궁"), T("How did a birthday bring a king to Suwon?", "한 번의 생신이 어떻게 왕을 수원으로 이끌었을까요?"),
            sources = listOf("Suwon Hwaseong Museum / 1795 royal journey", "Suwon official tourism / Royal procession and pontoon bridge"),
        ),
        "hs1" to FullInfo(
            T("Historic Hangang crossing at Noryangjin", "노량진의 한강 배다리"), T("How do you build a bridge from boats?", "배로 어떻게 다리를 놓을까요?"),
            sources = listOf("National Museum of Korea / Royal Procession to Hwaseong in 1795"),
        ),
        "hs2" to FullInfo(
            T("Manangyo Bridge", "만안교"), T("Who else used this royal bridge?", "이 왕의 다리는 또 누가 건넜을까요?"),
            sources = listOf("Gyeonggi architecture portal / Manangyo", "Anyang City / Seoksu 2-dong local history"),
        ),
        "hs3" to FullInfo(
            T("Jijidae Pass", "지지대 고개"), T("Why does local tradition say the king looked back?", "왜 왕이 뒤돌아봤다고 전해질까요?"),
            T("Historical context", "역사적 맥락") to T("The story of the king slowing down is local tradition. The 1807 memorial is documented.", "왕이 걸음을 늦췄다는 이야기는 전해 오는 이야기예요. 1807년 비석은 기록으로 확인돼요."),
            listOf("Suwon Jangan District / Jijidaebi", "Suwon Museum / Jijidae narrative"),
        ),
        "hs4" to FullInfo(
            T("Nosongjidae, the old pine-tree belt", "노송지대, 오래된 소나무 길"), T("Why did a king pay to plant roadside trees?", "왜 왕이 길가에 나무를 심게 했을까요?"),
            sources = listOf("Gyeonggi Institute of Cultural Properties / Nosongjidae", "Korea Tourism Organization / Nosongjidae"),
        ),
        "hs5" to FullInfo(
            T("Hwaseong Haenggung, Bongsudang Hall", "화성행궁 봉수당"), T("What happened here on a royal mother's 60th birthday?", "왕의 어머니 회갑날, 이곳에서 무슨 일이 있었을까요?"),
            sources = listOf("Suwon Hwaseong Museum / Bongsudang birthday banquet painting", "Suwon official tourism / Hwaseong Haenggung"),
        ),
    )

    /** Figma 에 전문이 없는 이야기의 질문 (원고 내용 안에서 작성) */
    private val questions = mapOf(
        "cd1" to T("Why did kings prefer Changdeokgung?", "왕들은 왜 창덕궁을 더 좋아했을까요?"),
        "nh1" to T("How did people cross the Han River before bridges?", "다리가 없던 시절, 한강은 어떻게 건넜을까요?"),
        "fv1" to T("Why are market scenes filmed outside Seoul?", "사극의 장터 장면은 왜 서울 밖에서 찍을까요?"),
        "fv2" to T("What do the different roofs tell you?", "지붕 모양은 무엇을 알려 줄까요?"),
        "nm1" to T("Why did this road become a weekend favourite?", "이 길은 왜 주말 여행길이 되었을까요?"),
        "nm2" to T("Which scene do visitors still recreate?", "사람들은 어떤 장면을 지금도 따라 할까요?"),
        "intro:folkvillage" to T("Why did a village become a film set?", "이 마을은 어떻게 촬영지가 되었을까요?"),
        "intro:nami" to T("How did a drama make an island famous?", "드라마 한 편이 어떻게 섬을 알렸을까요?"),
    )

    fun fullInfo(storyId: String): FullInfo? = full[storyId]

    fun question(storyId: String): T? = full[storyId]?.question ?: questions[storyId]

    /**
     * 코스별 도보 지점. 거리: 화성행궁은 Figma 10 값, 창덕궁·남한산성은 OpenStreetMap 좌표의 직선거리 × 1.3 (도보 70m/분).
     * 민속촌·남이섬은 안쪽 길 좌표가 없어 거리 없이 안내만 한다.
     */
    private val walkSpots = mapOf(
        "changdeokgung" to listOf(
            WalkSpot(T("Donhwamun Gate", "돈화문"), T("The palace's main gate", "창덕궁의 정문"), 1250, 18),
            WalkSpot(T("Injeongjeon Hall", "인정전"), T("The throne hall for state ceremonies", "나라의 큰 의식을 치른 정전"), 1350, 20),
            WalkSpot(T("Buyongji Pond", "부용지"), T("Inside the Secret Garden · separate entry", "후원 안의 연못 · 별도 관람"), 1600, 23),
        ),
        "namhansanseong" to listOf(
            WalkSpot(T("Temporary Palace", "남한산성 행궁"), T("Where the court stayed in 1636", "1636년 조정이 머문 행궁"), 500, 7),
            WalkSpot(T("South Gate", "남문 (지화문)"), T("The fortress's main gate", "산성의 정문"), 850, 13),
            WalkSpot(T("Sueojangdae", "수어장대"), T("The command post on the ridge", "능선 위의 지휘소"), 1250, 18),
        ),
        "hwaseong" to listOf(
            WalkSpot(T("Sinpungnu Gate", "신풍루"), T("The palace's main entrance", "행궁의 정문"), 250, 4, R.drawable.photo_sinpungnu),
            WalkSpot(T("Bongsudang Hall", "봉수당"), T("The royal birthday banquet hall", "회갑 잔치를 연 곳"), 400, 6, R.drawable.photo_bongsudang),
            WalkSpot(T("Janganmun Gate", "장안문"), T("The fortress's north gate", "수원화성의 북문"), 1100, 17, R.drawable.photo_janganmun),
        ),
        "folkvillage" to listOf(
            WalkSpot(T("Village gate", "마을 입구"), T("Where the old streets begin", "옛 거리가 시작되는 곳")),
            WalkSpot(T("Magistrate's office", "관아"), T("A favourite set for period dramas", "사극에 자주 나오는 관청")),
            WalkSpot(T("Market street", "장터"), T("Food stalls and market scenes", "먹거리와 장터 풍경")),
        ),
        "nami" to listOf(
            WalkSpot(T("Ferry wharf", "선착장"), T("Board the ferry to the island", "배를 타고 섬으로")),
            WalkSpot(T("Metasequoia Lane", "메타세쿼이아길"), T("The drama's famous tree-lined lane", "드라마 속 그 가로수길"), photo = R.drawable.photo_nami),
            WalkSpot(T("Riverside path", "강변 산책로"), T("A quiet walk along the river", "강을 따라 걷는 길")),
        ),
    )

    fun walkSpots(courseId: String): List<WalkSpot> = walkSpots[courseId].orEmpty()

    /**
     * 장소 블록 (그 자리에서 듣는 이야기): 좌표·반경·중요도. 여기에 없는 이야기는 자유 블록(배경·인물, 빈 시간 채우기).
     * 좌표: OpenStreetMap. 만안교는 소재지(석수동), 지지대 고개는 의왕·수원 경계 근처 추정이라 반경을 넓게 잡았다.
     * 궁 안·성 안 지점(인정전·후원·행궁 등)은 차로 닿지 않으므로 도착 무렵 들리도록 반경을 넓혔다.
     */
    data class Place(val lat: Double, val lng: Double, val radius: Int, val importance: Int)

    private val places = mapOf(
        "cd1" to Place(37.5759, 126.9768, 600, 2),   // 광화문
        "cd2" to Place(37.5779, 126.9900, 700, 3),   // 돈화문
        "cd3" to Place(37.5794, 126.9911, 1200, 2),  // 인정전
        "cd4" to Place(37.5817, 126.9930, 1400, 1),  // 후원
        "nh1" to Place(37.5100, 127.1025, 1200, 2),  // 송파나루
        "nh2" to Place(37.4733, 127.1812, 1500, 2),  // 남문·성곽
        "nh3" to Place(37.4798, 127.1766, 1500, 2),  // 수어장대
        "nh4" to Place(37.4778, 127.1828, 1000, 3),  // 행궁
        "hs1" to Place(37.5154, 126.9573, 1200, 3),  // 한강대교(노량진)
        "hs2" to Place(37.4201, 126.9061, 1500, 2),  // 만안교(석수동)
        "hs3" to Place(37.3225, 126.9845, 1000, 2),  // 지지대 고개
        "hs4" to Place(37.3151, 126.9875, 800, 2),   // 노송지대
        "hs5" to Place(37.2819, 127.0137, 1200, 3),  // 화성행궁 봉수당
        "fv2" to Place(37.2590, 127.1177, 2000, 3),  // 한국민속촌
        "nm2" to Place(37.8034, 127.5257, 2500, 3),  // 남이섬 선착장
    )

    fun place(storyId: String): Place? = places[storyId]

    /** 고정 블록(마무리): 도착 전 "꼭 볼 3곳". tools/make_voice.py 의 outro 문장과 같아야 녹음 파일과 맞는다 */
    fun outroStory(c: Course): Story? {
        val s = walkSpots(c.id).take(3)
        if (s.size < 3) return null
        return Story(
            "outro:${c.id}", T("Three places to see", "꼭 볼 세 곳"), T("Before you arrive", "도착 전에"),
            T(
                "When you arrive, three places are worth seeing: ${s[0].name.en}, ${s[1].name.en} and ${s[2].name.en}. Explore them on foot after you park.",
                "도착하면 꼭 볼 세 곳이에요. ${s[0].name.ko}, ${s[1].name.ko}, ${s[2].name.ko}. 주차한 뒤 걸어서 둘러보세요.",
            ),
            1f,
        )
    }

    /** 코스 소개를 다시 듣기·전문 화면에서 쓰는 이야기 형태 (id = "intro:코스id", 녹음 파일과 같은 id) */
    fun introStory(c: Course) = Story("intro:${c.id}", c.introTitle, c.tagline, c.intro, 0f)

    /** 이야기 id → (코스, 이야기). "intro:코스id" 는 코스 소개 */
    fun story(storyId: String): Pair<Course, Story>? {
        if (storyId.startsWith("intro:")) return Catalog.courses.firstOrNull { "intro:${it.id}" == storyId }?.let { it to introStory(it) }
        return Catalog.courses.firstNotNullOfOrNull { c -> c.stories.firstOrNull { it.id == storyId }?.let { c to it } }
    }

    /**
     * 여행 단위로 묶기 (Figma 11A): 저장 시각이 앞 기록과 [gapMs] 넘게 떨어지면 새 여행.
     * @return 오래된 여행부터. 마지막이 이번(가장 최근) 여행
     */
    fun trips(records: List<StampRecord>, gapMs: Long = 6 * 60 * 60 * 1000L): List<List<StampRecord>> {
        val sorted = records.sortedBy { it.timestamp }
        val out = mutableListOf<MutableList<StampRecord>>()
        sorted.forEach { r ->
            val cur = out.lastOrNull()
            if (cur == null || r.timestamp - cur.last().timestamp > gapMs) out += mutableListOf(r) else cur += r
        }
        return out
    }
}
