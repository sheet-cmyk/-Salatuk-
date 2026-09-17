# Validation — 17 September 2026

## Verified builds

- `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease`, and `bundleRelease` succeeded.
- Final application build, including the emerald navigation color correction: **BUILD SUCCESSFUL**.
- `lintDebug`: **0 errors, 30 warnings**. Remaining warnings include available dependency updates, target/minimum API notices, resource cleanup, and pluralization suggestions. They were not hidden with a lint baseline or disabled error checking.
- Release APK/AAB are unsigned. Debug APK uses the development signing key and is installable for testing.
- The merged release manifest contains no Internet permission and no `DebugAlarmReceiver`. The debug-only receiver requires the shell/system DUMP permission.

## Automated tests

**22 JVM tests passed**, zero failures: 13 prayer-engine tests and 9 alarm-planner tests. These cover calculation methods, ordered daily times, sunrise exclusion, next/tomorrow prayer, offsets, Hanafi Asr, DST/timezones, polar failure, Qibla bearing, stable alarm identities, revisions, reminders and enable/disable settings.

**6 instrumented tests passed together**, final run: `OK (6 tests)`, 63.46 seconds, on `emulator-5554` (Android API 37, x86_64, 16 KB image):

1. Clearing location/imported-audio preferences removes stale values.
2. Settings survive Activity recreation.
3. Audio preview produces a foreground notification; its Stop action removes the playback notification.
4. The manual location form saves coordinates.
5. Navigation reaches calendar, Qibla, settings and home.
6. Atomic alarm consumption rejects duplicates and stale revisions.

An earlier run alongside a large release build had two lifecycle/UI synchronization failures. Both tests passed in isolation, then all six passed together without additional application changes. A previous Espresso version failed on a removed reflective Android input API; the project now explicitly uses Espresso 3.7.0.

## Observed device behavior

- Installed and launched the debug APK successfully.
- Scheduled a debug Fajr event through real AlarmManager, sent the app to the background and locked the screen. The production receiver started `AdhanPlaybackService`; Android reported `isForeground=true`, notification ID 50 and MediaSession `PLAYING(3)`. The start exemption was `ALARM_MANAGER_WHILE_IDLE`.
- Scheduled an Asr test event and forced deep Doze. While `mState=IDLE`, the service and session were playing; logs showed dispatch at the scheduled time. Battery simulation and forced idle were subsequently reset.
- Rebooted the emulator and did not launch Noor. Android initially queued the boot broadcast; after it delivered, all five upcoming prayer alarms were restored, without boot-time playback.
- Confirmed five distinct pending prayer alarms and no sunrise alarm. Alarm-manager historical logs are not additional pending alarms.
- Denied exact-alarm access and notification permission together. The app stayed alive, displayed the exact-alarm explanation, and had no pending prayer alarms. Restored permissions after the test.
- Inspected English onboarding and the Arabic home screen: RTL layout, localized dates, countdown and prayer rows rendered correctly.

Debug test events deliberately accelerate the schedule. They validate the production receiver/service plumbing, not a live astronomical prayer boundary. The normal schedule is independently covered by calculation/planner tests.

## Remaining device/publication checks

- No physical phone was tested. Phone-call/Bluetooth interruptions, audible volume, OEM battery policies, long unattended operation, sensor calibration, and the API 26–36 device matrix remain field tests.
- Automated GPS success/denial flows, German visual QA, launcher widget placement, full large-font/accessibility checks, natural audio completion, and system timezone-change delivery were not all exercised end-to-end. Implementation is present; see TESTING.md for follow-up cases.
- Only the licensed standard Adhan is bundled. Makkah and Madinah are local import slots; separately authorized recordings are needed to pre-bundle all three.
- Supply a release signing key, store listing/privacy policy, final assets and physical-device QA before publication. This delivery is a working development build, not a claim of completed store certification.

## Toolchain corrections

Hilt's annotation processors now use Kotlin metadata 2.4.20; R8 is pinned to 9.1.43; JVM tests use Java 21 for Adhan 0.0.7's JVM bytecode. App compilation remains Java 17. Windows SDK property escaping and Espresso 3.7.0 compatibility were fixed. Dependency compatibility references are linked in README.md.
