# DAYFLOW

**Your Life, In Flow.**

DAYFLOW is an Arabic-first, offline Android productivity app published by DSTWR.

## Included modules
- Dashboard with daily task progress and quick summaries.
- Task list with completion, editing, and local persistence.
- Monthly calendar with month navigation and configurable week start.
- Notes with local create, edit, and delete.
- Reminders with Android notifications. To schedule one, begin its details with `YYYY-MM-DD HH:mm`, followed by an optional message.
- Habit tracker with daily check-off.
- Focus timer with 15, 25, 45, and 60 minute sessions.
- Goals list.
- Local progress overview and settings.

## Product principles
- Arabic-first, right-to-left interface; DAYFLOW wordmark remains LTR.
- Local-first data. No account, remote server, subscription, or always-on network.
- Android notification scheduling with graceful fallback when exact alarms are unavailable.
- Dark violet visual language with gold accents.
- Android 10+ support.

## Technology
Kotlin, Jetpack Compose, Material 3, Room foundation, DataStore dependency, and Android notification/alarm APIs.

## Build
The GitHub Actions workflow is **manual-only**. It runs only when a repository owner explicitly starts the DAYFLOW Android Build workflow from the Actions tab. No push or pull-request event starts a build automatically.

## Current implementation notes
The active interface currently stores its task, note, reminder, habit, and goal records locally using Android SharedPreferences. Room entities and DAO are present as the next persistence migration foundation. Calendar task association currently uses an ISO date included in task details. Reminder details must begin with the date/time format above.
