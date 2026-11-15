package pleos.gilwe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pleos.gilwe.R
import pleos.gilwe.data.ArtStyle
import pleos.gilwe.data.Lang

/** 00 서비스 소개 */
@Composable
fun IntroScreen(lang: Lang, hasHistory: Boolean, onLang: () -> Unit, onStart: () -> Unit, onContinue: () -> Unit) {
    val s = lang.strings
    Box(Modifier.fillMaxSize().background(C.Bg)) {
        // 오른쪽: Figma 원본 루프 영상 (Entry / Korean ink landscape)
        // 화면 전체를 채우고 오른쪽 끝 기준으로 맞춰서 도로·차가 잘리지 않게 함
        HeroVideo(Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to C.Bg, 0.3f to C.Bg, 0.5f to C.Bg.copy(alpha = 0.55f), 0.7f to Color.Transparent),
            ),
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GilweLogo(size = 64.dp, nameSize = 32)
                Spacer(Modifier.weight(1f))
                SmallPill(lang.label, onLang)
            }
            Spacer(Modifier.weight(1f))
            Text(s.helloTitle, style = Type.hero)
            Spacer(Modifier.height(24.dp))
            Text(s.helloHeadline, style = Type.h1.copy(fontSize = 44.sp, lineHeight = 54.sp))
            Spacer(Modifier.height(20.dp))
            Text(s.helloBody, style = Type.sub)
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PillButton(s.getStarted, onStart, Modifier.width(260.dp))
                PillButton(s.continueLabel, onContinue, Modifier.width(260.dp), primary = false, enabled = hasHistory)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 소개 화면 배경 영상: 무음 반복 재생, 화면을 꽉 채우도록 가운데 기준으로 잘라 보여줌 */
@Composable
private fun HeroVideo(modifier: Modifier) {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier.clip(RoundedCornerShape(0.dp)),
        factory = { ctx ->
            android.view.TextureView(ctx).apply {
                val player = android.media.MediaPlayer()
                surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, w: Int, h: Int) {
                        val afd = ctx.resources.openRawResourceFd(R.raw.intro_hero)
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        player.setSurface(android.view.Surface(st))
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                        player.setOnVideoSizeChangedListener { _, vw, vh -> centerCrop(this@apply, vw, vh) }
                        player.setOnPreparedListener { it.start() }
                        player.prepareAsync()
                    }
                    override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, w: Int, h: Int) {
                        centerCrop(this@apply, player.videoWidth, player.videoHeight)
                    }
                    override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                        player.release(); return true
                    }
                    override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
                }
            }
        },
    )
}

private fun centerCrop(v: android.view.TextureView, vw: Int, vh: Int) {
    if (vw <= 0 || vh <= 0 || v.width == 0 || v.height == 0) return
    val sx = v.width.toFloat() / vw
    val sy = v.height.toFloat() / vh
    val s = maxOf(sx, sy)
    val m = android.graphics.Matrix()
    // 오른쪽 끝 기준 확대: 넘치는 부분은 왼쪽(그라데이션으로 가려지는 쪽)에서 잘림
    m.setScale(vw * s / v.width, vh * s / v.height, v.width.toFloat(), v.height / 2f)
    v.setTransform(m)
}

/** 00A 화면 언어 선택 / 오디오 언어 선택 (같은 레이아웃) */
@Composable
fun LanguageScreen(
    title: String,
    selected: Lang?,
    onBack: () -> Unit,
    onPick: (Lang) -> Unit,
    header: String,
) {
    Column(Modifier.fillMaxSize().background(C.Panel).padding(horizontal = 56.dp, vertical = 40.dp)) {
        BackHeader(header, onBack)
        Spacer(Modifier.height(40.dp))
        Text(title, style = Type.h2)
        Spacer(Modifier.height(32.dp))
        Lang.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { l ->
                    val isSel = l == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 120.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSel) C.CardHi else C.Card)
                            .then(if (isSel) Modifier.border(2.dp, C.Pill, RoundedCornerShape(18.dp)) else Modifier)
                            .clickable { onPick(l) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(l.label, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = C.Text)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
