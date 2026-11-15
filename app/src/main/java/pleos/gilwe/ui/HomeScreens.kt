package pleos.gilwe.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pleos.gilwe.data.Catalog
import pleos.gilwe.data.Category
import pleos.gilwe.data.Course
import pleos.gilwe.data.Lang
import pleos.gilwe.data.StartPoint
import kotlin.math.absoluteValue

@Composable
fun AppTopBar(lang: Lang, onLang: () -> Unit, onStamps: () -> Unit, onLogoLongPress: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable(onClick = onLogoLongPress)) { GilweLogo(size = 52.dp, nameSize = 26) }
        Spacer(Modifier.weight(1f))
        SmallPill(lang.code, onLang)
        Spacer(Modifier.width(12.dp))
        SmallPill(lang.strings.myStamps, onStamps)
    }
}

@Composable
fun LocationBar(label: String, name: String, change: String, onChange: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Card).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocationOn, null, tint = C.Sub, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            LabelText(label)
            Text(name, style = Type.body, fontWeight = FontWeight.Medium)
        }
        SmallPill(change, onChange)
    }
}

/** 01 홈 — K-History / K-Scene 캐러셀 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    lang: Lang,
    start: StartPoint,
    category: Category,
    onCategory: (Category) -> Unit,
    onLang: () -> Unit,
    onStamps: () -> Unit,
    onChangeStart: () -> Unit,
    onExplore: (Course) -> Unit,
    onDemoTools: () -> Unit,
) {
    val s = lang.strings
    val courses = Catalog.courses.filter { it.category == category }
    val isDefaultStart = start.id == Catalog.startPoints.first().id
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 48.dp, vertical = 32.dp)) {
        AppTopBar(lang, onLang, onStamps, onDemoTools)
        Spacer(Modifier.height(20.dp))
        LocationBar(if (isDefaultStart) s.currentLocation else s.startingPoint, start.name.of(lang), s.change, onChangeStart)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallPill(s.kHistory, { onCategory(Category.HISTORY) }, selected = category == Category.HISTORY, modifier = Modifier.width(200.dp))
            SmallPill(s.kScene, { onCategory(Category.SCENE) }, selected = category == Category.SCENE, modifier = Modifier.width(200.dp))
        }
        Spacer(Modifier.height(20.dp))

        // 카테고리를 바꾸면 캐러셀 전체를 새로 만든다: 항상 첫 장부터, 이전 카테고리의 장 수가 남지 않게
        androidx.compose.runtime.key(category) {
        val pager = rememberPagerState { courses.size }
        val scope = rememberCoroutineScope()
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val cardWidth = (maxWidth * 0.56f)
            val side = (maxWidth - cardWidth) / 2
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = side),
                pageSpacing = 20.dp,
                modifier = Modifier.fillMaxSize(),
                key = { courses[it].id },
            ) { page ->
                val c = courses[page]
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue
                CourseCard(
                    c, lang, start, index = page, total = courses.size,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val k = 1f - (offset.coerceIn(0f, 1f) * 0.08f)
                        scaleY = k
                        alpha = 1f - offset.coerceIn(0f, 1f) * 0.45f
                    },
                    onClick = { onExplore(c) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, enabled = pager.currentPage > 0)
            Spacer(Modifier.width(24.dp))
            PillButton(s.exploreRoute, { onExplore(courses[pager.currentPage.coerceIn(courses.indices)]) }, Modifier.width(420.dp))
            Spacer(Modifier.width(24.dp))
            CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, enabled = pager.currentPage < courses.lastIndex)
        }
        }
    }
}

@Composable
private fun CourseCard(c: Course, lang: Lang, start: StartPoint, index: Int, total: Int, modifier: Modifier, onClick: () -> Unit) {
    val s = lang.strings
    val minutes = (c.minutesFromSeoul + start.minutesDelta).coerceAtLeast(5)
    val km = (c.kmFromSeoul + start.kmDelta).coerceAtLeast(2)
    PlaceArt(c.art, modifier.clip(CardShape).clickable(onClick = onClick), photo = c.photo) {
        Box(
            Modifier.align(Alignment.TopEnd).padding(20.dp).clip(RoundedCornerShape(50)).background(C.Bg.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) { Text("${index + 1} / $total", fontSize = 18.sp, color = C.Text) }
        Column(Modifier.align(Alignment.BottomStart).padding(28.dp)) {
            Text(c.place.of(lang), style = Type.h2)
            Spacer(Modifier.height(6.dp))
            Text(c.tagline.of(lang), style = Type.body)
            Spacer(Modifier.height(4.dp))
            Text(c.region.of(lang), style = Type.sub)
            Spacer(Modifier.height(10.dp))
            Text("$minutes ${s.minDrive}  ·  $km ${s.km}", style = Type.body, fontWeight = FontWeight.Medium)
        }
    }
}

/** 01A 출발지 변경 */
@Composable
fun StartSearchScreen(
    lang: Lang,
    showResults: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onChoose: (StartPoint) -> Unit,
) {
    val s = lang.strings
    val results = Catalog.startPoints.filter {
        query.isBlank() || it.name.en.contains(query, true) || it.name.ko.contains(query) || it.address.of(lang).contains(query, true)
    }
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 56.dp, vertical = 40.dp)) {
        BackHeader(if (showResults) s.chooseSearchResult else s.whereStarting, onBack)
        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(RoundedCornerShape(16.dp)).background(C.Card).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = C.Sub, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(16.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(s.searchHint, style = Type.body, color = C.Faint)
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = Type.body,
                    cursorBrush = SolidColor(C.Text),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSubmit() }, onDone = { onSubmit() }),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                )
            }
            if (!showResults) CircleButton(Icons.Filled.Search, onSubmit, size = 56.dp)
        }
        Spacer(Modifier.height(24.dp))
        if (!showResults) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Card).clickable { onChoose(Catalog.startPoints.first()) }
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MyLocation, null, tint = C.Text, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text(s.useCurrentLocation, style = Type.body)
            }
            Spacer(Modifier.height(24.dp))
            LabelText(s.recentPlaces)
            Spacer(Modifier.height(12.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(if (showResults) results else Catalog.startPoints.drop(1), key = { it.id }) { p ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Card).padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.LocationOn, null, tint = C.Sub, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name.of(lang), style = Type.body, fontWeight = FontWeight.Medium)
                        Text(p.address.of(lang), style = Type.sub)
                    }
                    SmallPill(s.choose, { onChoose(p) })
                }
            }
        }
    }
}
