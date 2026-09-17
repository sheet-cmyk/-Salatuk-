# Noor — prayer, in rhythm with your day

A native, offline-first Android prayer application built with Kotlin, Compose and Material 3. Emerald, ivory and gold; English, Arabic with RTL, and German. No accounts, analytics, Firebase, backend or app Internet permission.

## Build

Open this directory in Android Studio. Use JDK 21 (Android Studio's bundled JBR works), with a JDK 17 compilation toolchain also installed, Android SDK 36.1, and platform/build tools. Desktop tests use Java 21 because Adhan 0.0.7 publishes JVM 21 bytecode; app compilation stays at Java 17 and D8 converts dependencies for Android. Set `sdk.dir` in your local, untracked `local.properties` or set `ANDROID_HOME`. On Windows, escape the drive colon, for example `sdk.dir=C\:/Users/your-name/AppData/Local/Android/Sdk`.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleRelease :app:bundleRelease
```

On macOS/Linux, use `./gradlew`. Gradle 8.13 is pinned in the wrapper. AGP 8.13.2, Kotlin 2.4.20, Hilt 2.57.2 and KSP 2.3.12 are pinned; no preview dependencies. The project compiles against the newest complete stable SDK found on the development machine (36.1), targets API 36 and supports API 26+. The installed Android 17 platform was a preview, so it is not used for compilation.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Release APK and AAB are **unsigned**; use Android Studio Generate Signed Bundle with your private upload key before distribution. No signing credentials are stored here.

Toolchain compatibility is explicit: R8 9.1.43 understands Kotlin 2.4 metadata; Hilt 2.57.2 uses `kotlin-metadata-jvm:2.4.20` on its KSP/Java processor classpaths. Espresso 3.7.0 avoids the removed reflective InputManager API on the API 37 test emulator. See [Android Kotlin compatibility](https://developer.android.com/build/kotlin-support), [Hilt processor configuration](https://github.com/google/dagger/blob/dagger-2.57.2/java/dagger/hilt/android/plugin/main/src/main/kotlin/dagger/hilt/android/plugin/HiltGradlePlugin.kt), and [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0).

## Features

- Local Adhan Kotlin 0.0.7 astronomy for Fajr, sunrise, Dhuhr, Asr, Maghrib and Isha.
- Twelve calculation methods, Hanafi/standard Asr, independent −60 to +60 minute offsets; sunrise never receives Adhan.
- Next obligatory prayer and a lifecycle-aware second-by-second countdown, including tomorrow’s Fajr.
- Exact prayer alarms, separate optional approximate reminders, prayer notifications and foreground Adhan playback.
- Full bundled CC0 standard Adhan, preview/stop, per-prayer audio switches, and local recording import for Makkah/Madinah slots.
- One-shot fused location, optional approximate access, offline nearest-city labels, manual coordinates and IANA time zone.
- Gregorian/Hijri dates and −2 to +2 day Hijri adjustment.
- Qibla bearing and lifecycle-bound rotation-vector compass with true-north declination correction, accuracy guidance and sensor fallback.
- Local monthly calendar, light/dark/system appearance and Android home-screen widget.
- Adaptive icon with monochrome variant; official SplashScreen API without an artificial delay.

### Recording availability

A real standard recitation by Adam-synagda is bundled, not a synthetic tone. Independently licensed recordings from Makkah and Madinah were not supplied. Those two slots are enabled after the user imports an audio file they have permission to use. The app never labels the standard recording as one of those voices. Imported audio is validated (1 second–15 minutes, at most 25 MB) and copied into private app storage. This is a known difference from the requested three pre-bundled voices, and must be resolved with approved recordings if all three must ship out of the box. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Architecture

One Gradle app module with clear feature and platform boundaries, rather than unnecessary build modules:

- `domain/`: typed settings, prayer instants, calculation wrapper and pure alarm planner.
- `data/`: Preferences DataStore, corruption recovery, atomic event consumption and serialized schedule changes.
- `core/alarm/`: AlarmManager adapter and non-exact WorkManager maintenance.
- `core/location/`: FusedLocationProviderClient, bundled city lookup and lifecycle-bound compass flow.
- `core/media/`, `service/`: playback controller, validated audio import, Media3 ExoPlayer/MediaSession foreground service.
- `core/notifications/`: channels and localized prayer/playback notifications.
- `feature/`: MVVM state, onboarding, home, settings, calendar and Qibla.
- `receiver/`: alarm delivery and reboot/time/time-zone/package/permission changes.
- `widget/`: lightweight native RemoteViews, refreshed at alarm delivery, setting changes and OS 30-minute widget updates. No seconds timer in the background.
- `di/`: Hilt process entry point for WorkManager and widgets.

Room is unnecessary because the daily and monthly results are cheap deterministic calculations. The UI uses repositories/ViewModel and does not call DataStore, AlarmManager, location services or ExoPlayer directly.

## Prayer calculations and time zones

The Adhan library calculates astronomical UTC instants from coordinates and a local calendar date. Formatting converts those instants through the selected IANA zone; device zone is the default. Alarms use epoch milliseconds, never formatted clock strings. The wrapper applies user offsets to obligatory prayers and explicitly rejects the library’s out-of-range polar sentinels rather than scheduling an epoch alarm. Twilight-angle high-latitude rules are used where a solar solution exists. In polar day/night, the app explains that a local mosque timetable is needed; it does not invent religious times.

Tehran parameters are 17.7° Fajr, 14° Isha and 4.5° Maghrib. Turkey is the library’s Diyanet approximation, not an official published timetable. Umm al-Qura users should apply the appropriate Isha adjustment during Ramadan in consultation with their local timetable. Method and Hijri calendar choices can differ from local mosque practice.

GPS is fetched on user request and when the app becomes visible in automatic mode (at most once per six hours in the same process). It is never continuously tracked or requested from a boot/background receiver. Therefore travel while the app remains unopened requires reopening it to refresh coordinates. After obtaining location, calculation and playback work offline. City labels are the nearest bundled city within 50 km; they are approximate, not administrative-border geocoding. Outside that range, coordinates still work and labels can be entered manually.

## Exact alarms and process death

`PrayerAlarmScheduler` calls `setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` for obligatory prayers after checking `canScheduleExactAlarms()` on API 31+. A visible explanation links to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Missing or revoked access cancels schedules safely; there is no misleading exactness claim or background service workaround.

One immutable PendingIntent per prayer/type has a stable request code. A settings change writes a fresh revision, cancels existing alarms and rebuilds the schedule under a mutex. Each pending intent carries its scheduled instant and revision. The receiver checks the current revision and expected calculated event, rejects stale/late events, and atomically consumes the event in DataStore before dispatch. A duplicate broadcast cannot replay that event. After delivery, the next occurrence of each prayer is scheduled. Each of the other four prayers already has its own alarm.

Reminders use `setWindow`, not idle exact alarms: a five-minute reminder must not consume the system’s idle exact-alarm quota immediately before Adhan. Reminders can be delayed or suppressed in Doze. WorkManager only repairs schedules every 12 hours; it is never the prayer clock.

Boot (after unlock), package replacement, time and timezone changes and exact-alarm permission grants trigger recalculation from persisted preferences. Revocation can kill the app and remove alarms; resume checks permission again. There is no direct-boot playback. Force-stop, powered-off devices and some OEM restrictions cannot be overridden. See [TESTING.md](TESTING.md).

## Background audio

A valid exact-alarm receiver starts a short-lived media-playback foreground service, which is an Android-supported exemption from the general background-start restriction. Boot and maintenance never start audio. The service promotes itself to foreground before requesting audio focus (important on Android 15+), plays local audio through Media3 ExoPlayer and exposes a MediaSession/media notification with Stop.

The stream uses `USAGE_ALARM` / speech and transient audio focus. ExoPlayer is not asked to manage focus automatically because alarm usage requires explicit focus handling. Focus loss and unplug/noisy events stop playback. The app does not change system volume, acquire DND access, or ignore mute policies. A bounded 15-second dispatch wake lock bridges receiver processing; ExoPlayer holds a local wake lock only during audio. Completion, Stop and errors release player, session and focus and remove the foreground notification. The service is not sticky and does not run between prayers.

## Permissions and privacy

| Permission | Purpose |
|---|---|
| Coarse/fine location | User-requested local prayer and Qibla coordinates; either precision works |
| POST_NOTIFICATIONS | Prayer/reminder notifications on Android 13+ |
| SCHEDULE_EXACT_ALARM | User-granted exact prayer alarms |
| RECEIVE_BOOT_COMPLETED | Restore schedules after restart and unlock |
| FOREGROUND_SERVICE / MEDIA_PLAYBACK | Temporary background Adhan audio |
| WAKE_LOCK | Bounded alarm dispatch and active local audio |

No `USE_EXACT_ALARM`, background location, broad storage access, battery-exemption request, or Internet permission. Location is not uploaded. GeoNames labels are bundled; no online reverse geocoder is used. Backup is disabled to keep settings/location and audio on this device. Android location providers may use their own system services under the device’s privacy settings.

## Validation and publication

See [TESTING.md](TESTING.md) for reproducible tests and [VALIDATION.md](VALIDATION.md) for results observed on this machine. Compilation alone is not proof of delivery on every manufacturer’s locked device. Before Google Play publication, supply final licensed voice assets, complete the physical-device/OS matrix, provide a public privacy policy and store declarations, and sign the release with your own key.
