# DAYFLOW

**Your Life, In Flow.**

DAYFLOW is an offline-first Arabic Android productivity app by DSTWR.

## Product principles
- Arabic-first, right-to-left experience; English product wordmark remains LTR.
- Local-first storage. No account, server, subscription, or always-on network.
- Reliable reminders using Android scheduling APIs and notification permissions.
- Premium dark violet glass visual language with restrained gold accents.
- Accessibility, battery awareness, and graceful behavior on Android 10+.

## Planned modules
Dashboard, tasks, calendar, notes, reminders, habits, focus timer, goals, statistics, settings, home-screen widgets, and local backup/restore.

## Technology
Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room, DataStore, Hilt, and Android notification/alarm APIs.

## Build
Open this repository in Android Studio and run the `:app` configuration. CI builds a debug APK on manual dispatch and pushes to the default branch.

## Status
Foundation setup. Features are implemented incrementally with functional tests; mock-only controls are not considered complete.
