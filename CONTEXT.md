# MediReminder — Context / Handoff File

> READ THIS FILE FIRST at the start of every working session.
> Full source of truth: `C:\Users\zemze\Downloads\MediReminder_Spec.pdf` (8 pages).

## 1. Project in one paragraph

Android app (Java, minSdk 24 / targetSdk 34, Gradle, AndroidX) that reminds a user to take medicines.
One medicine has several alarms per day; each alarm has its own pill count (0.5 supported).
If the user does not tap **Taken** within a configurable delay, the app logs MISSED and sends an SMS to
caregivers. Data in Room; settings in SharedPreferences; architecture = simple MVVM
(Entity → DAO → Repository → ViewModel → UI). School project: code must stay simple, commented, explainable.

## 2. Confirmed setup (from user, before coding)

- **No git during development.** `CONTEXT.md` was the handoff/memory file. **Changed at the end by user request:** the project was initialized as a Git repo and pushed to `https://github.com/zemzemahmed/mobile.git` (private repo, branch `main`).
- Android Studio stable `2026.2.2.10` installed via winget. SDK at `C:\Users\zemze\AppData\Local\Android\Sdk` (platform-tools, platforms;android-34, build-tools;34.0.0, USB driver).
- Testing on a **physical Android phone over USB**, Developer options + USB debugging already enabled.
- App name `MediReminder`, package `com.example.medireminder`.
- Visible text: **English only** in v1 (French/English remains an extra for later).
- **Skip** the optional foreground service.
- Working rhythm: **one small beginner-friendly part at a time, simple commented code, wait for user confirmation before the next part.**
- **Total planned parts: 13** (Part 1 done; 12 remaining). The 6 spec stages are split into small compiling steps; a part may be split further if it feels too big, but never merged silently.
- User is a beginner: prefer the simplest readable solution over clever/compact code.

## 3. Locked technical decisions (proposed — confirm before coding)

| Topic | Decision |
|---|---|
| Package | `com.example.medireminder` with subpackages `data.entity`, `data.dao`, `data.database`, `data.repository`, `ui.activity`, `ui.fragment`, `ui.adapter`, `viewmodel`, `receiver`, `util` |
| Room | Singleton `AppDatabase`; FK `Schedule.medicineId → Medicine.id` with `ON DELETE CASCADE`; `MedicineWithSchedules` via `@Relation` |
| Threading | `ExecutorService` for all DB work (no coroutines — spec requirement); `LiveData` for UI updates |
| Alarm request codes | Reminder = `scheduleId`; Missed-check = `100_000 + scheduleId`; Snooze = `200_000 + scheduleId`. Never reuse a code across intent types. (Originally snooze used `doseLogId`; changed to `scheduleId` so it can be scheduled without waiting for a log insert.) |
| PendingIntents | `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` (use MUTABLE only if an extra must be filled at send time) |
| Notification channel | Single channel id `medireminder_reminders`; sound + vibration; grouped notification when several doses share the same time |
| Dose identity in actions | Action intents carry `scheduleId` + `scheduledTime` (and/or `doseLogId`) so Taken/Snooze act on one precise dose |
| Settings keys | SharedPreferences file `medireminder_prefs`: `snooze_minutes`, `alert_delay_minutes`, `alert_enabled`, `ringtone_uri`, `dark_mode`, `large_text`, `debug_short_delays` |
| Permissions | Runtime: POST_NOTIFICATIONS (API 33+), SEND_SMS, exact-alarm setting (API 31+); manifest: RECEIVE_BOOT_COMPLETED; contact picker Intent avoids READ_CONTACTS |

## 4. Ambiguities / rules to settle before Stage 4

1. **Snooze vs missed timer:** spec says snooze logs SNOOZED and reschedules a reminder, but the original missed timer could still fire and create a MISSED log. Proposed rule: on Snooze → cancel the original missed timer, then schedule a new reminder AND a new missed timer for the snoozed time.
2. **Grouped notification actions:** each dose needs its own PendingIntent extra; decide the exact extras before writing `NotificationHelper`.
3. **Missed detection after reboot:** BootReceiver reschedules future alarms; decided whether pending missed-check alarms are also restored (proposed: yes, if their scheduledTime is still in the past but within the delay window).
4. **End date:** treat endDate as inclusive (whole day).

