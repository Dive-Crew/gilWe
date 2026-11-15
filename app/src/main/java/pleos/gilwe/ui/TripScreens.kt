package pleos.gilwe.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pleos.gilwe.data.Course
import pleos.gilwe.data.Lang
import pleos.gilwe.data.StartPoint
import pleos.gilwe.vehicle.VehicleState

/** 01B~02 경로 미리보기 */
@Composable
fun RoutePreviewScreen(
    lang: Lang,
    course: Course,
    start: StartPoint,
    introPlaying: Boolean,
    onBack: () -> Unit,
    onToggleIntro: () -> Unit,
    onChooseAudio: () -> Unit,
) {
    val s = lang.strings
    val minutes = (course.minutesFromSeoul + start.minutesDelta).coerceAtLeast(5)
    val km = (course.kmFromSeoul + start.kmDelta).coerceAtLeast(2)
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 48.dp, vertical = 32.dp)) {
        BackHeader(s.routePreview, onBack)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // 왼쪽: 경로 지도 + 주차장
            Box(Modifier.weight(1.2f).fillMaxHeight().clip(CardShape)) {
                PreviewMap(Modifier.fillMaxSize(), course.art, course.photo)
                Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        LabelText(s.from, color = C.Sub)
                        Text(start.name.of(lang), style = Type.title)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$minutes ${s.min}", style = Type.title)
                        Text("$km ${s.km}", style = Type.sub)
                    }
                }
                CardBox(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp), color = C.Panel.copy(alpha = 0.92f)) {
                    Text(course.parking.of(lang), style = Type.body, fontWeight = FontWeight.Medium)
                    Text(course.parkingAddress.of(lang), style = Type.sub)
                    course.parkingNote?.let {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.clip(RoundedCornerShape(12.dp)).background(C.CardHi).padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Info, null, tint = C.Sub, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it.of(lang), fontSize = 18.sp, color = C.Sub)
                        }
                    }
                }
            }
            // 오른쪽: 소개 + 도착 후 둘러볼 곳
            CardBox(Modifier.weight(1f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlaceArt(course.art, Modifier.size(120.dp, 80.dp).clip(RoundedCornerShape(12.dp)), photo = course.photo, scrim = false)
                    Spacer(Modifier.width(18.dp))
                    Text(course.introTitle.of(lang), style = Type.title, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Text(course.intro.of(lang), style = Type.sub, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(20.dp))
                Text(s.afterYouArrive, style = Type.sub)
                Spacer(Modifier.height(10.dp))
                course.placesAfterArrival.forEachIndexed { i, p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(C.CardHi)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("%02d".format(i + 1), fontSize = 18.sp, color = C.Faint)
                        Spacer(Modifier.width(16.dp))
                        Text(p.of(lang), style = Type.body, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.Sub)
                    }
                }
                course.arrivalNote?.let {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Info, null, tint = C.Sub, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it.of(lang), fontSize = 18.sp, color = C.Sub)
                    }
                }
                Spacer(Modifier.weight(1f))
                PillButton(
                    if (introPlaying) s.stopIntroduction else s.playIntroduction, onToggleIntro, Modifier.fillMaxWidth(),
                    icon = if (introPlaying) Icons.Filled.Stop else Icons.Filled.Headphones, height = 64.dp,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PillButton(s.chooseAudioLanguageCta, onChooseAudio, Modifier.fillMaxWidth())
    }
}

