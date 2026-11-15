package pleos.gilwe.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Figma "[HMG] 길 위 Prototype" 다크 HMI 팔레트 */
object C {
    val Bg = Color(0xFF0B0D0F)
    val Panel = Color(0xFF131518)
    val Card = Color(0xFF1C1F23)
    val CardHi = Color(0xFF24282D)
    val Line = Color(0xFF2C3036)
    val Text = Color(0xFFF4F5F6)
    val Sub = Color(0xFFA3A9B0)
    val Faint = Color(0xFF6E757D)
    val Pill = Color(0xFFF1F1EF)
    val PillText = Color(0xFF15171A)
    val Disabled = Color(0xFF3A3E43)
    val Route = Color(0xFFBFF3D6)
    val MapBg = Color(0xFF1A1D20)
    val MapRoad = Color(0xFF30353A)
    val MapRoadMinor = Color(0xFF24282C)
    val Paper = Color(0xFFEDE9DD)
    val PaperEdge = Color(0xFFD9D3C2)
    val Ink = Color(0xFF3F5A47)
    val InkSoft = Color(0xFF6E8873)
}

object Type {
    val serif = FontFamily.Serif
    val hero = TextStyle(fontFamily = serif, fontSize = 112.sp, lineHeight = 120.sp, color = C.Text)
    val h1 = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold, color = C.Text)
    val h2 = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, color = C.Text)
    val title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium, color = C.Text)
    val body = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, color = C.Text)
    val sub = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, color = C.Sub)
    val label = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 1.sp, color = C.Faint, fontWeight = FontWeight.Medium)
    val button = TextStyle(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun GilweTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = C.Bg,
            surface = C.Panel,
            primary = C.Pill,
            onPrimary = C.PillText,
            onSurface = C.Text,
        ),
        content = content,
    )
}
