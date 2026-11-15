package pleos.gilwe.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pleos.gilwe.R
import pleos.gilwe.data.ArtStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

val CardShape = RoundedCornerShape(20.dp)
val PillShape = RoundedCornerShape(50)

@Composable
fun GilweLogo(size: Dp = 48.dp, showName: Boolean = true, nameSize: Int = 24, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_gilwe), contentDescription = null, modifier = Modifier.size(size))
        if (showName) {
            Spacer(Modifier.width(size / 4))
            Text("길 위", color = C.Text, fontSize = nameSize.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 72.dp,
) {
    val bg = when {
        !enabled -> C.Disabled
        primary -> C.Pill
        else -> C.CardHi
    }
    val fg = when {
        !enabled -> C.Sub
        primary -> C.PillText
        else -> C.Text
    }
    Row(
        modifier
            .heightIn(min = height)
            .clip(PillShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = Type.button, color = fg)
    }
}

@Composable
fun SmallPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    Box(
        modifier
            .heightIn(min = 56.dp)
            .clip(PillShape)
            .background(if (selected) C.Pill else C.CardHi)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = if (selected) C.PillText else C.Text)
    }
}

@Composable
fun CircleButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 64.dp, light: Boolean = false, enabled: Boolean = true) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (light) C.Pill else C.CardHi)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (!enabled) C.Faint else if (light) C.PillText else C.Text, modifier = Modifier.size(size * 0.45f))
    }
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CircleButton(Icons.AutoMirrored.Filled.ArrowBack, onBack, size = 64.dp)
        Spacer(Modifier.width(20.dp))
        Text(title, style = Type.title, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    color: Color = C.Card,
    padding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(CardShape)
            .background(color)
            .padding(padding),
        content = content,
    )
}

@Composable
fun LabelText(text: String, modifier: Modifier = Modifier, color: Color = C.Faint) {
    Text(text.uppercase(), style = Type.label, color = color, modifier = modifier)
}

/**
 * 장소 이미지. Figma 시안 사진(photo)이 있으면 사진을, 없으면 그림(하늘 + 능선 + 건축 실루엣)을 그린다.
 * scrim=true 면 아래쪽을 어둡게 해서 위에 얹는 글자 대비(4.5:1)를 확보한다.
 */
@Composable
fun PlaceArt(
    style: ArtStyle,
    modifier: Modifier = Modifier,
    photo: Int? = null,
    scrim: Boolean = true,
    alignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier) {
        if (photo != null) {
            Image(
                painterResource(photo), contentDescription = null,
                contentScale = ContentScale.Crop, alignment = alignment,
                modifier = Modifier.matchParentSize(),
            )
            if (scrim) {
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xE6000000))))
            }
        } else {
            Canvas(Modifier.matchParentSize()) { drawPlace(style) }
        }
        content()
    }
}

private data class ArtPalette(val sky: List<Color>, val far: Color, val near: Color, val accent: Color)

private fun paletteOf(style: ArtStyle) = when (style) {
    ArtStyle.PALACE -> ArtPalette(listOf(Color(0xFF8C6A3F), Color(0xFFD9A35B), Color(0xFF3E3324)), Color(0xFF5C5236), Color(0xFF2F3A24), Color(0xFF1E1A14))
    ArtStyle.FORTRESS -> ArtPalette(listOf(Color(0xFF8FA3A8), Color(0xFFC9D2CE), Color(0xFF55665F)), Color(0xFF6E8277), Color(0xFF3A4E3E), Color(0xFF2A2F2B))
    ArtStyle.HAENGGUNG -> ArtPalette(listOf(Color(0xFF6E8FA8), Color(0xFFB9CCD6), Color(0xFF4D5E55)), Color(0xFF5E7466), Color(0xFF34473A), Color(0xFF231F1C))
    ArtStyle.FOLK_VILLAGE -> ArtPalette(listOf(Color(0xFFB98A3E), Color(0xFFE5C07A), Color(0xFF6D5A33)), Color(0xFF8A6A2E), Color(0xFF4F4426), Color(0xFF3B2E1E))
    ArtStyle.ISLAND -> ArtPalette(listOf(Color(0xFF8DA7B5), Color(0xFFD7E2E3), Color(0xFF7E9488)), Color(0xFF6F8D7A), Color(0xFF3F5D47), Color(0xFF2B3A2F))
}