/** 02 주행과 듣기 — 지도 + 이야기 카드. 주행 중 화면이라 버튼은 크게, 글은 짧게. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DriveScreen(
    lang: Lang,
    trip: pleos.gilwe.ui.Trip,
    audio: Lang,
    vehicle: VehicleState,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    onDemoArrive: () -> Unit,
    onDemoTools: () -> Unit = {},
    onShowNavi: () -> Unit = {},
    onRetryLocation: () -> Unit = {},
) {
    val s = lang.strings
    val story = trip.currentStory
    val next = trip.nextStory
    Box(Modifier.fillMaxSize()) {
        RouteMap(trip.progress, Modifier.fillMaxSize(), seed = trip.course.id.hashCode())
        // 회전 안내
        CardBox(Modifier.align(Alignment.TopStart).padding(24.dp).width(460.dp), color = C.Panel.copy(alpha = 0.94f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowUpward, null, tint = C.Text, modifier = Modifier.size(52.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        when { trip.locationLost -> s.locationLost; trip.progress > 0.92f -> s.arrivingSoon; else -> s.continueLabel },
                        style = Type.h2,
                    )
                    Text(
                        when {
                            trip.locationLost -> s.lastKnownPosition
                            trip.waitingForNavi -> s.naviPlaying
                            trip.naviGuided -> s.naviGuiding
                            else -> trip.course.region.of(lang)
                        },
                        style = Type.sub,
                    )
                }
            }
            // 기본 내비로 경로 안내 중이면 내비 화면으로 전환 (이야기는 뒤에서 계속 재생)
            if (trip.naviGuided) {
                Spacer(Modifier.height(16.dp))
                PillButton(s.showNavi, onShowNavi, Modifier.fillMaxWidth(), height = 56.dp, primary = false)
            }
        }
        // 목적지 (탭: 시연 도구, 길게 누르면 시연용 도착)
        DestinationChip(
            lang, trip,
            Modifier.align(Alignment.TopEnd).padding(24.dp).clip(CardShape).combinedClickable(onClick = onDemoTools, onLongClick = onDemoArrive),
        )
        // 남은 시간·거리
        Row(
            Modifier.align(Alignment.CenterStart).padding(24.dp).clip(CardShape).background(C.Panel.copy(alpha = 0.94f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column {
                Text(if (trip.locationLost) "—" else "${trip.remainingMinutes} ${s.min}", style = Type.h2)
                Text(s.remaining, style = Type.sub)
            }
            Column {
                Text(if (trip.locationLost) "—" else "%.1f %s".format(trip.remainingKm, s.km), style = Type.h2)
                Text(s.distance, style = Type.sub)
            }
        }
        // 이야기 카드
        StoryCard(lang, trip, audio, onTogglePause, onNext, Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp), onRetryLocation = onRetryLocation)
    }
}

/** 길 위 목적지 칩 — 주행 화면과 내비 위 오버레이에서 같이 쓴다 */
@Composable
fun DestinationChip(lang: Lang, trip: pleos.gilwe.ui.Trip, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CardShape).background(C.Panel.copy(alpha = 0.94f)).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GilweLogo(size = 36.dp, showName = false)
        Spacer(Modifier.width(14.dp))
        Column {
            LabelText("길 위")
            Text(trip.course.place.of(lang), style = Type.title)
        }
    }
}

