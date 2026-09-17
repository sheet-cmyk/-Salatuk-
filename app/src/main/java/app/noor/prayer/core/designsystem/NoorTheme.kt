package app.noor.prayer.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.noor.prayer.domain.Appearance

val Emerald = Color(0xFF103F35)
val Gold = Color(0xFFE4CF99)
private val Light = lightColorScheme(primary = Emerald, onPrimary = Color.White, primaryContainer = Color(0xFFDCE8DD), onPrimaryContainer = Emerald, secondary = Color(0xFF79663F), onSecondary = Color.White, secondaryContainer = Color(0xFFDCE8DD), onSecondaryContainer = Emerald, background = Color(0xFFF7F5EE), onBackground = Color(0xFF1D342C), surface = Color(0xFFFCFBF7), surfaceTint = Emerald, surfaceVariant = Color(0xFFECEEE5), onSurface = Color(0xFF1D342C), onSurfaceVariant = Color(0xFF59655C), outlineVariant = Color(0xFFDDE2D8))
private val Dark = darkColorScheme(primary = Color(0xFFA5D3BC), onPrimary = Emerald, primaryContainer = Color(0xFF204E40), onPrimaryContainer = Color(0xFFE6F1E9), secondary = Gold, onSecondary = Color(0xFF392F16), secondaryContainer = Color(0xFF204E40), onSecondaryContainer = Color(0xFFE6F1E9), background = Color(0xFF0C1E19), onBackground = Color(0xFFE4EBDF), surface = Color(0xFF142A23), surfaceTint = Color(0xFFA5D3BC), surfaceVariant = Color(0xFF20392F), onSurface = Color(0xFFE4EBDF), onSurfaceVariant = Color(0xFFAFBFB2), outlineVariant = Color(0xFF30473D))
@Composable
fun NoorTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = appearance == Appearance.DARK || (appearance == Appearance.SYSTEM && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if(dark) Dark else Light, typography = Typography(
        displayLarge = TextStyle(fontFamily = FontFamily.Serif,fontWeight = FontWeight.Normal,fontSize = 60.sp,lineHeight = 68.sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.Serif,fontSize = 34.sp,lineHeight = 42.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Serif,fontSize = 28.sp,lineHeight = 36.sp)
    ),content = content)
}
