package pleos.gilwe.data

import pleos.gilwe.R

/** 한/영 두 벌 텍스트 */
data class T(val en: String, val ko: String) {
    fun of(lang: Lang) = if (lang.isKorean) ko else en
}

enum class Category { HISTORY, SCENE }

/** 그림 대체용 색 테마 (사진 에셋을 넣기 전까지 카드·스탬프 그림을 그린다) */
enum class ArtStyle { PALACE, FORTRESS, HAENGGUNG, FOLK_VILLAGE, ISLAND }

data class Story(
    val id: String,
    val title: T,
    val subtitle: T,
    val script: T,
    /** 경로 진행률(0~1) 중 이 이야기가 시작되는 지점 */
    val at: Float,
    /** 이야기 전용 사진. 없으면 코스 사진 */
    val photo: Int? = null,
)

data class Course(
    val id: String,
    val category: Category,
    val place: T,
    val tagline: T,
    val region: T,
    val introTitle: T,
    val intro: T,
    val parking: T,
    val parkingAddress: T,
    val parkingNote: T?,
    val placesAfterArrival: List<T>,
    val arrivalNote: T?,
    val minutesFromSeoul: Int,
    val kmFromSeoul: Int,
    val art: ArtStyle,
    /** Figma 시안 사진 (res/drawable-nodpi) */
    val photo: Int,
    val stories: List<Story>,
)

data class StartPoint(val id: String, val name: T, val address: T, val minutesDelta: Int, val kmDelta: Int)

object Catalog {
    /** 시연 기준 출발지 */
    val startPoints = listOf(
        StartPoint("cityhall", T("Seoul City Hall", "서울시청"), T("Jung-gu, Seoul", "서울 중구"), 0, 0),
        StartPoint("suwon", T("Suwon Station", "수원역"), T("Suwon, Gyeonggi-do", "경기 수원시"), -25, -30),
        StartPoint("gangnam", T("Gangnam Station", "강남역"), T("Gangnam-gu, Seoul", "서울 강남구"), -5, -6),
        StartPoint("incheon", T("Incheon Airport T1", "인천공항 제1터미널"), T("Jung-gu, Incheon", "인천 중구"), 35, 45),
    )

