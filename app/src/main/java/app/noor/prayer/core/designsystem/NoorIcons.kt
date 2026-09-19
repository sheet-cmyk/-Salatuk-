package app.noor.prayer.core.designsystem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Compact outlined vector set; no full icon-font dependency. */
object NoorIcons {
    private fun icon(name: String, data: String, mirror: Boolean = false) = ImageVector.Builder(name,24.dp,24.dp,24f,24f,autoMirror = mirror).addPath(PathParser().parsePathString(data).toNodes(),stroke = SolidColor(Color.Black),strokeLineWidth = 1.7f).build()
    val Home = icon("Home","M3,11 L12,3 L21,11 M5,10 L5,21 L10,21 L10,15 L14,15 L14,21 L19,21 L19,10")
    val CalendarMonth = icon("CalendarMonth","M4,5 L20,5 L20,21 L4,21 Z M4,10 L20,10 M8,3 L8,7 M16,3 L16,7 M8,14 L10,14 M14,14 L16,14 M8,17 L10,17")
    val Explore = icon("Explore","M12,2 A10,10 0,1 1,11.99,2 M16,8 L14,14 L8,16 L10,10 Z")
    val Tune = icon("Tune","M4,6 L20,6 M4,12 L20,12 M4,18 L20,18 M8,3 L8,9 M16,9 L16,15 M10,15 L10,21")
    val MyLocation = icon("MyLocation","M12,2 L12,5 M12,19 L12,22 M2,12 L5,12 M19,12 L22,12 M12,5 A7,7 0,1 1,11.99,5 M12,9 A3,3 0,1 1,11.99,9")
    val WbTwilight = icon("WbTwilight","M2,17 L22,17 M6,15 A6,6 0,0 1,18,15 M12,3 L12,6 M3,8 L5,10 M21,8 L19,10 M5,21 L19,21")
    val WbSunny = icon("WbSunny","M12,7 A5,5 0,1 1,11.99,7 M12,1 L12,4 M12,20 L12,23 M1,12 L4,12 M20,12 L23,12 M4,4 L6,6 M18,18 L20,20 M4,20 L6,18 M18,6 L20,4")
    val LightMode = icon("LightMode","M12,7 A5,5 0,1 1,11.99,7 M12,1 L12,4 M12,20 L12,23 M1,12 L4,12 M20,12 L23,12")
    val WbCloudy = icon("WbCloudy","M6,18 A4,4 0,0 1,6,10 A6,6 0,0 1,18,10 A4,4 0,0 1,18,18 Z")
    val DarkMode = icon("DarkMode","M20,15 A9,9 0,1 1,9,4 A8,8 0,0 0,20,15 Z")
    val Alarm = icon("Alarm","M12,5 A8,8 0,1 1,11.99,5 M12,8 L12,13 L15,15 M3,5 L6,2 M18,2 L21,5 M6,20 L4,22 M18,20 L20,22")
    val VolumeUp = icon("VolumeUp","M3,9 L7,9 L12,5 L12,19 L7,15 L3,15 Z M16,8 Q20,12 16,16 M19,5 Q25,12 19,19")
    val AudioFile = icon("AudioFile","M5,2 L14,2 L19,7 L19,22 L5,22 Z M14,2 L14,7 L19,7 M10,17 A2,2 0,1 1,9.99,17 M12,17 L12,10 L16,11")
    val HelpOutline = icon("HelpOutline","M12,2 A10,10 0,1 1,11.99,2 M9,8 Q9,4 13,6 Q17,8 12,11 L12,14 M12,17 L12,18")
    val Remove = icon("Remove","M5,12 L19,12")
    val Add = icon("Add","M5,12 L19,12 M12,5 L12,19")
    val ExpandMore = icon("ExpandMore","M6,9 L12,15 L18,9")
    val KeyboardArrowLeft = icon("KeyboardArrowLeft","M15,5 L8,12 L15,19", true)
    val KeyboardArrowRight = icon("KeyboardArrowRight","M9,5 L16,12 L9,19", true)
    val Radio = icon("Radio","M4,10 L20,10 L20,19 L4,19 Z M6,10 L4,4 M17,4 Q20,6 17,8 M8,14 A1,1 0,1 1,7.99,14 M11,13 L17,13 M11,16 L15,16")
    val PlayArrow = icon("PlayArrow","M7,4 L19,12 L7,20 Z")
    val Pause = icon("Pause","M7,4 L11,4 L11,20 L7,20 Z M13,4 L17,4 L17,20 L13,20 Z")
    val SkipNext = icon("SkipNext","M6,5 L16,12 L6,19 Z M17,5 L17,19")
    val SkipPrevious = icon("SkipPrevious","M18,5 L8,12 L18,19 Z M7,5 L7,19")
}
