package app.noor.prayer.feature.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import app.noor.prayer.core.designsystem.NoorIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.noor.prayer.R
import app.noor.prayer.core.notifications.labelResource
import app.noor.prayer.domain.*
import app.noor.prayer.feature.PrayerViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(vm: PrayerViewModel,settings: PrayerSettings) {
    var month by remember { mutableStateOf(YearMonth.now(settings.location!!.zone())) }
    var rows by remember { mutableStateOf<List<DailyPrayerTimes>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(month,settings.revision) {
        loading = true; failed = false
        runCatching { vm.calendar(month,settings) }.onSuccess { rows = it }.onFailure { failed = true; rows = emptyList() }
        loading = false
    }
    val clock = remember { DateTimeFormatter.ofPattern("HH:mm",Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.calendar),style = MaterialTheme.typography.headlineLarge)
        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(NoorIcons.KeyboardArrowLeft,stringResource(R.string.previous_month)) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.getDefault())),style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(NoorIcons.KeyboardArrowRight,stringResource(R.string.next_month)) }
        }
        TextButton(onClick = { month = YearMonth.now(settings.location!!.zone()) }) { Text(stringResource(R.string.today)) }
        if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(failed) Text(stringResource(R.string.calculation_failed),color = MaterialTheme.colorScheme.error)
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            LazyColumn(Modifier.width(640.dp)) {
                item {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) { Row(Modifier.padding(vertical = 16.dp)) {
                        Text(stringResource(R.string.date),Modifier.width(88.dp).padding(start = 12.dp),style = MaterialTheme.typography.labelMedium)
                        PrayerName.entries.forEach { Text(stringResource(it.labelResource()),Modifier.width(92.dp),style = MaterialTheme.typography.labelMedium) }
                    } }
                }
                items(rows,key = { it.date.toString() }) { day ->
                    Row(Modifier.padding(vertical = 16.dp)) {
                        Text(day.date.format(DateTimeFormatter.ofPattern("dd EEE",Locale.getDefault())),Modifier.width(88.dp).padding(start = 12.dp),style = MaterialTheme.typography.bodySmall)
                        PrayerName.entries.forEach { prayer -> Text(day.times.first { it.name == prayer }.instant.atZone(day.zone).format(clock),Modifier.width(92.dp),style = MaterialTheme.typography.bodyMedium) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