/** 이야기 카드 (지금 이야기·진행률·일시정지·다음 이야기) — 주행 화면과 내비 위 오버레이에서 같이 쓴다 */
@Composable
fun StoryCard(
    lang: Lang,
    trip: pleos.gilwe.ui.Trip,
    audio: Lang,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    upNextWidth: androidx.compose.ui.unit.Dp = 380.dp,
    onRetryLocation: () -> Unit = {},
) {
    val s = lang.strings
    val story = trip.currentStory
    val next = trip.nextStory
    CardBox(modifier, color = C.Panel.copy(alpha = 0.96f)) {
        if (trip.waitingForDrive && story == null) {
            Text(s.waitingForDrive, style = Type.title)
            return@CardBox
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlaceArt(trip.course.art, Modifier.size(140.dp, 96.dp).clip(RoundedCornerShape(12.dp)), photo = story?.photo ?: trip.course.photo, scrim = false)
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                // 04B 내비 안내 우선 > 04C 위치 신호 없음 > 기본 (이야기 n/N)
                val label = when {
                    trip.waitingForNavi -> s.naviFirst
                    trip.locationLost -> s.locationUnavailable
                    story != null && trip.storyIndex >= 0 -> s.storyOf(trip.storyIndex + 1, trip.course.stories.size)
                    story != null -> story.subtitle.of(audio)
                    else -> s.upNext
                }
                LabelText(label)
                Text((story ?: next)?.title?.of(audio).orEmpty(), style = Type.h2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = when {
                    trip.waitingForNavi -> s.waitingForNaviBody
                    trip.locationLost -> s.waitingForLocation
                    else -> story?.subtitle?.of(audio)
                }
                if (sub != null) Text(sub, style = Type.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (story != null) {
                val showPlay = trip.storyPaused || trip.waitingForNavi
                CircleButton(if (showPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause, onTogglePause, size = 88.dp, light = true)
            }
        }
        if (story != null) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    ProgressBar(trip.storyProgress)
                }
                Spacer(Modifier.width(20.dp))
                // 오른쪽 칸: 04B 이야기 이어 듣기 / 04C 위치 다시 찾기 / 다음 이야기
                val (slotLabel, slotText, slotAction) = when {
                    trip.waitingForNavi -> Triple(s.directionsFinished, s.resumeStory, onTogglePause)
                    trip.locationLost -> Triple(s.locationLabel, s.retryLocation, onRetryLocation)
                    else -> Triple(s.upNext, next?.title?.of(audio) ?: s.noMoreStories, onNext)
                }
                val enabled = trip.waitingForNavi || trip.locationLost || next != null
                Row(
                    Modifier.width(upNextWidth).clip(RoundedCornerShape(14.dp)).background(C.CardHi)
                        .clickable(enabled = enabled, onClick = slotAction)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        LabelText(slotLabel)
                        Text(slotText, style = Type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (enabled) Icon(if (trip.locationLost && !trip.waitingForNavi) Icons.Filled.Refresh else Icons.Filled.SkipNext, null, tint = C.Text, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun ProgressBar(value: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        drawRoundRect(C.Line, cornerRadius = CornerRadius(8f))
        drawRoundRect(C.Text, size = Size(size.width * value.coerceIn(0f, 1f), size.height), cornerRadius = CornerRadius(8f))
    }
}

/** 03 도착 — P단이 되면 자동 체크인 (07), 기어를 못 읽으면 직접 확인 (07A) */
@Composable
fun ParkScreen(lang: Lang, course: Course, vehicle: VehicleState, onCheckIn: () -> Unit, onDemoTools: () -> Unit = {}) {
    val s = lang.strings
    val manual = !vehicle.gearReadable
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 56.dp, vertical = 40.dp)) {
        Row(Modifier.clickable(onClick = onDemoTools), verticalAlignment = Alignment.CenterVertically) {
            GilweLogo(size = 44.dp, showName = false)
            Spacer(Modifier.width(16.dp))
            Text(course.place.of(lang), style = Type.title)
        }
        Spacer(Modifier.height(12.dp))
        LabelText(course.parking.of(lang))
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                vehicle.isParked -> s.parkedCheckingIn
                manual -> s.finishedParking
                else -> s.parkToCheckIn
            },
            style = Type.h1,
        )
        Spacer(Modifier.height(28.dp))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            CardBox(Modifier.fillMaxHeight().aspectRatio(1.6f), color = C.Card) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ParkingGraphic(parked = vehicle.isParked)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            if (manual) s.gearUnavailable else s.shiftToP,
            style = Type.sub, modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(20.dp))
        PillButton(s.imParked, onCheckIn, Modifier.fillMaxWidth(), primary = manual || vehicle.isParked)
    }
}

@Composable
private fun ParkingGraphic(parked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(260.dp, 200.dp)) {
            val stroke = Stroke(6f)
            val c = if (parked) C.Route else C.Text
            // 주차 칸 모서리
            val l = size.width * 0.2f
            val pad = 10f
            listOf(
                Offset(pad, pad) to Offset(pad + l, pad), Offset(pad, pad) to Offset(pad, pad + l),
                Offset(size.width - pad, pad) to Offset(size.width - pad - l, pad), Offset(size.width - pad, pad) to Offset(size.width - pad, pad + l),
                Offset(pad, size.height - pad) to Offset(pad + l, size.height - pad), Offset(pad, size.height - pad) to Offset(pad, size.height - pad - l),
                Offset(size.width - pad, size.height - pad) to Offset(size.width - pad - l, size.height - pad), Offset(size.width - pad, size.height - pad) to Offset(size.width - pad, size.height - pad - l),
            ).forEach { (a, b) -> drawLine(c, a, b, strokeWidth = 6f) }
            // 차 윗모습
            val cw = size.width * 0.34f
            val ch = size.height * 0.7f
            val tl = Offset((size.width - cw) / 2, (size.height - ch) / 2)
            drawRoundRect(c, tl, Size(cw, ch), CornerRadius(40f), style = stroke)
            drawRoundRect(c, Offset(tl.x + cw * 0.15f, tl.y + ch * 0.2f), Size(cw * 0.7f, ch * 0.22f), CornerRadius(14f), style = Stroke(4f))
            drawRoundRect(c, Offset(tl.x + cw * 0.15f, tl.y + ch * 0.62f), Size(cw * 0.7f, ch * 0.16f), CornerRadius(14f), style = Stroke(4f))
        }
        Spacer(Modifier.width(32.dp))
        Box(Modifier.size(width = 40.dp, height = 2.dp).background(C.Faint))
        Spacer(Modifier.width(32.dp))
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)).background(if (parked) C.Route else C.Pill),
            contentAlignment = Alignment.Center,
        ) { Text("P", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = C.PillText) }
    }
}

