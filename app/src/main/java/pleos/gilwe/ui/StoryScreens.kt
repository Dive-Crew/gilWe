package pleos.gilwe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pleos.gilwe.data.Course
import pleos.gilwe.data.Extras
import pleos.gilwe.data.Lang
import pleos.gilwe.data.Story
import pleos.gilwe.voice.Playback

private fun mmss(ms: Int): String = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)

/** 주행 규제 중에는 이야기 화면을 열지 않는다 */
@Composable
private fun LockedWhileDriving(lang: Lang, title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 48.dp, vertical = 32.dp)) {
        BackHeader(title, onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Lock, null, tint = C.Sub, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text(lang.strings.drivingLimited, style = Type.title)
            }
        }
    }
}

@Composable
private fun QuestionCard(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(C.Card).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Lightbulb, null, tint = Color.Amber, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = Type.body, color = C.Text)
    }
}

private object Color { val Amber = androidx.compose.ui.graphics.Color(0xFFE8C26A) }

/** 06 이야기 듣기 (Listen) — 사진, 문장 자막, 진행·앞뒤 이동, 질문, 다음 이야기, 전문 읽기 */
@Composable
fun StoryListenScreen(
    lang: Lang,
    audio: Lang,
    course: Course,
    story: Story,
    playback: Playback?,
    playing: Boolean,
    restricted: Boolean,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipLine: (Int) -> Unit,
    onOpenStory: (Story) -> Unit,
    onFull: () -> Unit,
) {
    val s = lang.strings
    if (restricted) { LockedWhileDriving(lang, s.listen, onBack); return }
    var captions by rememberSaveable { mutableStateOf(true) }
    val idx = course.stories.indexOf(story)
    val next = course.stories.getOrNull(idx + 1)
    val pb = playback?.takeIf { it.id == story.id }
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 48.dp, vertical = 32.dp)) {
        BackHeader(s.listen, onBack)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.width(520.dp).fillMaxHeight()) {
                PlaceArt(course.art, Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(20.dp)), photo = story.photo ?: course.photo, scrim = false)
                Spacer(Modifier.height(20.dp))
                Extras.question(story.id)?.let { QuestionCard(it.of(audio), Modifier.fillMaxWidth()) }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                LabelText(s.nowPlaying)
                Spacer(Modifier.height(6.dp))
                Text(story.title.of(audio), style = Type.h1, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(story.subtitle.of(audio), style = Type.sub)
                Spacer(Modifier.height(20.dp))
                // 문장 자막: 지금 문장은 크게, 다음 문장은 흐리게
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    if (captions) {
                        val line = pb?.line ?: story.script.of(audio)
                        Text(line, fontSize = 30.sp, lineHeight = 42.sp, color = C.Text, fontWeight = FontWeight.Medium)
                        pb?.nextLine?.let {
                            Spacer(Modifier.height(14.dp))
                            Text(it, style = Type.body, color = C.Faint, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                ProgressBar(pb?.fraction ?: 0f)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(mmss(pb?.elapsedMs ?: 0), style = Type.sub)
                    Spacer(Modifier.weight(1f))
                    Text(mmss(pb?.totalMs ?: 0), style = Type.sub)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    CircleButton(Icons.Filled.Replay10, { onSkipLine(-1) }, enabled = pb != null)
                    Spacer(Modifier.width(20.dp))
                    CircleButton(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, onTogglePlay, size = 88.dp, light = true)
                    Spacer(Modifier.width(20.dp))
                    CircleButton(Icons.Filled.Forward10, { onSkipLine(1) }, enabled = pb != null)
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.clip(PillShape).background(if (captions) C.Pill else C.CardHi).clickable { captions = !captions }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Subtitles, null, tint = if (captions) C.PillText else C.Text, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.captions, style = Type.button, color = if (captions) C.PillText else C.Text)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (next != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.CardHi).clickable { onOpenStory(next) }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaceArt(course.art, Modifier.size(64.dp, 44.dp).clip(RoundedCornerShape(8.dp)), photo = next.photo ?: course.photo, scrim = false)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            LabelText(s.upNext)
                            Text(next.title.of(audio), style = Type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Filled.SkipNext, null, tint = C.Text, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    s.readFullStory, style = Type.body, color = C.Sub,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onFull).padding(vertical = 8.dp, horizontal = 4.dp),
                )
            }
        }
    }
}