## 5. Planned file map

```
app/src/main/java/com/example/medireminder/
├── data/
│   ├── entity/       Medicine.java, Schedule.java, DoseLog.java, Caregiver.java, MedicineWithSchedules.java
│   ├── dao/          MedicineDao.java, ScheduleDao.java, DoseLogDao.java, CaregiverDao.java
│   ├── database/     AppDatabase.java, Converters.java (if needed for dates/bitmask)
│   └── repository/   MedicineRepository.java, DoseLogRepository.java, CaregiverRepository.java
├── ui/
│   ├── activity/     MainActivity.java, AddEditMedicineActivity.java, CaregiversActivity.java
│   ├── fragment/     HomeFragment.java, MedicinesFragment.java, HistoryFragment.java, SettingsFragment.java
│   └── adapter/      MedicineAdapter.java, DoseAdapter.java, ScheduleRowAdapter.java, LogAdapter.java
├── viewmodel/        MedicineViewModel.java, HomeViewModel.java, HistoryViewModel.java, CaregiverViewModel.java
├── receiver/         AlarmReceiver.java, ActionReceiver.java, MissedReceiver.java, BootReceiver.java
├── util/             AlarmScheduler.java, NotificationHelper.java, SmsHelper.java, PermissionHelper.java, PreferencesHelper.java
└── MediReminderApp.java
res/layout/ ...   res/values/strings.xml, styles.xml   AndroidManifest.xml   build.gradle
```

## 6. Stage status (update after each session)

- [x] Stage 1 — Gradle setup + Room entities, DAOs, relation, repository, singleton DB (Parts 1–3)
- [x] Stage 2 — Medicine list + Add/Edit screen with dynamic alarm rows and validation (Parts 4–5)
- [x] Stage 3 — AlarmScheduler, AlarmReceiver, notifications (Taken/Snooze), BootReceiver (Parts 6–7)
- [x] Stage 4 — Caregivers screen, missed-dose detection, SMS alert, permission dialogs (Parts 8–9)
- [x] Stage 5 — Home screen, History + adherence chart, Settings (Parts 10–12)
- [x] Stage 6 — Pause/Delete actions, polish, README, full test checklist (Part 13)

**Current stage:** ALL 13 PARTS COMPLETE. Final APK built (`BUILD SUCCESSFUL`) and installed on the phone (`adb install -r` → Success). Optional extras from the spec (French/English switch, TTS, widget, PDF export, Telegram, foreground service) are deliberately not implemented and are documented in `README.md`.
**Next move:** user tests the full app on the phone; fix anything reported. No further coding parts are planned.

## 7. Handoff template (fill at the end of each session)

```
Stage completed:
Files created/modified:
Decisions made (new or changed):
Build result (compile / run):
Known issues or shortcuts taken:
Next session — start with these exact files:
Tests passed from spec checklist (numbers):
```

### Handoff after Part 1

