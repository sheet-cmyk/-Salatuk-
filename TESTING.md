# Testing Noor

Use a disposable emulator for destructive test state changes. All ADB examples below target `emulator-5554`; substitute your test device serial explicitly. Do not use your personal phone for force-idle/reboot testing without choosing to do so.

## Build and local tests

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease
```

Unit tests cover all six calculated times; every method; Asr madhhab; offsets; invalid/polar coordinates; next-prayer and exactly-at-boundary behavior; tomorrow Fajr; timezone/DST conversion; Tehran Maghrib; Qibla bearing; alarm identities; per-prayer/master switches; reminders; revision invalidation; and daily recurrence.

Install and open:

```powershell
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n app.noor.prayer/.MainActivity
```

Complete manual location (e.g. Berlin, Germany, 52.52, 13.405, Europe/Berlin). Also test fresh onboarding with approximate location, precise location, denial and location services disabled. Allow exact alarms through the app’s explanation/system page. Allow notifications for the full notification test. The debug receiver can initialize a Berlin location for automated runs:

```powershell
adb -s emulator-5554 shell appops set app.noor.prayer SCHEDULE_EXACT_ALARM allow
adb -s emulator-5554 shell pm grant app.noor.prayer android.permission.POST_NOTIFICATIONS
adb -s emulator-5554 shell am broadcast -n app.noor.prayer/.DebugAlarmReceiver --ez setup true --es prayer FAJR --ei seconds 30
```

The **debug APK only** contains this exported receiver. It schedules a real exact alarm 10–120 seconds ahead through AlarmManager; the production receiver and foreground playback service then handle it. The release manifest has no test receiver. This test validates delivery plumbing, not astronomical timing: separately compare the displayed daily schedule with the calculated epoch alarms. `--es prayer` accepts FAJR, DHUHR, ASR, MAGHRIB and ISHA. Do not use SUNRISE; the production receiver rejects it.

## Background, locked screen and process death

Schedule a test 30 seconds ahead, press Home and lock:

```powershell
adb -s emulator-5554 shell input keyevent KEYCODE_HOME
adb -s emulator-5554 shell input keyevent KEYCODE_SLEEP
adb -s emulator-5554 shell am kill app.noor.prayer
```

At delivery, verify the Adhan service is foreground and the media session is playing:

```powershell
adb -s emulator-5554 shell dumpsys activity services app.noor.prayer
adb -s emulator-5554 shell dumpsys media_session
adb -s emulator-5554 shell dumpsys notification
adb -s emulator-5554 logcat -d -s AndroidRuntime NoorTest ExoPlayerImpl
```

Listen to the actual recitation on a physical test device. Check alarm-stream volume without changing it in the app. Stop from the notification; verify the service, focus and ongoing notification disappear. Preview, stop preview, unplug headphones, switch Bluetooth routes and interrupt with a phone call. After natural completion (~154 seconds), verify resources are released. Do not confuse `am kill` with `am force-stop`: force-stop intentionally removes alarm delivery eligibility until the user reopens Noor.

## Doze

Use an unplugged emulator battery state and schedule **before** forcing idle:

```powershell
adb -s emulator-5554 shell am broadcast -n app.noor.prayer/.DebugAlarmReceiver --es prayer ASR --ei seconds 45
adb -s emulator-5554 shell input keyevent KEYCODE_HOME
adb -s emulator-5554 shell dumpsys battery unplug
adb -s emulator-5554 shell input keyevent KEYCODE_SLEEP
adb -s emulator-5554 shell dumpsys deviceidle force-idle
adb -s emulator-5554 shell dumpsys deviceidle
```

Wait past the alarm and inspect the service/media session. The device should still report idle. Exact-alarm exemptions permit the playback foreground service. Android may impose an idle delivery quota: allow at least nine minutes between repeated idle exact-alarm trials, or exit idle and start a new controlled trial. Approximate reminders are expected to be deferred in Doze.

Always restore the emulator:

```powershell
adb -s emulator-5554 shell dumpsys deviceidle unforce
adb -s emulator-5554 shell dumpsys battery reset
adb -s emulator-5554 shell input keyevent KEYCODE_WAKEUP
adb -s emulator-5554 shell wm dismiss-keyguard
```

## Reboot, clock and timezone

1. Save a manual location and enabled prayers. Inspect `adb -s emulator-5554 shell dumpsys alarm` for `app.noor` actions and unique per-prayer IDs.
2. `adb -s emulator-5554 reboot`, wait for boot, unlock once, then inspect the alarms without launching Noor. Five upcoming prayer events should be restored with exact permission intact. The boot receiver must not start audio.
3. Change the time zone in Android Settings. With location’s zone field blank, the day/display zone follows the new device zone. An explicitly chosen manual IANA zone remains attached to that location. In both cases the receiver recalculates absolute schedules.
4. Change the clock, then restore automatic time. Ensure expired alarms are not replayed and tomorrow Fajr remains present.
5. Change calculation method, location, madhhab and offsets repeatedly. Inspect alarms: one occurrence per enabled event identity, not growing duplicates. Old-revision events must be rejected.

## Denied/revoked permission

```powershell
adb -s emulator-5554 shell pm revoke app.noor.prayer android.permission.POST_NOTIFICATIONS
adb -s emulator-5554 shell appops set app.noor.prayer SCHEDULE_EXACT_ALARM deny
adb -s emulator-5554 shell am start -n app.noor.prayer/.MainActivity
```

The app must remain usable with manual location. Exact alarm access should show as unavailable; there must be no fallback timer pretending to deliver precise Adhan. Grant again through the system page and verify schedules return. With notifications denied but exact alarms allowed, verify no crash and legal foreground media handling; Android controls which notification surfaces remain visible. Deny location twice, continue with manual coordinates, and verify offline calculation.

## UI/device matrix

- API 26, 31/32, 33, 34, 35, 36 and the current stable release; at least Pixel and one restrictive OEM.
- English/Arabic/German; Arabic RTL; dark/light/system; font scale 1.0 and 2.0; portrait/landscape; TalkBack.
- Navigation, all settings persistence, invalid numeric input, every calculation method, month navigation, Hijri adjustments and next-day rollover.
- Add and resize the home-screen widget. Check next prayer/location on settings changes, alarm delivery and after reboot. It intentionally has no seconds countdown.
- Qibla on a real calibrated rotation-vector sensor, near metal, missing sensor, and rotated display. The emulator can validate fallback/rendering but cannot certify physical compass accuracy.
- Airplane mode after saving coordinates; audio import validation; missing/corrupted imported audio; no remote traffic or backend dependency.

Record actual observations in VALIDATION.md. Do not mark an unperformed physical or OEM test as passed merely because its implementation compiles.