/** 06 이야기 전문 (Full transcript) */
@Composable
fun StoryFullScreen(
    lang: Lang,
    audio: Lang,
    course: Course,
    story: Story,
    restricted: Boolean,
    onBack: () -> Unit,
    onBackToListening: () -> Unit,
) {
    val s = lang.strings
    if (restricted) { LockedWhileDriving(lang, s.fullStory, onBack); return }
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 48.dp, vertical = 32.dp)) {
        BackHeader(s.fullStory, onBack)
        Spacer(Modifier.height(20.dp))
        CardBox(Modifier.weight(1f).fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(32.dp)) {
            val info = Extras.fullInfo(story.id)
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text((info?.title ?: story.title).of(audio), style = Type.h1)
                Text("${course.place.of(audio)} · ${story.subtitle.of(audio)}", style = Type.sub)
                Spacer(Modifier.height(18.dp))
                Extras.question(story.id)?.let { QuestionCard(it.of(audio)); Spacer(Modifier.height(20.dp)) }
                Text(story.script.of(audio), fontSize = 26.sp, lineHeight = 40.sp, color = C.Text)
                // 알아 두세요: Figma 전문의 안내 상자, 없으면 코스의 도착·주차 안내
                val note: Pair<String, String>? = info?.note?.let { (t, b) -> t.of(audio) to b.of(audio) }
                    ?: (course.arrivalNote ?: course.parkingNote)?.let { s.goodToKnow to it.of(audio) }
                if (note != null) {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, C.Line, RoundedCornerShape(14.dp)).padding(18.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Filled.Info, null, tint = C.Sub, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(note.first, style = Type.body, fontWeight = FontWeight.Medium)
                            Text(note.second, style = Type.sub)
                        }
                    }
                }
                // 출처 (Figma 06)
                if (!info?.sources.isNullOrEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    LabelText(s.sources)
                    Spacer(Modifier.height(6.dp))
                    info!!.sources.forEach { Text(it, style = Type.sub, color = C.Faint) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PillButton(s.backToListening, onBackToListening, Modifier.fillMaxWidth(), primary = false)
    }
}

/** 10 주변 장소 둘러보기 — 주차장에서 걸어서 갈 수 있는 지점 */
@Composable
fun NearbyScreen(lang: Lang, course: Course, onBack: () -> Unit, onMyStamps: () -> Unit) {
    val s = lang.strings
    val spots = Extras.walkSpots(course.id)
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 48.dp, vertical = 32.dp)) {
        BackHeader(s.exploreTitle(course.place.of(lang)), onBack)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            spots.forEach { sp ->
                CardBox(Modifier.weight(1f).fillMaxHeight(), padding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
                    PlaceArt(course.art, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)), photo = sp.photo, scrim = false) // 지점 사진이 없으면 그림 (다른 건물 사진으로 오해하지 않게)
                    Spacer(Modifier.height(16.dp))
                    Text(sp.name.en, style = Type.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sp.name.ko, style = Type.sub)
                    Spacer(Modifier.height(4.dp))
                    Text(sp.desc.of(lang), style = Type.sub, color = C.Faint, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.heightIn(min = 52.dp))
                    Spacer(Modifier.height(12.dp))
                    if (sp.meters != null) {
                        Text(if (sp.meters >= 1000) "%.1f km".format(sp.meters / 1000f) else "${sp.meters} m", style = Type.h2)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsWalk, null, tint = C.Sub, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s.minWalk(sp.minutes ?: 0), style = Type.sub)
                        }
                    } else {
                        Text(s.placesHere, style = Type.sub)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(s.fromParking(course.parking.of(lang)), style = Type.sub, color = C.Faint)
        Spacer(Modifier.height(16.dp))
        PillButton(s.viewMyStamps, onMyStamps, Modifier.fillMaxWidth())
    }
}