    val courses: List<Course> = listOf(
        Course(
            id = "changdeokgung",
            category = Category.HISTORY,
            place = T("Changdeokgung Palace", "창덕궁"),
            tagline = T("A royal palace shaped by nature.", "자연을 닮은 궁궐"),
            region = T("Jongno-gu, Seoul", "서울 종로구"),
            introTitle = T("A palace shaped by nature", "자연을 거스르지 않은 궁궐"),
            intro = T(
                "Changdeokgung offers a different way to imagine a royal palace. Its buildings and gardens were arranged around an uneven landscape, rather than forcing every space into a single rigid plan. The route from the main gate to the throne hall even turns along the way. Injeongjeon introduces the public world of royal ceremony. Beyond it, Huwon reveals a quieter setting of water, trees and pavilions. This course connects those two sides of court life: the palace as a place of power, and the garden as a retreat. Before your visit, check the current entrance arrangements and reserve Secret Garden access separately if you want to include it.",
                "창덕궁은 궁궐을 상상하는 또 다른 방법을 보여 줘요. 건물과 정원은 모든 공간을 하나의 반듯한 틀에 끼워 맞추는 대신, 고르지 않은 지형을 따라 배치됐어요. 정문에서 정전으로 가는 길조차 중간에 방향을 틀어요. 인정전은 왕실 의례가 펼쳐지던 공적인 세계를 보여 주고, 그 너머의 후원은 물과 나무, 정자가 어우러진 한결 고요한 공간을 드러내요. 이 코스는 궁중 생활의 두 얼굴, 권력의 공간인 궁궐과 쉼터인 정원을 이어 줘요. 방문 전에 현재 출입 방식을 확인하고, 후원까지 둘러보려면 따로 예약하세요.",
            ),
            parking = T("Jongmyo Public Parking", "종묘 공영주차장"),
            parkingAddress = T("157 Jong-ro, Jongno-gu, Seoul", "서울 종로구 종로 157"),
            parkingNote = T("No palace parking. Check the current entrance.", "궁 안에는 주차장이 없어요. 현재 출입구를 확인하세요."),
            placesAfterArrival = listOf(
                T("Donhwamun Gate", "돈화문"),
                T("Injeongjeon Hall", "인정전"),
                T("Secret Garden", "후원"),
            ),
            arrivalNote = T("Secret Garden admission is separate. Check current entry arrangements.", "후원은 별도 관람이에요. 현재 입장 방식을 확인하세요."),
            minutesFromSeoul = 12,
            kmFromSeoul = 3,
            art = ArtStyle.PALACE,
            photo = R.drawable.photo_changdeokgung,
            stories = listOf(
                Story("cd1", T("Gwanghwamun crossing", "광화문 네거리"), T("Where two palaces meet", "두 궁궐이 만나는 길"),
                    T("As you pass Gwanghwamun, you are between two palaces. Gyeongbokgung was the main palace, but kings often preferred Changdeokgung, the one you are heading to.",
                        "광화문을 지나는 지금, 두 궁궐 사이에 있어요. 정궁은 경복궁이었지만 많은 왕들은 지금 향하는 창덕궁에서 머무는 걸 더 좋아했어요."), 0.05f),
                Story("cd2", T("Donhwamun Gate", "돈화문"), T("Rebuilt, repaired, still standing", "다시 짓고 고쳐 온 정문"),
                    T("Donhwamun is Changdeokgung's main gate. It was first built in 1412, but the timber building did not survive the Japanese invasions of the sixteenth century. It was rebuilt in the early seventeenth century. Today another chapter is unfolding: conservation work is underway. This is a reminder that a historic palace survives through repair and care, not by remaining untouched.",
                        "돈화문은 창덕궁의 정문이에요. 1412년에 처음 세워졌지만, 목조 건물은 16세기 임진왜란을 견디지 못했어요. 17세기 초에 다시 지어졌죠. 지금은 또 다른 장이 펼쳐지고 있어요. 보수 공사가 진행 중이거든요. 오래된 궁궐은 손대지 않고 남아서가 아니라, 고치고 돌보며 살아남는다는 걸 보여 줘요."), 0.5f),
                Story("cd3", T("Injeongjeon Hall", "인정전"), T("The stage for royal ceremony", "왕실 의례의 무대"),
                    T("Injeongjeon was the palace's great ceremonial hall. Kings were enthroned here, royal weddings were celebrated, and envoys from other countries were received. Its grand setting was part of the message: visitors were entering the formal world of the Joseon court. Think of this as a stage for the kingdom's most public moments, rather than an everyday royal living room.",
                        "인정전은 궁궐의 으뜸 의례 공간이었어요. 이곳에서 왕이 즉위하고, 왕실 혼례가 치러지고, 다른 나라의 사신을 맞았어요. 웅장한 공간 자체가 메시지였죠. 이곳에 들어서는 사람은 조선 조정의 공식적인 세계로 들어선다는 뜻이었어요. 왕의 일상적인 거처라기보다, 나라의 가장 공적인 순간들이 펼쳐진 무대라고 생각해 보세요."), 0.75f),
                Story("cd4", T("Huwon, the Secret Garden", "후원"), T("A royal retreat shaped by nature", "자연을 닮은 왕실의 쉼터"),
                    T("Behind the formal palace lies Huwon, often called the Secret Garden. This was a place for the royal family to rest. Rather than flattening the land into one rigid pattern, the garden works with its slopes, water and wooded setting. Pavilions and ponds become part of that landscape. The surprise is how a royal retreat can feel so closely connected to nature.",
                        "격식 있는 궁궐 뒤편에는 흔히 비원이라고도 부르는 후원이 있어요. 왕실 가족이 쉬던 곳이에요. 땅을 평평하게 깎아 하나의 틀에 맞추는 대신, 정원은 비탈과 물, 숲을 그대로 살렸어요. 정자와 연못도 그 풍경의 일부가 되었죠. 왕실의 쉼터가 이렇게 자연과 가깝게 느껴진다는 게 놀라운 점이에요."), 0.9f),
            ),
        ),
        Course(
            id = "namhansanseong",
            category = Category.HISTORY,
            place = T("Namhansanseong Fortress", "남한산성"),
            tagline = T("Why build a capital on a mountain?", "왜 산 위에 도읍을 지었을까?"),
            region = T("Gwangju, Gyeonggi-do", "경기 광주시"),
            introTitle = T("The mountain that became a capital", "도읍이 된 산"),
            intro = T(
                "Namhansanseong was planned as an emergency capital for the Joseon dynasty. Within the mountain defenses were spaces for royal life, government and a resident community. During the Qing invasion that began in 1636, King Injo and his court took refuge here. The resistance lasted forty-seven days before surrender. The site carries that difficult history alongside remarkable engineering. After parking, the temporary palace introduces the royal refuge; Sueojangdae explains military observation; and the walls show how the mountain shaped the defense. These are places to explore on foot. The story is not only about holding a fortress, but about keeping a kingdom functioning during a crisis.",
                "남한산성은 조선의 비상 도읍으로 계획됐어요. 산속 방어 시설 안에 왕실 생활과 행정, 그리고 주민들이 사는 공간이 있었죠. 1636년 청나라의 침입이 시작되자 인조와 조정은 이곳으로 피신했어요. 항전은 47일 동안 이어진 끝에 항복으로 끝났어요. 이곳에는 그 아픈 역사와 함께 놀라운 축성 기술이 남아 있어요. 주차한 뒤에는 행궁에서 왕실의 피난처를, 수어장대에서 군사적 관측을, 성벽에서 산이 방어를 어떻게 만들었는지 걸으며 볼 수 있어요. 이 이야기는 성을 지켜 낸 것만이 아니라, 위기 속에서도 나라를 움직이게 한 이야기예요.",
            ),
            parking = T("Namhansanseong Central Parking", "남한산성 중앙주차장"),
            parkingAddress = T("Namhansanseong-myeon, Gwangju", "경기 광주시 남한산성면"),
            parkingNote = null,
            placesAfterArrival = listOf(
                T("Temporary Palace", "행궁"),
                T("Sueojangdae", "수어장대"),
                T("Fortress walls", "성곽길"),
            ),
            arrivalNote = null,
            minutesFromSeoul = 50,
            kmFromSeoul = 30,
            art = ArtStyle.FORTRESS,
            photo = R.drawable.photo_namhansanseong,
            stories = listOf(
                Story("nh1", T("Songpa crossing", "송파나루"), T("A river port before the fortress", "산성 아래 나루터"),
                    T("Long before the bridges, travellers crossed the Han River here by ferry on their way to the southern fortress.",
                        "다리가 놓이기 훨씬 전, 사람들은 이 근처 나루에서 배를 타고 한강을 건너 남쪽 산성으로 향했어요."), 0.1f),
                Story("nh2", T("Fortress walls", "남한산성 성곽"), T("A city inside the walls", "성벽 안의 도시"),
                    T("These walls belonged to more than a military outpost. Namhansanseong was a fortified city, with places for administration, worship and daily life inside its defenses. Buddhist monk-soldiers helped build and defend it. The mountain was part of the protection, and the fortifications developed as weapons changed. Following a section of wall on foot reveals how carefully the city and the terrain worked together.",
                        "이 성벽은 단순한 군사 초소의 것이 아니었어요. 남한산성은 방어 시설 안에 관청과 사찰, 일상생활의 공간이 있던 성곽 도시였어요. 승병들도 성을 쌓고 지키는 데 힘을 보탰죠. 산 자체가 방어의 일부였고, 무기가 바뀌면서 성곽 시설도 함께 발전했어요. 성벽 한 구간을 직접 걸어 보면 도시와 지형이 얼마나 정교하게 맞물렸는지 알 수 있어요."), 0.55f),
                Story("nh3", T("Sueojangdae", "수어장대"), T("A command post above the clouds", "구름 위의 지휘소"),
                    T("Sueojangdae was built for military command and observation. Namhansanseong once had five command posts; this is the only one that remains. Its elevated position helps explain the choice: defending a mountain fortress meant reading the surrounding terrain as well as guarding its walls. The pavilion was expanded to two storeys in 1751, so its present form reflects more than one period of the fortress's history.",
                        "수어장대는 군사 지휘와 관측을 위해 지어졌어요. 남한산성에는 한때 지휘소가 다섯 곳 있었는데, 지금 남은 것은 이곳뿐이에요. 높은 자리에 선 데는 이유가 있어요. 산성을 지키려면 성벽뿐 아니라 주변 지형까지 읽어야 했거든요. 1751년에 2층으로 증축되어, 지금의 모습에는 산성 역사의 여러 시기가 함께 담겨 있어요."), 0.85f),
                Story("nh4", T("Temporary Palace", "남한산성 행궁"), T("Where the court took refuge", "조정이 피신한 곳"),
                    T("A haenggung is a palace used when a king is away from the capital. At Namhansanseong, that idea had an urgent purpose: the royal court could move inside the fortress during war or rebellion. King Injo took refuge here during the Qing invasion of 1636. The palace you visit today includes reconstructed buildings, helping us understand how government and royal life continued inside these defenses.",
                        "행궁은 왕이 도성을 떠나 있을 때 머무는 궁궐이에요. 남한산성에서는 그 쓰임이 절박했어요. 전쟁이나 반란이 일어나면 조정이 성 안으로 옮겨 올 수 있었죠. 인조는 1636년 청나라가 침입했을 때 이곳으로 피신했어요. 지금 둘러보는 행궁에는 복원된 건물들이 있어, 이 방어 시설 안에서 나랏일과 왕실 생활이 어떻게 이어졌는지 이해하게 해 줘요."), 0.95f),
            ),
        ),
        Course(
            id = "hwaseong",
            category = Category.HISTORY,
            place = T("Hwaseong Haenggung", "화성행궁"),
            tagline = T("A birthday that shaped a journey.", "한 번의 생신이 만든 길"),
            region = T("Suwon, Gyeonggi-do", "경기 수원시"),
            introTitle = T("The king's road to Suwon", "정조가 걸은 수원 가는 길"),
            intro = T(
                "In 1795, King Jeongjo travelled to Hwaseong with his mother, Lady Hyegyeong, for celebrations marking her sixtieth birthday. Their journey crossed the Han River on a temporary bridge of boats. Along the modern drive, stories connected with the king lead toward Suwon: an enduring stone bridge, a pass remembered for his devotion to his father, and an old roadside pine-tree belt. At Hwaseong Haenggung, Bongsudang Hall brings the birthday story into focus. This route links places associated with Jeongjo across different moments in history; it does not reproduce every step of the 1795 procession. The historical road and today's drivable route are related, but they are not identical.",
                "1795년 정조는 어머니 혜경궁 홍씨의 회갑을 기념하기 위해 어머니와 함께 화성으로 행차했어요. 행렬은 배를 이어 만든 임시 다리로 한강을 건넜죠. 지금 달리는 길을 따라 정조와 이어진 이야기들이 수원까지 이어져요. 오래도록 남은 돌다리, 아버지를 향한 효심으로 기억되는 고개, 그리고 오래된 길가 소나무 숲이에요. 화성행궁에 이르면 봉수당에서 회갑 잔치 이야기가 펼쳐져요. 이 경로는 역사의 여러 순간에 정조와 이어진 장소들을 잇지만, 1795년 행렬의 모든 걸음을 그대로 재현하지는 않아요. 옛 길과 오늘 차로 달리는 길은 이어져 있지만, 똑같지는 않아요.",
            ),
            parking = T("Hwaseong Haenggung Parking", "화성행궁 공영주차장"),
            parkingAddress = T("Paldal-gu, Suwon", "경기 수원시 팔달구"),
            parkingNote = null,
            placesAfterArrival = listOf(
                T("Sinpungnu Gate", "신풍루"),
                T("Bongsudang Hall", "봉수당"),
                T("Hwaseong Fortress walls", "수원화성 성곽"),
            ),
            arrivalNote = null,
            minutesFromSeoul = 60,
            kmFromSeoul = 40,
            art = ArtStyle.HAENGGUNG,
            photo = R.drawable.photo_hwaseong,
            stories = listOf(
                Story("hs1", T("Hangang crossing", "한강 배다리"), T("A bridge of boats", "배를 이어 만든 다리"),
                    T("In 1795, King Jeongjo's journey to Hwaseong included a remarkable river crossing. Boats were joined together to support a temporary bridge across the Han River. The king was travelling with his mother for celebrations marking her sixtieth birthday. The journey was recorded in words and pictures, including the crossing. Today's riverside is different, but that record preserves an extraordinary moment of royal travel.",
                        "1795년 정조의 화성 행차에는 놀라운 강 건너기가 있었어요. 배들을 이어 붙여 한강을 가로지르는 임시 다리를 받쳤죠. 왕은 어머니의 회갑을 기념하러 어머니와 함께 가는 길이었어요. 이 행차는 글과 그림으로 기록되었고, 강을 건너는 장면도 그 안에 남아 있어요. 오늘의 강변은 달라졌지만, 그 기록은 왕실 행차의 특별한 순간을 지금까지 전해 줘요."), 0.02f),
                // 자유 블록: 장소와 상관없이 지점 사이 빈 시간을 채우는 배경·인물 이야기
                Story("hsf1", T("Lady Hyegyeong's story", "혜경궁 이야기"), T("The mother at the heart of the journey", "행차의 주인공, 어머니"),
                    T("Lady Hyegyeong was King Jeongjo's mother. Her husband, Crown Prince Sado, died in 1762, when Jeongjo was still a boy. Years later she wrote a memoir, Hanjungnok, about her life at court. The journey of 1795 celebrated her sixtieth birthday, and it was also a visit to her husband's tomb near Suwon.",
                        "혜경궁 홍씨는 정조의 어머니예요. 남편 사도세자는 1762년, 정조가 아직 어린 소년이던 때 세상을 떠났어요. 훗날 혜경궁은 궁중에서의 삶을 담은 회고록 『한중록』을 남겼죠. 1795년의 행차는 혜경궁의 회갑을 기념하는 길이자, 수원 근처에 있는 남편의 무덤을 찾아가는 길이기도 했어요."), 0.15f),
                Story("hs2", T("Manangyo Bridge", "만안교"), T("Seven stone arches, completed in 1795", "1795년에 완공된 일곱 개의 돌 아치"),
                    T("Manangyo was completed in 1795 under King Jeongjo. Unlike a temporary crossing assembled for a royal visit, a stone bridge could keep serving other travellers. Its seven stone arches are the detail to notice. There is another twist: the bridge was moved in 1980 during road expansion. So the structure carries the history of the royal road, even though it no longer stands at its original crossing.",
                        "만안교는 1795년 정조 때 완공됐어요. 왕의 행차를 위해 잠시 놓는 임시 다리와 달리, 돌다리는 다른 길손들도 계속 건널 수 있었죠. 일곱 개의 돌 아치를 눈여겨보세요. 반전도 하나 있어요. 1980년 도로를 넓히면서 다리가 옮겨졌거든요. 그래서 원래 자리는 아니지만, 이 다리는 여전히 왕의 길의 역사를 품고 있어요."), 0.25f),
                Story("hsf2", T("Building a new city", "신도시 화성을 짓다"), T("Suwon, planned by a king", "왕이 계획한 도시, 수원"),
                    T("Hwaseong Fortress was built between 1794 and 1796. King Jeongjo planned Suwon as a new city near his father's relocated tomb. The scholar Jeong Yak-yong helped design it, and new devices such as a lifting crane made the work faster. A detailed construction record, Hwaseong Seongyeok Uigwe, later guided its restoration, and UNESCO listed the fortress in 1997.",
                        "수원화성은 1794년부터 1796년까지 지어졌어요. 정조는 옮겨 모신 아버지의 무덤 가까이에 수원을 새 도시로 계획했죠. 학자 정약용이 설계를 도왔고, 거중기 같은 새 기구 덕분에 공사가 빨라졌어요. 공사 과정을 자세히 남긴 『화성성역의궤』는 훗날 복원의 길잡이가 되었고, 수원화성은 1997년 유네스코 세계유산이 되었어요."), 0.4f),
                Story("hs3", T("Jijidae Pass", "지지대 고개"), T("A king's devotion, remembered on a hill", "고개에 남은 효심"),
                    T("Jijidae Pass lies between Uiwang and Suwon. Local tradition tells of King Jeongjo slowing his return journey here, reluctant to leave the area associated with his father's tomb. The story connects the pass with a son's grief and devotion. What we can see in the historical record is the memorial stone erected in 1807 to honour that devotion. The remembered emotion gives this ordinary-looking road another layer.",
                        "지지대 고개는 의왕과 수원 사이에 있어요. 전해 오는 이야기에 따르면, 정조는 아버지의 무덤이 있는 곳을 떠나기 아쉬워 돌아가는 길에 이 고개에서 걸음을 늦췄대요. 이 이야기는 고개를 아들의 슬픔과 효심으로 이어 줘요. 역사 기록으로 확인할 수 있는 것은 그 효심을 기리려 1807년에 세운 비석이에요. 기억 속 마음이 평범해 보이는 이 길에 또 하나의 겹을 더해 줘요."), 0.5f),
                Story("hs4", T("Old pine road", "노송지대"), T("Trees planted for the procession", "행차를 위해 심은 소나무"),
                    T("Nosongjidae is the old pine-tree belt along the historic road near Suwon. The heritage account says King Jeongjo funded the planting of five hundred pines and forty weeping willows on this route. It links the landscape with journeys to his father's tomb. These are historical planting figures, not a count of trees standing today. The surviving landscape is cared for as a living part of that memory.",
                        "노송지대는 수원 근처 옛길을 따라 이어진 오래된 소나무 숲이에요. 문화재 기록에 따르면 정조는 이 길에 소나무 500그루와 능수버들 40그루를 심도록 비용을 댔어요. 이 풍경은 아버지의 무덤을 오가던 여정과 이어져 있어요. 다만 이 숫자는 역사 속 식재 기록이지, 지금 서 있는 나무의 수는 아니에요. 남아 있는 풍경은 그 기억의 살아 있는 일부로 보살펴지고 있어요."), 0.72f, photo = R.drawable.photo_pines),
                Story("hs5", T("Bongsudang Hall", "봉수당"), T("A sixtieth birthday banquet", "회갑 잔치가 열린 곳"),
                    T("In 1795, King Jeongjo honoured his mother, Lady Hyegyeong, with a sixtieth-birthday banquet at Bongsudang Hall. The event was part of his celebrated journey to Hwaseong. Yet this palace was more than a setting for royal festivities. When the king was away, it also served the local administration. The same complex held both the intimate story of a family and the everyday business of governing a city.",
                        "1795년 정조는 봉수당에서 어머니 혜경궁 홍씨의 회갑 잔치를 열어 어머니를 기렸어요. 이 잔치는 널리 알려진 화성 행차의 한 부분이었죠. 하지만 이 행궁은 왕실 잔치의 무대만은 아니었어요. 왕이 없을 때는 지방 행정을 맡는 관아로도 쓰였거든요. 한 공간에 한 가족의 애틋한 이야기와 도시를 다스리는 일상이 함께 담겨 있어요."), 0.9f),
            ),
        ),
        Course(
            id = "folkvillage",
            category = Category.SCENE,
            place = T("Korean Folk Village", "한국민속촌"),
            tagline = T("Walk into a Korean period drama.", "사극 속으로 걸어 들어가기"),
            region = T("Yongin, Gyeonggi-do", "경기 용인시"),
            introTitle = T("A village made for stories", "이야기를 위해 남은 마을"),
            intro = T(
                "Opened in 1974, the Korean Folk Village brings together traditional houses from across the country. Many period dramas have been filmed on its streets.",
                "1974년 문을 연 한국민속촌에는 전국의 전통 가옥이 모여 있어요. 수많은 사극이 이 골목에서 촬영됐어요.",
            ),
            parking = T("Korean Folk Village Parking", "한국민속촌 주차장"),
            parkingAddress = T("Giheung-gu, Yongin", "경기 용인시 기흥구"),
            parkingNote = null,
            placesAfterArrival = listOf(
                T("Village gate", "마을 입구"),
                T("Magistrate's office", "관아"),
                T("Market street", "장터"),
            ),
            arrivalNote = null,
            minutesFromSeoul = 50,
            kmFromSeoul = 43,
            art = ArtStyle.FOLK_VILLAGE,
            photo = R.drawable.photo_folkvillage,
            stories = listOf(
                Story("fv1", T("Leaving the city", "도시를 벗어나며"), T("Why dramas leave Seoul", "사극이 서울을 떠나는 이유"),
                    T("Seoul's palaces are protected heritage, so most period scenes with crowds and markets are filmed in places like the one ahead.",
                        "서울의 궁궐은 보호 문화재라, 군중과 장터가 나오는 사극 장면은 대부분 지금 향하는 곳 같은 촬영지에서 찍어요."), 0.1f),
                Story("fv2", T("Houses from every region", "팔도의 집"), T("A map of Korea in rooftops", "지붕으로 보는 한국 지도"),
                    T("Look at the roofs when you arrive. Thatched, tiled and bark roofs come from different regions and climates across Korea.",
                        "도착하면 지붕을 보세요. 초가, 기와, 너와 지붕은 저마다 다른 지역과 기후에서 왔어요."), 0.6f),
            ),
        ),
        Course(
            id = "nami",
            category = Category.SCENE,
            place = T("Nami Island", "남이섬"),
            tagline = T("An island made for a drama scene.", "드라마 한 장면이 된 섬"),
            region = T("Gapyeong wharf, Gyeonggi-do", "경기 가평 선착장"),
            introTitle = T("Tree-lined paths on the river", "강 위의 가로수길"),
            intro = T(
                "Nami Island became known across Asia after the 2002 drama Winter Sonata filmed its tree-lined paths.",
                "남이섬은 2002년 드라마 〈겨울연가〉가 가로수길을 촬영한 뒤 아시아 곳곳에 알려졌어요.",
            ),
            parking = T("Gapyeong Wharf Parking", "가평 선착장 주차장"),
            parkingAddress = T("Gapyeong-eup, Gapyeong-gun", "경기 가평군 가평읍"),
            parkingNote = T("Cars stay at the wharf. Take the ferry to the island.", "차는 선착장에 두고 배로 들어가요."),
            placesAfterArrival = listOf(
                T("Metasequoia Lane", "메타세쿼이아길"),
                T("Riverside path", "강변 산책로"),
                T("Ferry wharf", "선착장"),
            ),
            arrivalNote = null,
            minutesFromSeoul = 95,
            kmFromSeoul = 80,
            art = ArtStyle.ISLAND,
            photo = R.drawable.photo_nami,
            stories = listOf(
                Story("nm1", T("Along the Bukhan River", "북한강을 따라"), T("A river that shaped weekend trips", "주말 여행을 만든 강"),
                    T("This road follows the Bukhan River. For decades it has been Seoul's favourite weekend escape by car and by rail.",
                        "이 길은 북한강을 따라가요. 오랫동안 서울 사람들이 차와 기차로 즐겨 찾은 주말 여행길이에요."), 0.1f),
                Story("nm2", T("The tree-lined lane", "가로수길"), T("A scene fans still recreate", "팬들이 지금도 따라 하는 장면"),
                    T("The tall metasequoia lane on the island is where the drama's most famous scenes were filmed. Visitors still recreate them today.",
                        "섬 안의 메타세쿼이아 길은 드라마의 대표 장면을 찍은 곳이에요. 지금도 많은 사람들이 그 장면을 따라 해요."), 0.65f),
            ),
        ),
    )

    fun course(id: String) = courses.first { it.id == id }

    /** 모을 수 있는 전체 장소 수 (내 스탬프 화면 "24곳 중") */
    const val TOTAL_PLACES = 24
}
