package pleos.gilwe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pleos.gilwe.data.Catalog
import pleos.gilwe.data.Course
import pleos.gilwe.data.Extras
import pleos.gilwe.data.Lang
import pleos.gilwe.data.StampRecord
import pleos.gilwe.data.Story

/** 11 내 스탬프 */
@Composable
fun MyStampsScreen(
    lang: Lang,
    records: List<StampRecord>,
    onBack: () -> Unit,
    onSaveToPhone: () -> Unit,
    onPreviewNext: (Course) -> Unit,
    onStories: (Course) -> Unit,
) {
    val s = lang.strings
    val visited = records.map { Catalog.course(it.courseId) }
    val distinct = visited.distinctBy { it.id }
    val km = records.sumOf { it.km }
    val nextCourse = Catalog.courses.firstOrNull { c -> distinct.none { it.id == c.id } } ?: Catalog.courses.first()
    val lastCourse = distinct.lastOrNull()
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 48.dp, vertical = 32.dp)) {
        BackHeader(s.myStamps, onBack) { PillButton(s.saveToPhone, onSaveToPhone, height = 56.dp) }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            CardBox(Modifier.width(320.dp).fillMaxHeight()) {
                LabelText(s.placesCollected)
                Text("%02d".format(distinct.size), fontSize = 64.sp, color = C.Text, fontWeight = FontWeight.Medium)
                Text(s.ofPlaces(Catalog.TOTAL_PLACES), style = Type.sub)
                Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) { KoreaMap(distinct.size, Modifier.aspectRatio(0.62f)) }
                Text(s.kmTravelled(km), style = Type.body, fontWeight = FontWeight.Medium)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                // 11A: 저장 시각으로 여행 단위를 묶어 이번 여행 / 지난 여행을 나눠 보여 준다
                val trips = remember(records) { Extras.trips(records) }
                val thisTrip = trips.lastOrNull().orEmpty()
                val earlier = trips.dropLast(1).flatten()
                var showEarlier by rememberSaveable { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.passportStamps, style = Type.h2, modifier = Modifier.weight(1f))
                    Text(s.tripsSaved(trips.size), style = Type.sub)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallPill("${s.thisTrip} ${thisTrip.size}", { showEarlier = false }, selected = !showEarlier)
                    SmallPill("${s.earlierTrips} ${earlier.size}", { showEarlier = true }, selected = showEarlier)
                }
                Spacer(Modifier.height(12.dp))
                val shown = (if (showEarlier) earlier else thisTrip).map { Catalog.course(it.courseId) }
                if (showEarlier && earlier.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(s.noEarlierTrips, style = Type.sub) }
                } else {
                    PassportBook(lang, shown, highlight = if (showEarlier) null else lastCourse?.id, modifier = Modifier.weight(1f).fillMaxWidth())
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PillButton(s.previewNextRoute, { onPreviewNext(nextCourse) }, Modifier.weight(1f))
                    PillButton(
                        s.routeStories, { lastCourse?.let(onStories) }, Modifier.weight(1f),
                        primary = false, enabled = lastCourse != null,
                    )
                }
            }
        }
    }
}

/** 12 다시 들을 이야기 */
@Composable
fun RouteStoriesScreen(
    lang: Lang,
    audio: Lang,
    course: Course,
    record: StampRecord?,
    speakingId: String?,
    onBack: () -> Unit,
    onPlay: (Story) -> Unit,
    onOpen: (Story) -> Unit = {},
) {
    val s = lang.strings
    val start = Catalog.startPoints.firstOrNull { it.id == record?.startId } ?: Catalog.startPoints.first()
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 48.dp, vertical = 32.dp)) {
        BackHeader(s.routeStories, onBack)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            CardBox(Modifier.width(340.dp).fillMaxHeight()) {
                LabelText(s.driveCompleted)
                Spacer(Modifier.height(8.dp))
                Text("${start.name.of(lang)}\n→ ${course.place.of(lang)}", style = Type.title)
                Spacer(Modifier.height(16.dp))
                PreviewMap(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)), course.art, course.photo)
                Spacer(Modifier.height(16.dp))
                Text(s.routeStoriesCount(course.stories.size), style = Type.body, fontWeight = FontWeight.Medium)
                Text(s.listenAgain, style = Type.sub)
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 맨 위에 코스 소개(00), 이어서 경로 이야기
                itemsIndexed(listOf(Extras.introStory(course)) + course.stories, key = { _, st -> st.id }) { i, st ->
                    val playing = speakingId == st.id
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (playing) C.CardHi else C.Card)
                            .clickable { onOpen(st) }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("%02d".format(i), fontSize = 20.sp, color = C.Faint)
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(st.title.of(audio), style = Type.body, fontWeight = FontWeight.Medium)
                            Text(st.subtitle.of(audio), style = Type.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        CircleButton(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, { onPlay(st) }, light = true)
                    }
                }
            }
        }
    }
}

/** 05 휴대폰으로 이어보기 — QR 안내 */
@Composable
fun SaveToPhoneDialog(lang: Lang, records: List<StampRecord>, onDismiss: () -> Unit) {
    val s = lang.strings
    Dialog(onDismissRequest = onDismiss) {
        CardBox(Modifier.width(560.dp), color = C.Card, padding = androidx.compose.foundation.layout.PaddingValues(36.dp)) {
            Text(s.saveToPhone, style = Type.h2)
            Spacer(Modifier.height(12.dp))
            Text(s.saveToPhoneBody, style = Type.sub)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                QrPattern(records.joinToString { it.courseId + it.timestamp }, Modifier.size(260.dp).clip(RoundedCornerShape(12.dp)))
            }
            Spacer(Modifier.height(28.dp))
            PillButton(s.close, onDismiss, Modifier.fillMaxWidth())
        }
    }
}
