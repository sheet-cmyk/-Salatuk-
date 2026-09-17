package app.noor.prayer.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import app.noor.prayer.core.designsystem.NoorIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.noor.prayer.R
import app.noor.prayer.core.common.Capabilities
import app.noor.prayer.core.notifications.labelResource
import app.noor.prayer.domain.*
import app.noor.prayer.feature.onboarding.LocationForm

@Composable
fun SettingsScreen(s: PrayerSettings,caps: Capabilities,busy: Boolean,update: ((PrayerSettings) -> PrayerSettings) -> Unit,locate: () -> Unit,exact: () -> Unit,notifications: () -> Unit,appSettings: () -> Unit,preview: () -> Unit,stop: () -> Unit,import: (Voice) -> Unit) {
    var locationDialog by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    if(locationDialog) Dialog(onDismissRequest = { locationDialog = false },properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column { TextButton(onClick = { locationDialog = false }) { Text(stringResource(R.string.close)) }; Box(Modifier.weight(1f)) { LocationForm(s.location,busy,false,locate) { value -> update { it.copy(location = value) }; locationDialog = false } } }
        }
    }
    if(help) AlertDialog(onDismissRequest = { help = false },title = { Text(stringResource(R.string.troubleshooting)) },text = { Text(stringResource(R.string.troubleshooting_body)) },confirmButton = { TextButton(onClick = appSettings) { Text(stringResource(R.string.app_settings)) } },dismissButton = { TextButton(onClick = { help = false }) { Text(stringResource(R.string.close)) } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.settings),style = MaterialTheme.typography.headlineLarge)
        if(!caps.exact) SettingsGroup(R.string.exact_title) {
            Text(stringResource(R.string.exact_body),style = MaterialTheme.typography.bodyMedium)
            Button(onClick = exact) { Text(stringResource(R.string.allow_exact)) }
        } else Text(stringResource(R.string.ready),style = MaterialTheme.typography.labelLarge,color = MaterialTheme.colorScheme.primary)
        if(!caps.notifications) SettingsGroup(R.string.notifications) {
            Text(stringResource(R.string.notifications_body),style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = notifications) { Text(stringResource(R.string.allow_notifications)) }
            TextButton(onClick = appSettings) { Text(stringResource(R.string.app_settings)) }
        }
        SettingsGroup(R.string.location) {
            Text(s.location?.city?.ifBlank { stringResource(R.string.current_location) }.orEmpty(),style = MaterialTheme.typography.titleMedium)
            Text(listOfNotNull(s.location?.country?.takeIf { it.isNotBlank() },s.location?.let { "${it.latitude}, ${it.longitude}" },s.location?.zone()?.id).joinToString("\n"),style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = locate,enabled = !busy) { Text(stringResource(R.string.automatic_location)) }
            TextButton(onClick = { locationDialog = true }) { Text(stringResource(R.string.manual_location)) }
        }
        SettingsGroup(R.string.adhan) {
            Toggle(stringResource(R.string.enable_adhan),s.adhan.enabled) { value -> update { it.copy(adhan = it.adhan.copy(enabled = value)) } }
            PrayerName.entries.filter { it.obligatory }.forEach { prayer -> Toggle(stringResource(prayer.labelResource()),prayer in s.adhan.prayers) { value -> update { it.copy(adhan = it.adhan.copy(prayers = if(value) it.adhan.prayers + prayer else it.adhan.prayers - prayer)) } } }
            Choice(stringResource(R.string.voice),s.adhan.voice,Voice.entries.filter { it == Voice.STANDARD || it in s.adhan.customAudio },{ voiceLabel(it) }) { value -> update { it.copy(adhan = it.adhan.copy(voice = value)) } }
            Text(stringResource(R.string.audio_note),style = MaterialTheme.typography.bodySmall)
            Voice.entries.filter { it != Voice.STANDARD }.forEach { voice -> OutlinedButton(onClick = { import(voice) }) { Icon(NoorIcons.AudioFile,null); Spacer(Modifier.width(8.dp)); Text("${stringResource(R.string.import_audio)} · ${voiceLabel(voice)}") } }
            Text(stringResource(R.string.volume_note),style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = preview,modifier = Modifier.weight(1f)) { Text(stringResource(R.string.preview)) }
                OutlinedButton(onClick = stop,modifier = Modifier.weight(1f)) { Text(stringResource(R.string.stop_adhan)) }
            }
        }
        SettingsGroup(R.string.notifications) {
            Toggle(stringResource(R.string.prayer_notifications),s.notifications) { value -> update { it.copy(notifications = value) } }
            Choice(stringResource(R.string.reminders),s.reminderMinutes,listOf(0,5,10,15,30),{ if(it == 0) stringResource(R.string.disabled) else stringResource(R.string.minutes_before,it) }) { value -> update { it.copy(reminderMinutes = value) } }
            Text(stringResource(R.string.reminder_note),style = MaterialTheme.typography.bodySmall)
        }
        SettingsGroup(R.string.appearance) {
            Choice(stringResource(R.string.appearance),s.appearance,Appearance.entries,{ stringResource(when(it) { Appearance.SYSTEM -> R.string.system; Appearance.LIGHT -> R.string.light; Appearance.DARK -> R.string.dark }) }) { value -> update { it.copy(appearance = value) } }
            Choice(stringResource(R.string.language),s.language,listOf("","en","ar","de"),{ when(it) { "en" -> "English"; "ar" -> "العربية"; "de" -> "Deutsch"; else -> stringResource(R.string.system) } }) { value -> update { it.copy(language = value) } }
            Stepper(stringResource(R.string.hijri_adjustment),s.hijriOffset,-2..2) { value -> update { it.copy(hijriOffset = value) } }
            Text(stringResource(R.string.hijri_note),style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { help = true }) { Icon(NoorIcons.HelpOutline,null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.troubleshooting)) }
        SettingsGroup(R.string.about) { Text(stringResource(R.string.about_body),style = MaterialTheme.typography.bodySmall); Text(stringResource(R.string.city_credit),style = MaterialTheme.typography.bodySmall); Text(stringResource(R.string.privacy),style = MaterialTheme.typography.bodySmall) }
    }
}
@Composable
private fun SettingsGroup(title: Int,content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(stringResource(title),style = MaterialTheme.typography.titleLarge,color = MaterialTheme.colorScheme.primary); content() }
    }
}
@Composable
private fun Toggle(label: String,checked: Boolean,change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp),verticalAlignment = Alignment.CenterVertically) { Text(label,Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); Switch(checked,change) }
}
@Composable
private fun Stepper(label: String,value: Int,range: IntRange,change: (Int) -> Unit) {
    Column { Text(label,style = MaterialTheme.typography.bodyMedium); Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { change(value-1) },enabled = value > range.first) { Icon(NoorIcons.Remove,stringResource(R.string.decrease)) }
        Text(if(value > 0) "+$value" else "$value",modifier = Modifier.widthIn(min = 32.dp),style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { change(value+1) },enabled = value < range.last) { Icon(NoorIcons.Add,stringResource(R.string.increase)) }
    } }
}
@Composable
private fun <T> Choice(title: String,current: T,values: List<T>,label: @Composable (T) -> String,change: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 8.dp)) { Text(title,style = MaterialTheme.typography.labelMedium,color = MaterialTheme.colorScheme.onSurfaceVariant); Row(verticalAlignment = Alignment.CenterVertically) { Text(label(current),Modifier.weight(1f),style = MaterialTheme.typography.bodyLarge); Icon(NoorIcons.ExpandMore,null) } }
    if(open) AlertDialog(onDismissRequest = { open = false },title = { Text(title) },text = {
        Column(Modifier.verticalScroll(rememberScrollState())) { values.forEach { value -> Row(Modifier.fillMaxWidth().clickable { change(value); open = false }.padding(vertical = 8.dp),verticalAlignment = Alignment.CenterVertically) { RadioButton(value == current,{ change(value); open = false }); Text(label(value)) } } }
    },confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } })
}
@Composable
private fun voiceLabel(voice: Voice) = stringResource(when(voice) { Voice.STANDARD -> R.string.voice_standard; Voice.MAKKAH -> R.string.voice_makkah; Voice.MADINAH -> R.string.voice_madinah })
