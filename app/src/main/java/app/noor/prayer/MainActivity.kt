package app.noor.prayer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.noor.prayer.core.alarm.MaintenanceWorker
import app.noor.prayer.core.notifications.PrayerNotifications
import app.noor.prayer.data.SettingsRepository
import app.noor.prayer.domain.Voice
import app.noor.prayer.feature.NoorApp
import app.noor.prayer.feature.PrayerViewModel
import app.noor.prayer.widget.PrayerWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val vm: PrayerViewModel by viewModels()
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var notifications: PrayerNotifications
    private var importVoice = Voice.MAKKAH
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if(granted.values.any { it }) vm.locate() else vm.message.value = R.string.location_failed
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.resume() }
    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importAudio(it,importVoice) } }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        importVoice = savedInstanceState?.getString("importVoice")?.let { runCatching { Voice.valueOf(it) }.getOrNull() } ?: Voice.MAKKAH
        enableEdgeToEdge()
        notifications.channels(); MaintenanceWorker.enqueue(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.settings.collect { s ->
                    val locales = LocaleListCompat.forLanguageTags(s.language)
                    if(AppCompatDelegate.getApplicationLocales() != locales) AppCompatDelegate.setApplicationLocales(locales)
                    PrayerWidget.refresh(this@MainActivity)
                }
            }
        }
        setContent { NoorApp(vm,
            locate = { locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)) },
            exact = { if(Build.VERSION.SDK_INT >= 31) open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,"package:$packageName".toUri())) },
            notifications = { if(Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else appSettings() },
            appSettings = ::appSettings,
            import = { voice -> importVoice = voice; audioPicker.launch(arrayOf("audio/*")) }) }
    }
    private fun open(intent: Intent) { runCatching { startActivity(intent) }.onFailure { vm.message.value = R.string.schedule_failed } }
    private fun appSettings() = open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:$packageName".toUri()))
    override fun onResume() { super.onResume(); vm.resume() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("importVoice",importVoice.name); super.onSaveInstanceState(outState) }
}
