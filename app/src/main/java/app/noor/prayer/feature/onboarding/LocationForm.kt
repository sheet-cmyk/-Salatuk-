package app.noor.prayer.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import app.noor.prayer.core.designsystem.NoorIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.noor.prayer.R
import app.noor.prayer.domain.LocationSettings
import java.time.ZoneId

@Composable
fun LocationForm(existing: LocationSettings?, busy: Boolean, welcome: Boolean, locate: () -> Unit, save: (LocationSettings) -> Unit) {
    var city by rememberSaveable { mutableStateOf(existing?.city.orEmpty()) }
    var country by rememberSaveable { mutableStateOf(existing?.country.orEmpty()) }
    var latitude by rememberSaveable { mutableStateOf(existing?.latitude?.toString().orEmpty()) }
    var longitude by rememberSaveable { mutableStateOf(existing?.longitude?.toString().orEmpty()) }
    var zone by rememberSaveable { mutableStateOf(existing?.timeZone.orEmpty()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if(welcome) stringResource(R.string.app_name) else stringResource(R.string.location),style = MaterialTheme.typography.labelLarge,color = MaterialTheme.colorScheme.primary)
        if(welcome) { Text(stringResource(R.string.welcome_title),style = MaterialTheme.typography.headlineLarge); Text(stringResource(R.string.welcome_body),style = MaterialTheme.typography.bodyLarge) }
        Text(stringResource(R.string.location_explanation),color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = locate,enabled = !busy,modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth = 2.dp) else Icon(NoorIcons.MyLocation,null)
            Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.automatic_location))
        }
        HorizontalDivider()
        Text(stringResource(R.string.manual_location),style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(city,{ city = it },label = { Text(stringResource(R.string.city)) },modifier = Modifier.fillMaxWidth(),singleLine = true)
        OutlinedTextField(country,{ country = it },label = { Text(stringResource(R.string.country)) },modifier = Modifier.fillMaxWidth(),singleLine = true)
        OutlinedTextField(latitude,{ latitude = it },label = { Text(stringResource(R.string.latitude)) },keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),modifier = Modifier.fillMaxWidth(),singleLine = true)
        OutlinedTextField(longitude,{ longitude = it },label = { Text(stringResource(R.string.longitude)) },keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),modifier = Modifier.fillMaxWidth(),singleLine = true)
        OutlinedTextField(zone,{ zone = it },label = { Text(stringResource(R.string.timezone)) },modifier = Modifier.fillMaxWidth(),singleLine = true)
        Text(stringResource(R.string.timezone_hint),style = MaterialTheme.typography.bodySmall)
        if(invalid) Text(stringResource(R.string.invalid_location),color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            val value = runCatching { if(zone.isNotBlank()) ZoneId.of(zone.trim()); LocationSettings(latitude.trim().toDouble(),longitude.trim().toDouble(),city.trim(),country.trim(),timeZone = zone.trim()) }.getOrNull()
            if(value == null) invalid = true else save(value)
        },modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.save)) }
        Text(stringResource(R.string.privacy),style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }
}