private fun DrawScope.drawPlace(style: ArtStyle) {
    val p = paletteOf(style)
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(p.sky))
    // 먼 능선
    ridge(p.far, h * 0.52f, h * 0.10f, 3.1f, 0.7f)
    // 가까운 능선
    ridge(p.near, h * 0.68f, h * 0.08f, 5.3f, 2.1f)
    when (style) {
        ArtStyle.PALACE, ArtStyle.HAENGGUNG -> roof(p.accent, Offset(w * 0.5f, h * 0.62f), w * 0.46f, h * 0.2f, tiers = 2)
        ArtStyle.FORTRESS -> {
            // 성벽 + 누각
            val wall = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.3f, h * 0.66f, w * 0.6f, h * 0.74f, w, h * 0.62f)
                lineTo(w, h * 0.7f)
                cubicTo(w * 0.6f, h * 0.82f, w * 0.3f, h * 0.74f, 0f, h * 0.86f)
                close()
            }
            drawPath(wall, Color(0xFFB9B5A6))
            roof(p.accent, Offset(w * 0.68f, h * 0.6f), w * 0.24f, h * 0.12f, tiers = 1)
        }
        ArtStyle.FOLK_VILLAGE -> {
            for (i in 0..2) {
                val cx = w * (0.25f + i * 0.25f)
                val top = h * (0.64f + (i % 2) * 0.04f)
                drawOval(Color(0xFFB08A4A), Offset(cx - w * 0.11f, top), Size(w * 0.22f, h * 0.12f))
                drawRect(Color(0xFF6B5636), Offset(cx - w * 0.08f, top + h * 0.06f), Size(w * 0.16f, h * 0.1f))
            }
        }
        ArtStyle.ISLAND -> {
            drawRect(Color(0xFF7D98A3), Offset(0f, h * 0.8f), Size(w, h * 0.2f))
            for (i in 0..6) {
                val x = w * (0.18f + i * 0.11f)
                val tree = Path().apply {
                    moveTo(x, h * 0.36f + abs(3 - i) * h * 0.03f)
                    lineTo(x + w * 0.035f, h * 0.8f)
                    lineTo(x - w * 0.035f, h * 0.8f)
                    close()
                }
                drawPath(tree, Color(0xFF3E5A3F))
            }
        }
    }
    // 아래쪽 어둡게 (텍스트 대비 4.5:1 확보)
    drawRect(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xE6000000)))
}

