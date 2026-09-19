package app.noor.prayer.feature.radio

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import app.noor.prayer.core.designsystem.NoorIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.noor.prayer.R
import app.noor.prayer.core.designsystem.Emerald
import app.noor.prayer.core.designsystem.Gold
import app.noor.prayer.core.media.RadioState
import app.noor.prayer.domain.RadioStations
import app.noor.prayer.feature.PrayerViewModel

@Composable
fun RadioScreen(vm: PrayerViewModel) {
    val radio by vm.radio.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(),contentPadding = PaddingValues(24.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(stringResource(R.string.radio),style = MaterialTheme.typography.headlineLarge) }
        if(radio.stationId != null) item { NowPlaying(radio,vm::previousStation,vm::toggleRadio,vm::nextStation,vm::stopRadio) }
        items(RadioStations,key = { it.id }) { station ->
            val active = station.id == radio.stationId
            Surface(shape = RoundedCornerShape(18.dp),color = if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,onClick = { vm.playStation(station.id) }) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp,vertical = 16.dp),verticalAlignment = Alignment.CenterVertically) {
                    Icon(NoorIcons.Radio,null,Modifier.size(22.dp),tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text(station.name,Modifier.weight(1f),style = MaterialTheme.typography.titleMedium)
                    if(active) EqualizerBars(playing = radio.playing,color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Text(stringResource(R.string.radio_credit),style = MaterialTheme.typography.labelSmall,color = MaterialTheme.colorScheme.onSurfaceVariant,modifier = Modifier.padding(top = 8.dp,bottom = 12.dp)) }
    }
}

@Composable
private fun NowPlaying(radio: RadioState,previous: () -> Unit,toggle: () -> Unit,next: () -> Unit,stop: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Emerald,contentColor = Color.White),shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EqualizerBars(playing = radio.playing,color = Gold,barWidth = 4.dp,height = 26.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(radio.stationName,style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if(radio.buffering) R.string.radio_buffering else if(radio.playing) R.string.radio_playing else R.string.radio_paused),style = MaterialTheme.typography.bodySmall,color = Gold)
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.Center,verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = previous,modifier = Modifier.size(48.dp)) { Icon(NoorIcons.SkipPrevious,stringResource(R.string.radio_previous),tint = Color.White) }
                Spacer(Modifier.width(20.dp))
                Surface(shape = CircleShape,color = Gold,modifier = Modifier.size(60.dp).clip(CircleShape)) {
                    IconButton(onClick = toggle,modifier = Modifier.fillMaxSize()) { Icon(if(radio.playing) NoorIcons.Pause else NoorIcons.PlayArrow,stringResource(R.string.radio_toggle),tint = Emerald,modifier = Modifier.size(28.dp)) }
                }
                Spacer(Modifier.width(20.dp))
                IconButton(onClick = next,modifier = Modifier.size(48.dp)) { Icon(NoorIcons.SkipNext,stringResource(R.string.radio_next),tint = Color.White) }
            }
            TextButton(onClick = stop,modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.radio_stop),color = Gold) }
        }
    }
}

@Composable
private fun EqualizerBars(playing: Boolean,color: Color,barWidth: androidx.compose.ui.unit.Dp = 3.dp,height: androidx.compose.ui.unit.Dp = 18.dp) {
    val transition = rememberInfiniteTransition(label = "eq")
    @Composable fun bar(delay: Int) = transition.animateFloat(
        initialValue = 0.25f,targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420,delayMillis = delay,easing = FastOutSlowInEasing),RepeatMode.Reverse),label = "bar"
    )
    val b1 by bar(0); val b2 by bar(140); val b3 by bar(260)
    Row(Modifier.height(height),verticalAlignment = Alignment.Bottom,horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(b1,b2,b3).forEach { scale ->
            Box(Modifier.width(barWidth).fillMaxHeight(if(playing) scale else 0.25f).background(color,RoundedCornerShape(2.dp)))
        }
    }
}
