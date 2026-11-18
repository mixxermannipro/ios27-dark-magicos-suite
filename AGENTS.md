# AGENTS.md – iOS 27 Dark Suite

Projektregeln für die **iOS 27 Dark Suite** (Honor Magic 9 Pro Max / MagicOS China-ROM + Standard-Android).

## Struktur
- `icons_generator/` — Python-Generator (`apps.json`, `build_icons.py`, `build_honor_hnt.py`) für 192×192 & 432×432 Squircle-Icons und Honor `.hnt`/`.hwt`-Pakete.
- `iconpack-apk/` — Standard-Android Icon-Pack-App (Kotlin + Gradle) für Custom Launcher.
- `widgets-app/` — Eigenständige Android-Widget-App „iOS 27 Widgets“ (Kotlin + Gradle).
- `output/` — Fertige Artefakte (`iOS27_Dark_MagicOS.hnt`, `iOS27_Dark_MagicOS.hwt`, `iOS27_Widgets.apk`, `iOS27_Dark_IconPack.apk`).

## Regeln
- Alle Grafiken, Icons, Masken und Widget-UI-Elemente sind 100 % programmatisch erzeugt (keine fremden APK-Grafiken).
- Keine Secrets, API-Keys oder persönliche Zugangsdaten im Repo speichern.
- Nach Änderungen an `icons_generator/` immer `python3 icons_generator/build_icons.py && python3 icons_generator/build_honor_hnt.py` ausführen und `.hnt`/`.hwt` prüfen.
- Nach Änderungen an `widgets-app/` oder `iconpack-apk/` immer `./gradlew assembleDebug` mit JDK 17 ausführen.
