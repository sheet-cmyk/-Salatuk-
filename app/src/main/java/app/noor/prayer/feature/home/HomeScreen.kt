package app.noor.prayer.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import app.noor.prayer.core.designsystem.NoorIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.noor.prayer.R
import app.noor.prayer.core.common.clockPattern
import app.noor.prayer.core.designsystem.*
import app.noor.prayer.core.notifications.labelResource
import app.noor.prayer.domain.PrayerName
import app.noor.prayer.feature.PrayerUiState
import java.time.Duration
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun HomeScreen(state: PrayerUiState,exact: Boolean,settings: () -> Unit) {
    val location = requireNotNull(state.settings.location)
    val date = state.now.atZone(location.zone()).toLocalDate()
    val hijri = runCatching { HijrahDate.from(date).plus(state.settings.hijriOffset.toLong(),ChronoUnit.DAYS).format(DateTimeFormatter.ofPattern("d MMMM yyyy",Locale.getDefault())) }.getOrDefault("")
    val context = LocalContext.current
    val clock = DateTimeFormatter.ofPattern(clockPattern(state.settings.clockFormat,context),Locale.US)
    LazyColumn(Modifier.fillMaxSize(),contentPadding = PaddingValues(24.dp),verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.greeting),style = MaterialTheme.typography.labelLarge,color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(location.city.ifBlank { stringResource(R.string.current_location) },style = MaterialTheme.typography.headlineMedium)
                    if(location.country.isNotBlank()) Text(location.country,style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),style = MaterialTheme.typography.bodyMedium)
                Text("$hijri ${stringResource(R.string.hijri_suffix)}",style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Emerald,contentColor = Color.White),shape = RoundedCornerShape(28.dp)) {
                Box(Modifier.fillMaxWidth()) {
                    MosquePattern(Modifier.matchParentSize())
                    Column(Modifier.padding(28.dp),verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.next_prayer),color = Gold,style = MaterialTheme.typography.labelLarge)
                        Text(state.next?.let { stringResource(it.name.labelResource()) } ?: "—",style = MaterialTheme.typography.headlineMedium)
                        Text(state.next?.instant?.atZone(location.zone())?.format(clock) ?: "—",style = MaterialTheme.typography.displayLarge)
                        if(state.next?.instant?.atZone(location.zone())?.toLocalDate()?.isAfter(date) == true) Text(stringResource(R.string.tomorrow),color = Gold)
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.remaining),style = MaterialTheme.typography.labelSmall,color = Gold)
                        val seconds = state.next?.let { Duration.between(state.now,it.instant).seconds.coerceAtLeast(0) } ?: 0
                        Text(String.format(Locale.US,"%02d:%02d:%02d",seconds / 3600,seconds / 60 % 60,seconds % 60),fontFamily = FontFamily.Monospace,style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        if(!exact) item {
            OutlinedCard(onClick = settings) { Row(Modifier.padding(16.dp),verticalAlignment = Alignment.CenterVertically) { Icon(NoorIcons.Alarm,null); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.exact_title),Modifier.weight(1f),style = MaterialTheme.typography.bodyMedium) } }
        }
        if(state.calculationError) item { Text(stringResource(R.string.calculation_failed),color = MaterialTheme.colorScheme.error) }
        item { Text(stringResource(R.string.prayer_times),style = MaterialTheme.typography.titleLarge) }
        items(state.daily?.times.orEmpty(),key = { it.name }) { prayer ->
            val selected = prayer.name == state.next?.name && prayer.instant == state.next.instant
            val icon = when(prayer.name) { PrayerName.FAJR -> NoorIcons.WbTwilight; PrayerName.SUNRISE -> NoorIcons.WbSunny; PrayerName.DHUHR -> NoorIcons.LightMode; PrayerName.ASR -> NoorIcons.WbCloudy; PrayerName.MAGHRIB -> NoorIcons.WbTwilight; PrayerName.ISHA -> NoorIcons.DarkMode }
            Surface(shape = RoundedCornerShape(18.dp),color = if(selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp,vertical = 16.dp),verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon,null,Modifier.size(24.dp),tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp)); Text(stringResource(prayer.name.labelResource()),Modifier.weight(1f),style = MaterialTheme.typography.titleMedium)
                    if(prayer.name.obligatory && state.settings.adhan.enabled && prayer.name in state.settings.adhan.prayers) { Icon(NoorIcons.VolumeUp,null,Modifier.size(16.dp),tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(12.dp)) }
                    Text(prayer.instant.atZone(location.zone()).format(clock),style = MaterialTheme.typography.titleLarge,fontFamily = FontFamily.Monospace)
                }
            }
        }
        item { Text(stringResource(R.string.on_device),style = MaterialTheme.typography.labelSmall,color = MaterialTheme.colorScheme.onSurfaceVariant,modifier = Modifier.padding(top = 8.dp,bottom = 12.dp)) }
    }
}
@Composable
private fun MosquePattern(modifier: Modifier) {
    Canvas(modifier) {
        val faint = Gold.copy(alpha = .08f)
        val step = 52.dp.toPx()
        for(x in 0..(size.width / step).toInt()) for(y in 0..(size.height / step).toInt()) {
            val cx = x * step; val cy = y * step
            drawPath(Path().apply { moveTo(cx,cy-step/2); lineTo(cx+step/2,cy); lineTo(cx,cy+step/2); lineTo(cx-step/2,cy); close() },faint,style = Stroke(1.dp.toPx()))
        }
        val w = size.width; val h = size.height
        val silhouette = Color(0xFF598174).copy(alpha = .28f)
        drawRect(silhouette,Offset(w*.68f,h*.76f),Size(w*.27f,h*.24f))
        drawArc(silhouette,180f,180f,true,Offset(w*.68f,h*.56f),Size(w*.27f,h*.40f))
        drawRect(silhouette,Offset(w*.91f,h*.38f),Size(w*.025f,h*.62f))
        drawPath(Path().apply { moveTo(w*.89f,h*.38f); lineTo(w*.922f,h*.27f); lineTo(w*.95f,h*.38f); close() },silhouette)
    }
}
