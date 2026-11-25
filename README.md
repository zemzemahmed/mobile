# MediReminder

A simple Android app that reminds you to take medicines. It is a school project, written in plain Java with beginner-friendly, commented code.

Repository: https://github.com/zemzemahmed/mobile.git

The launcher icon is generated from `mobile.jpg` (on the Desktop) in all Android densities.

## What it does

- Add / edit / pause / delete medicines.
- Each medicine can have several alarms per day, with its own pill count (e.g. `0.5`).
- Choose which days of the week each alarm applies to, plus an optional start/end date.
- At the alarm time a **full-screen alarm** opens with the dose name, a sound, and a big **Taken** button.
- You have **5 seconds** to tap **Taken**. If you do nothing, the dose is logged as **MISSED** immediately, the alarm closes, and an SMS is sent to caregivers.
- There is **no Snooze** button.
- Home tab: today's doses, next dose highlighted, and a low-stock warning.
- History tab: weekly adherence percentage, a small bar chart, and the dose log.
- Settings tab: alarm sound, caregivers, dark mode, large text, and rebuild alarms.
- Alarms are restored after a phone reboot.

## Technology

- Java 17, Android SDK 34 (minSdk 24).
- Room for local storage (`medireminder.db`), LiveData for UI updates.
- A single-thread `ExecutorService` (`AppExecutor`) for all database work — no coroutines.
- `AlarmManager` + `BroadcastReceiver` + a high-priority notification with a **full-screen intent**; no foreground service (skipped on purpose).

## Build

### Option A — Android Studio

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Click **Run** and pick your phone.

### Option B — command line (already set up on this machine)

```powershell
cd "C:\Users\zemze\Desktop\projet mobile"
.\gradlew.bat assembleDebug
```

`gradle.properties` already points to the installed JDK 17:

```
org.gradle.java.home=C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.21.10002-hotspot
```

The APK is created at:

```
app\build\outputs\apk\debug\app-debug.apk
```

## Install on a physical phone over USB

1. On the phone: **Developer options → USB debugging** on, phone unlocked.
2. Accept the **Allow USB debugging** dialog (tick *Always allow from this computer*).
3. Check the connection:

```powershell
& "C:\Users\zemze\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
```

You should see the device as `device`, not `unauthorized`.

4. Install / reinstall:

```powershell
& "C:\Users\zemze\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "C:\Users\zemze\Desktop\projet mobile\app\build\outputs\apk\debug\app-debug.apk"
```

## First run: permissions

The app asks for:

- **Notifications** (Android 13+) — needed to show the full-screen dose alarm.
- **SMS** — needed only to alert caregivers. SMS may be billed by your operator. If you deny it, the app still works and simply does not send SMS.
- **Exact alarms** (Android 12+) — tap **Open settings** and allow it; otherwise alarms may be late.
- Full-screen alarms use Android's `USE_FULL_SCREEN_INTENT` permission (granted to alarm apps).

After allowing exact alarms, come back to the app: alarms are rebuilt automatically. You can also use **Settings → Rebuild all alarms**.

## Quick test (about 3 minutes)

1. Open **Caregivers** (from the Medicines header or Settings) and add a phone number to receive the SMS.
2. Open the **Medicines** tab, tap **+**, and add a medicine:
   - Name: `Test`
   - Stock: `10`
   - Alarm time: 1–2 minutes ahead of the phone clock
   - Tick today's day
   - Save
3. Wait for the full-screen alarm. Check:
   - **Taken** → the dose appears as Taken on Home, stock drops, the missed timer is cancelled.
   - Or do nothing → after **5 seconds** the screen closes, the dose becomes Missed, and an SMS is sent to caregivers.
4. Open **History** to see the adherence percentage and the bar chart.
5. Use **Pause** on a medicine card: its alarms stop. **Resume** rebuilds them. **Delete** cancels alarms but keeps past history.
6. Reboot the phone: alarms are restored.

> Tip: set the phone clock/time zone correctly. Alarms are based on the device's local time.

## Project structure (simple MVVM)

```
app/src/main/java/com/example/medireminder/
├── data/
│   ├── entity/       Medicine, Schedule, DoseLog, Caregiver, MedicineWithSchedules
│   ├── dao/          Room queries
│   ├── database/     AppDatabase (singleton)
│   └── repository/   UI talks only to these classes
├── ui/
│   ├── activity/     MainActivity, AddEditMedicineActivity, CaregiversActivity, AlarmActivity
│   ├── fragment/     Home, History, Medicines, Settings
│   ├── adapter/      RecyclerView adapters
│   └── view/         AdherenceChartView (custom bar chart)
├── viewmodel/        MedicineViewModel
├── receiver/         AlarmReceiver, ActionReceiver, MissedReceiver, BootReceiver
├── util/             AlarmScheduler, NotificationHelper, SmsHelper,
│                     PermissionHelper, PreferencesHelper, TimeUtils, AppExecutor
└── MediReminderApp.java
```

Alarm request-code ranges (never reused):

- Reminder = `scheduleId`
- Missed check = `100000 + scheduleId`

## Known limitations / not implemented

These were optional extras in the specification and are **not** in this version:

- French/English language switch (v1 is English only).
- Text-to-speech reading of reminders.
- Home-screen widget.
- PDF export and Telegram notifications.
- Foreground service (skipped by decision).

Also note: SMS alerts use the phone's normal SIM card, so standard operator rates may apply. This project uses Git; `CONTEXT.md` is the handoff file.