/** 08 스탬프 수집 */
@Composable
fun StampCollectedScreen(
    lang: Lang,
    course: Course,
    visited: List<Course>,
    onExplore: () -> Unit,
    onMyStamps: () -> Unit,
    onSaveToPhone: () -> Unit = {},
) {
    val s = lang.strings
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 56.dp, vertical = 40.dp)) {
        // 09 스탬프 수집 완료: 로고 + 휴대폰에 저장
        Row(verticalAlignment = Alignment.CenterVertically) {
            GilweLogo(size = 44.dp, nameSize = 22)
            Spacer(Modifier.weight(1f))
            SmallPill(s.saveToPhone, onSaveToPhone)
        }
        Spacer(Modifier.height(16.dp))
        Text(course.place.of(lang), style = Type.h1)
        Spacer(Modifier.height(24.dp))
        PassportBook(lang, visited, highlight = course.id, modifier = Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PillButton(s.explorePlaces(course.placesAfterArrival.size), onExplore, Modifier.weight(1f), primary = false)
            PillButton(s.viewMyStamps, onMyStamps, Modifier.weight(1f))
        }
    }
}

/** 크림색 여권 펼침면: 왼쪽 = 이전 스탬프, 오른쪽 = 방금 받은 스탬프 */
@Composable
fun PassportBook(lang: Lang, visited: List<Course>, highlight: String?, modifier: Modifier = Modifier) {
    val s = lang.strings
    val distinct = visited.distinctBy { it.id }
    val featured = distinct.firstOrNull { it.id == highlight } ?: distinct.lastOrNull()
    val others = distinct.filter { it.id != featured?.id }.takeLast(2)
    Row(modifier.clip(RoundedCornerShape(16.dp)).background(C.Paper)) {
        Column(Modifier.weight(1f).fillMaxHeight().padding(28.dp)) {
            Text(s.visitedPlaces, fontSize = 18.sp, color = C.Ink, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (others.isEmpty()) {
                    Text(s.noStampsYet, fontSize = 18.sp, color = C.InkSoft)
                }
                others.forEachIndexed { i, c ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        StampArt(
                            c.art,
                            round = i % 2 == 0,
                            label = c.place.ko,
                            modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f),
                        )
                    }
                }
            }
            Row {
                Text("KOREA", fontSize = 16.sp, color = C.Ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(s.placesVisited(distinct.size), fontSize = 16.sp, color = C.Ink)
            }
        }
        Box(Modifier.fillMaxHeight().width(2.dp).background(C.PaperEdge))
        Box(Modifier.weight(1f).fillMaxHeight().padding(28.dp), contentAlignment = Alignment.Center) {
            if (featured != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(featured.place.en.uppercase(), fontSize = 16.sp, color = C.Ink, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    StampArt(featured.art, round = false, label = featured.place.ko, modifier = Modifier.fillMaxHeight(0.62f).aspectRatio(0.9f, matchHeightConstraintsFirst = true))
                }
            }
        }
    }
}
