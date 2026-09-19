package app.noor.prayer.feature

import androidx.compose.foundation.layout.*
import app.noor.prayer.core.designsystem.NoorIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import app.noor.prayer.R
import app.noor.prayer.domain.Voice
import app.noor.prayer.core.designsystem.NoorTheme
import app.noor.prayer.feature.home.HomeScreen
import app.noor.prayer.feature.settings.SettingsScreen
import app.noor.prayer.feature.onboarding.LocationForm
import app.noor.prayer.feature.calendar.CalendarScreen
import app.noor.prayer.feature.radio.RadioScreen

private data class Destination(val route: String,val title: Int,val icon: ImageVector)
@Composable
fun NoorApp(vm: PrayerViewModel, locate: () -> Unit, exact: () -> Unit, notifications: () -> Unit, appSettings: () -> Unit, battery: () -> Unit, volume: () -> Unit, import: (Voice) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val capabilities by vm.capabilities.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val snackbar = remember { SnackbarHostState() }
    val errorText = message?.let { stringResource(it) }
    LaunchedEffect(errorText) { errorText?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    val destinations = listOf(Destination("home",R.string.home,NoorIcons.Home),Destination("calendar",R.string.calendar,NoorIcons.CalendarMonth),Destination("radio",R.string.radio,NoorIcons.Radio),Destination("settings",R.string.settings,NoorIcons.Tune))
    NoorTheme(state.settings.appearance) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) },bottomBar = {
            if(state.loaded && state.settings.location != null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface,tonalElevation = 0.dp) {
                destinations.forEach { d -> NavigationBarItem(modifier = Modifier.testTag("nav_${d.route}"),selected = entry?.destination?.route == d.route,onClick = { nav.navigate(d.route) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },icon = { Icon(d.icon,null) },label = { Text(stringResource(d.title)) }) }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if(!state.loaded) Box(Modifier.fillMaxSize(),contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
                else if(state.settings.location == null) LocationForm(null,busy,true,locate) { value -> vm.update { it.copy(location = value) } }
                else NavHost(nav,startDestination = "home") {
                    composable("home") { HomeScreen(state,capabilities.exact) { nav.navigate("settings") } }
                    composable("settings") { SettingsScreen(state.settings,capabilities,busy,vm::update,locate,exact,notifications,appSettings,battery,volume,vm::preview,vm::stop,import) }
                    composable("calendar") { CalendarScreen(vm,state.settings) }
                    composable("radio") { RadioScreen(vm) }
                }
            }
        }
    }
}