private fun DrawScope.ridge(color: Color, base: Float, amp: Float, freq: Float, phase: Float) {
    val w = size.width
    val path = Path().apply {
        moveTo(0f, size.height)
        val steps = 40
        for (i in 0..steps) {
            val x = w * i / steps
            val t = i / steps.toFloat()
            val y = base - amp * (sin(t * freq + phase) * 0.6f + sin(t * freq * 2.3f + phase * 1.7f) * 0.4f).toFloat()
            lineTo(x, y)
        }
        lineTo(w, size.height)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.roof(color: Color, center: Offset, width: Float, height: Float, tiers: Int) {
    var cy = center.y
    var rw = width
    repeat(tiers) {
        val roof = Path().apply {
            moveTo(center.x - rw / 2, cy)
            quadraticTo(center.x - rw * 0.3f, cy - height * 0.25f, center.x - rw * 0.25f, cy - height * 0.45f)
            lineTo(center.x + rw * 0.25f, cy - height * 0.45f)
            quadraticTo(center.x + rw * 0.3f, cy - height * 0.25f, center.x + rw / 2, cy)
            close()
        }
        drawPath(roof, color)
        drawRect(color.copy(alpha = 0.85f), Offset(center.x - rw * 0.3f, cy), Size(rw * 0.6f, height * 0.35f))
        cy -= height * 0.5f
        rw *= 0.62f
    }
}

/** 스탬프 그림: 주요 역사 코스는 전용 이미지, 나머지 코스는 기존 벡터 그림을 사용한다. */
@Composable
fun StampArt(style: ArtStyle, round: Boolean, label: String, modifier: Modifier = Modifier, ink: Color = C.Ink) {
    val stampImage = when (style) {
        ArtStyle.PALACE -> R.drawable.stamp_changdeokgung
        ArtStyle.FORTRESS -> R.drawable.stamp_namhansanseong
        ArtStyle.HAENGGUNG -> R.drawable.stamp_hwaseong
        else -> null
    }

    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        if (stampImage != null) {
            Image(
                painter = painterResource(stampImage),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize().padding(4.dp),
            )
        } else {
            Canvas(Modifier.matchParentSize()) {
                val s = Stroke(width = size.minDimension * 0.022f, cap = StrokeCap.Round)
                val inset = size.minDimension * 0.06f
                if (round) {
                    drawCircle(ink, radius = size.minDimension / 2 - inset, style = s)
                    drawCircle(ink, radius = size.minDimension / 2 - inset * 2.2f, style = Stroke(size.minDimension * 0.01f))
                } else {
                    drawRect(ink, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), style = s)
                }
                val cx = size.width / 2
                val base = size.height * 0.62f
                val w = size.width * 0.5f
                // 능선
                val r = Path().apply {
                    moveTo(cx - w * 0.9f, base)
                    quadraticTo(cx - w * 0.4f, base - size.height * 0.2f, cx, base - size.height * 0.08f)
                    quadraticTo(cx + w * 0.4f, base - size.height * 0.24f, cx + w * 0.9f, base)
                }
                drawPath(r, ink.copy(alpha = 0.6f), style = Stroke(size.minDimension * 0.012f))
                when (style) {
                    ArtStyle.ISLAND -> for (i in -2..2) {
                        val x = cx + i * w * 0.22f
                        drawLine(ink, Offset(x, base - size.height * 0.22f), Offset(x, base), strokeWidth = size.minDimension * 0.02f)
                    }
                    ArtStyle.FOLK_VILLAGE -> drawArc(ink, 180f, 180f, false, Offset(cx - w * 0.4f, base - size.height * 0.14f), Size(w * 0.8f, size.height * 0.28f), style = s)
                    else -> {
                        val roof = Path().apply {
                            moveTo(cx - w * 0.55f, base - size.height * 0.08f)
                            quadraticTo(cx - w * 0.3f, base - size.height * 0.14f, cx - w * 0.28f, base - size.height * 0.2f)
                            lineTo(cx + w * 0.28f, base - size.height * 0.2f)
                            quadraticTo(cx + w * 0.3f, base - size.height * 0.14f, cx + w * 0.55f, base - size.height * 0.08f)
                        }
                        drawPath(roof, ink, style = s)
                        for (i in -2..2) {
                            val x = cx + i * w * 0.14f
                            drawLine(ink, Offset(x, base - size.height * 0.08f), Offset(x, base + size.height * 0.04f), strokeWidth = size.minDimension * 0.012f)
                        }
                    }
                }
            }
        }
        Text(
            label, color = ink, fontSize = 16.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 40.dp),
        )
    }
}

/** 스타일 지도: 도로망 + 경로 선 + 현재 위치 */
@Composable
fun RouteMap(progress: Float, modifier: Modifier = Modifier, seed: Int = 0, showCar: Boolean = true, dimmed: Boolean = false) {
    Canvas(modifier.background(C.MapBg)) {
        val w = size.width
        val h = size.height
        // 격자형 소도로
        val rnd = java.util.Random(seed.toLong() + 7)
        repeat(26) {
            val x0 = rnd.nextFloat() * w
            val y0 = rnd.nextFloat() * h
            val ang = rnd.nextFloat() * Math.PI.toFloat()
            val len = (0.2f + rnd.nextFloat() * 0.5f) * w
            drawLine(
                C.MapRoadMinor, Offset(x0, y0),
                Offset(x0 + cos(ang) * len, y0 + sin(ang) * len), strokeWidth = 3f,
            )
        }
        // 큰 도로
        repeat(5) {
            val path = Path().apply {
                val y = h * (0.15f + rnd.nextFloat() * 0.7f)
                moveTo(-20f, y)
                cubicTo(w * 0.3f, y + (rnd.nextFloat() - 0.5f) * h * 0.4f, w * 0.7f, y + (rnd.nextFloat() - 0.5f) * h * 0.4f, w + 20f, y + (rnd.nextFloat() - 0.5f) * h * 0.2f)
            }
            drawPath(path, C.MapRoad, style = Stroke(9f))
        }
        // 경로
        val route = Path().apply {
            moveTo(w * 0.5f, h * 1.05f)
            cubicTo(w * 0.5f, h * 0.7f, w * 0.38f, h * 0.55f, w * 0.48f, h * 0.38f)
            cubicTo(w * 0.56f, h * 0.24f, w * 0.6f, h * 0.12f, w * 0.58f, -h * 0.05f)
        }
        drawPath(route, C.Route.copy(alpha = if (dimmed) 0.5f else 0.9f), style = Stroke(14f, cap = StrokeCap.Round))
        if (showCar) {
            val car = Offset(w * 0.49f, h * (0.6f - 0.04f * progress))
            drawCircle(Color.White, 26f, car)
            val arrow = Path().apply {
                moveTo(car.x, car.y - 15f)
                lineTo(car.x + 11f, car.y + 12f)
                lineTo(car.x, car.y + 6f)
                lineTo(car.x - 11f, car.y + 12f)
                close()
            }
            drawPath(arrow, C.PillText)
        }
    }
}