- Files created: `settings.gradle`, `build.gradle`, `gradle.properties`, `local.properties`, `gradlew.bat`, `gradle/wrapper/*`, `app/build.gradle`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `MainActivity.java`, `activity_main.xml`, `strings.xml`, `styles.xml`.
- Decisions: AGP 8.6.1 + Gradle 8.7 + **Java 17** (Temurin installed at `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.10002-hotspot`, fixed via `org.gradle.java.home` in `gradle.properties` because Android Studio's bundled JDK 25 is too new for Gradle 8.7) + compileSdk/targetSdk 34; Room 2.6.1, Material 1.12.0, lifecycle 2.8.7 pre-added so later parts don't need Gradle edits.
- Build result: **BUILD SUCCESSFUL** — `gradlew.bat assembleDebug` passed (33 tasks). APK installed on the phone: `adb install -r app\build\outputs\apk\debug\app-debug.apk` → **Success**.
- Phone was initially `unauthorized`; fixed by Developer options → Revoke USB debugging authorizations → toggle USB debugging off/on → replug → Allow USB debugging (Always allow).
- Next: user opens MediReminder to confirm the "MediReminder is ready." screen, then Part 2 (Room entities). ✅ Confirmed.

### Handoff after Part 2

- Files created: `data/entity/Medicine.java`, `Schedule.java`, `DoseLog.java`, `Caregiver.java`, `MedicineWithSchedules.java`.
- Decisions: dates stored as `"YYYY-MM-DD"` text; times as `long` epoch millis; `daysOfWeek` as comma-separated `"1,2,..."` (1=Monday..7=Sunday); status as `String` constants on `DoseLog`; public fields to keep the code short for a beginner; `Schedule` FK with `CASCADE`, `DoseLog` has no FK so history survives deletion.
- Build result: **BUILD SUCCESSFUL** (`assembleDebug`, 6 tasks executed). Not reinstalled because there is no UI change yet.
- Next: Part 3 — DAOs + Repository + `AppDatabase` singleton (still no UI). ✅ Done.

### Handoff after Part 3

- Files created: `data/dao/{MedicineDao,ScheduleDao,DoseLogDao,CaregiverDao}.java`, `data/database/AppDatabase.java`, `data/repository/{MedicineRepository,DoseLogRepository,CaregiverRepository}.java`, `util/AppExecutor.java`.
- Decisions: one shared single-thread `ExecutorService` for all DB writes; sync "Sync" DAO helpers exist for receivers (always called off the main thread); `saveMedicineWithSchedules()` replaces all alarms of a medicine; added `@Ignore` on convenience constructors and an index on `Schedule.medicineId` to remove Room warnings.
- Build result: **BUILD SUCCESSFUL**, zero warnings. Database name: `medireminder.db`, version 1.
- Not reinstalled (no UI change yet).
- Next: Part 4 — Medicines list screen (fragment + adapter + ViewModel), first visible feature. ✅ Done and installed.

### Handoff after Part 4

- Files created: `ui/fragment/MedicinesFragment.java`, `ui/adapter/MedicineAdapter.java`, `viewmodel/MedicineViewModel.java`, `res/layout/fragment_medicines.xml`, `res/layout/item_medicine.xml`.
- Files modified: `app/build.gradle` (added fragment 1.8.5), `activity_main.xml` (now a FragmentContainerView hosting MedicinesFragment), `strings.xml` (medicines UI strings).
- Decisions: MainActivity hosts one fragment for now; bottom navigation comes later. Empty state is a TextView toggled by the LiveData observer. Cards use MaterialCardView. No Add button yet — that arrives with the real form in Part 5 (no placeholder buttons).
- Build result: **BUILD SUCCESSFUL**. Installed on phone with `adb install -r` → Success.
- Expected on phone: title "Medicines" + "No medicines yet. Add one to get started."
- Next: Part 5 — Add/Edit medicine Activity with dynamic alarm rows and validation. ✅ Done and installed.

### Handoff after Part 5

- Files created: `ui/activity/AddEditMedicineActivity.java`, `res/layout/activity_add_edit_medicine.xml`, `res/layout/item_alarm.xml`.
- Files modified: `AndroidManifest.xml` (declared AddEditMedicineActivity), `MedicineAdapter.java` (item click interface), `MedicinesFragment.java` (FAB + edit navigation), `fragment_medicines.xml` (CoordinatorLayout + FAB), `strings.xml` (form strings + `form_options` array).
- Decisions: dates entered as `YYYY-MM-DD` text with lenient=false validation; Spinner for form; TimePickerDialog for each alarm; ±0.5 pill buttons with 0.5 minimum; 7 day CheckBoxes (1=Mon..7=Sun); "same dose" checkbox disables per-row pill buttons and overrides on save; tapping a card opens edit mode; no Pause/Delete buttons yet (they will be added when AlarmScheduler exists so alarms can be cancelled properly).
- Build result: **BUILD SUCCESSFUL** (one harmless deprecation note). Installed on phone → Success.
- Known limitation: **alarms are not scheduled yet**, so saving a medicine will show it in the list but will NOT produce a notification. That arrives in Parts 6–7.
- Next: Part 6 — `AlarmScheduler` + `AlarmReceiver` (background, no visible change).

### Handoff after Parts 6–13 (final build)

- Files created in Parts 6–7: `util/PreferencesHelper.java`, `util/NotificationHelper.java`, `util/AlarmScheduler.java`, `util/SmsHelper.java`, `util/DoseActionHelper.java`, `util/PermissionHelper.java`, `MediReminderApp.java`, `receiver/{AlarmReceiver,ActionReceiver,MissedReceiver,BootReceiver}.java`.
- Files created in Parts 8–9: `ui/activity/CaregiversActivity.java`, `ui/adapter/CaregiverAdapter.java`, layouts `activity_caregivers.xml`, `item_caregiver.xml`, `dialog_caregiver.xml`.
- Files created in Parts 10–12: `util/TimeUtils.java`, `ui/fragment/{HomeFragment,HistoryFragment,SettingsFragment}.java`, `ui/adapter/{DoseAdapter,LogAdapter}.java`, `ui/view/AdherenceChartView.java`, layouts `activity_main.xml` (bottom nav), `fragment_{home,history,settings}.xml`, `item_dose.xml`, `item_log.xml`, `menu/bottom_nav_menu.xml`, `values/colors.xml`.
- Files created in Part 13: `README.md`. Modified: `item_medicine.xml`, `MedicineAdapter.java`, `MedicinesFragment.java`, `MedicineRepository.java` (Pause/Resume + Delete with alarm cancellation), `MainActivity.java` (bottom nav, permission dialogs, large text, auto-reschedule on resume), `AlarmReceiver.java` (snooze missed-check starts from now), `HomeFragment.java`/`HistoryFragment.java` (removed duplicate loads).
- Decisions: grouped notification uses one id per minute and passes **all** `scheduleIds` at that time; Taken/Snooze act on the whole group; snooze request code uses `scheduleId`, not `doseLogId`; snooze cancels the old missed timer and schedules a fresh missed check from the snooze fire time; BootReceiver calls `rescheduleAll()`; `debug_short_delays` forces 1-minute snooze and 1-minute missed delay; dark mode applies via `AppCompatDelegate`, large text via `MainActivity.attachBaseContext()`.
- Build result: repeated **BUILD SUCCESSFUL** (`gradlew.bat assembleDebug`). Final install: `adb install -r` → **Success** on device `5200f6b742c416d7`.
- Known shortcuts: SMS permission is requested with an explanation; exact-alarm permission opens system settings with an explanation. No foreground service, no French, no TTS/widget/PDF/Telegram (documented in README). History rows use text instead of a separate day-header view. Room DB stays version 1.
- Spec checklist coverage: all 14 acceptance paths are implemented in code; **not yet confirmed by the user on the phone** after the final install.
- Final user request: changed the launcher icon to `C:\Users\zemze\Desktop\mobile.jpg` (center-cropped and generated into `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` + `ic_launcher_round.png`), declared `android:icon` / `android:roundIcon` in the manifest, rebuilt (`BUILD SUCCESSFUL`) and reinstalled on the phone (`Success`), then initialized Git and pushed to `https://github.com/zemzemahmed/mobile.git` on branch `main` (private repo; signed in with Git Credential Manager account `zemzemahmed`). Added `.gitignore` so `local.properties`, `.gradle/` and `build/` are not uploaded.

### How to test on the physical phone (USB)

1. Keep the phone connected with USB and **unlocked**.
2. If a dialog appears: **Allow USB debugging** → tick **Always allow from this computer** → OK.
   (If no dialog appears, change the USB mode from "Charging" to "File Transfer / MTP" once.)
3. Verify the connection: `adb devices` → the device must show as `device`, not `unauthorized` or offline.
4. Build: `gradlew.bat assembleDebug`
5. Install/reinstall: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
6. Open **MediReminder** from the app drawer → expected screen: centered text **"MediReminder is ready."**
7. Optional check: `adb logcat` can show crashes if the app closes immediately.

## 8. Spec test checklist (acceptance criteria — don't retype, just reference numbers)

1 three alarms → 3 rows + 3 distinct alarms · 2 notification pill count + actions · 3 Taken logs/stock/cancels timer · 4 Snooze reschedules · 5 Ignored → MISSED + SMS · 6 same-time grouping · 7 edit cancels/recreates · 8 delete removes all · 9 reboot restores · 10 pause stops alarms · 11 deny SMS no crash · 12 low stock warning · 13 half pill · 14 rotation keeps form data.