/** 경로 미리보기용: 출발 → 도착 선 */
@Composable
fun PreviewMap(modifier: Modifier = Modifier, style: ArtStyle, photo: Int? = null) {
    PlaceArt(style, modifier, photo = photo) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Color(0x66000000))
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.62f, h * 0.18f)
                cubicTo(w * 0.2f, h * 0.2f, w * 0.12f, h * 0.45f, w * 0.38f, h * 0.5f)
                cubicTo(w * 0.62f, h * 0.55f, w * 0.7f, h * 0.7f, w * 0.5f, h * 0.82f)
            }
            drawPath(path, Color.White, style = Stroke(5f, cap = StrokeCap.Round))
            listOf(Offset(w * 0.62f, h * 0.18f), Offset(w * 0.38f, h * 0.5f), Offset(w * 0.5f, h * 0.82f)).forEach {
                drawCircle(Color.White, 11f, it)
                drawCircle(C.PillText, 5f, it)
            }
        }
    }
}

/** 내 스탬프 화면의 한반도 남쪽 실루엣 + 방문 경로 */
@Composable
fun KoreaMap(points: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val land = Path().apply {
            moveTo(w * 0.30f, h * 0.02f)
            lineTo(w * 0.62f, h * 0.0f)
            cubicTo(w * 0.78f, h * 0.18f, w * 0.86f, h * 0.36f, w * 0.88f, h * 0.55f)
            cubicTo(w * 0.9f, h * 0.72f, w * 0.82f, h * 0.86f, w * 0.66f, h * 0.92f)
            cubicTo(w * 0.5f, h * 0.98f, w * 0.3f, h * 0.96f, w * 0.18f, h * 0.88f)
            cubicTo(w * 0.1f, h * 0.76f, w * 0.16f, h * 0.62f, w * 0.12f, h * 0.48f)
            cubicTo(w * 0.08f, h * 0.36f, w * 0.22f, h * 0.3f, w * 0.18f, h * 0.18f)
            close()
        }
        drawPath(land, C.CardHi)
        val pts = listOf(
            Offset(w * 0.34f, h * 0.2f), Offset(w * 0.42f, h * 0.24f), Offset(w * 0.38f, h * 0.32f),
            Offset(w * 0.52f, h * 0.18f), Offset(w * 0.40f, h * 0.36f), Offset(w * 0.6f, h * 0.5f),
        )
        val shown = pts.take(points.coerceIn(0, pts.size))
        for (i in 1 until shown.size) drawLine(C.Sub, shown[i - 1], shown[i], strokeWidth = 3f)
        shown.forEach { drawCircle(Color.White, 8f, it) }
        drawCircle(C.Faint, 14f, Offset(w * 0.32f, h * 1.0f - 14f))
    }
}

/** "휴대폰에 저장" 용 QR 모양 (시연용 패턴) */
@Composable
fun QrPattern(seed: String, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color.White).padding(0.dp)) {
        val n = 25
        val cell = size.minDimension / (n + 2)
        val rnd = java.util.Random(seed.hashCode().toLong())
        fun finder(cx: Int, cy: Int) {
            drawRect(Color.Black, Offset((cx + 1) * cell, (cy + 1) * cell), Size(cell * 7, cell * 7))
            drawRect(Color.White, Offset((cx + 2) * cell, (cy + 2) * cell), Size(cell * 5, cell * 5))
            drawRect(Color.Black, Offset((cx + 3) * cell, (cy + 3) * cell), Size(cell * 3, cell * 3))
        }
        for (y in 0 until n) for (x in 0 until n) {
            val inFinder = (x < 8 && y < 8) || (x >= n - 8 && y < 8) || (x < 8 && y >= n - 8)
            if (!inFinder && rnd.nextBoolean()) drawRect(Color.Black, Offset((x + 1) * cell, (y + 1) * cell), Size(cell, cell))
        }
        finder(0, 0); finder(n - 7, 0); finder(0, n - 7)
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 4.dp).border(0.5.dp, C.Line))
}
