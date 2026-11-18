# iOS 27 Dark Suite — Honor MagicOS & Android

Komplette, zu 100 % programmatisch generierte Theme- und Widget-Suite im **iOS 27 Dark Design** – speziell optimiert für das **Honor Magic 9 Pro Max (MagicOS China-ROM)** sowie Standard-Android.

Alle Icons, System-Masken (`icon_back.png`, `icon_mask.png`, `icon_upon.png`), Vorschaubilder und Widget-Grafiken werden ohne fremde APK-Assets direkt über Python (PyCairo & Pillow) und Kotlin/Android-Canvas erzeugt.

---

## Enthaltene Komponenten & Ausgabedateien

| Datei | Zweck |
|---|---|
| `output/iOS27_Dark_MagicOS.hnt` | Fertiges Honor-MagicOS-Theme (135+ Apps + Aliase + Auto-Masking) |
| `output/iOS27_Dark_MagicOS.hwt` | Kompatible Theme-Kopie für ältere/alternative Huawei/Honor-Manager |
| `output/iOS27_Widgets.apk` | Eigenständige App **„iOS 27 Widgets“** mit 16 Live-Widgets (2×2, 4×2, 4×4) |
| `output/iOS27_Dark_IconPack.apk` | Standard-Android Icon-Pack für Custom Launcher (Nova, Lawnchair, Niagara etc.) |

---

## Teil 1: Installation auf dem Honor Magic 9 Pro Max (MagicOS China-ROM)

### A. Icons & Auto-Masking installieren (`iOS27_Dark_MagicOS.hnt`)

1. Kopiere **`output/iOS27_Dark_MagicOS.hnt`** auf dein Smartphone (z. B. per USB oder Cloud in den Ordner `Downloads` oder `Honor/Themes/`).
   - *Hinweis:* Falls die Honor-Themen-App `.hnt` im China-ROM nicht direkt im Datei-Explorer öffnet, nutze die Kopie **`iOS27_Dark_MagicOS.hwt`** oder verschiebe die Datei in den internen Speicher unter `Honor/Themes/` (bzw. `Huawei/Themes/`).
2. Öffne die **Dateien-App** (`Files`), tippe die `.hnt`-Datei an und wähle **Mit „Design“ / „Themes“ öffnen** (oder öffne direkt die Honor **Design/Themes-App** → **Mein / Me** → **Meine Designs / Themes**).
3. Wähle **iOS 27 Dark** aus und tippe auf **Anwenden** (oder **Anpassen** → **Symbole / Icons**, falls du dein bestehendes Hintergrundbild behalten möchtest).
4. **Auto-Masking für alle Apps:** Durch `icon_back.png`, `icon_mask.png` und `icon_upon.png` erhalten auch Apps ohne eigenes Icon automatisch den dunklen Squircle-Hintergrund (`#18181B` → `#09090B`) mit 1px Glas-Lichtrand.

---

### B. Live-Widgets installieren (`iOS27_Widgets.apk`)

1. Kopiere **`output/iOS27_Widgets.apk`** auf das Honor Magic 9 Pro Max und installiere die APK (bei Rückfrage „Aus unbekannter Quelle installieren“ einmalig erlauben).
2. Öffne die App **iOS 27 Widgets** einmalig.
3. Tippe im Bereich **Berechtigungen (1-Klick)** auf:
   - **Standort (Wetter)** — für lokalen Standort via Open-Meteo (alternativ Stadt direkt eintippen).
   - **Kalender (Termine)** — für echte Systemkalender-Einträge.
   - **Musik / Benachrichtigungen** — für **Now Playing** (Spotify, Apple Music, YouTube Music, NetEase, QQ Music).
   - **Bluetooth-Geräte (Akku)** — für Kopfhörer-/Smartwatch-Akkuringe.
   - **Speicher / Fotos** — für das Fotos-Widget (2×2 & 4×4).
4. **Wichtiger China-ROM-Tipp (MagicOS Hintergrund-Schutz):**
   - Öffne **Einstellungen → Apps → App-Start / Akku → iOS 27 Widgets**
   - Stelle von *Automatisch verwalten* auf **Manuell verwalten** und aktiviere **Auto-Start**, **Sekundärer Start** und **Im Hintergrund ausführen**.
   - So aktualisieren sich Wetter-, Aktien-, Akku- und Musik-Widgets auch im China-ROM zuverlässig im Hintergrund.
5. **Widgets auf dem Homescreen platzieren:**
   - Zwei Finger auf dem MagicOS-Homescreen zusammenziehen → **Widgets / Karten** → **iOS 27 Widgets** auswählen (oder in der App direkt auf **Hinzufügen** tippen).

#### Alle 16 enthaltenen Widgets (2×2, 4×2, 4×4)

- **Uhr & Weltuhr:** Analog-Uhr (2×2, iOS-Dark-Zifferblatt + roter Sekundenzeiger), Digital-Uhr (2×2), Weltuhr (4×2 mit 4 Zeitzonen).
- **Wetter:** Klein (2×2) & Mittel (4×2 mit 5-Stunden-Vorschau) über kostenlose **Open-Meteo API** (kein API-Key nötig).
- **Kalender:** Klein (2×2, roter Wochentag + große Tageszahl) & Mittel (4×2 mit nächsten Systemterminen).
- **Batterie & Geräte:** Klein (2×2) & Mittel (4×2) mit Apple-Fortschrittsringen (Grün/Orange/Rot) für Smartphone & verbundene Bluetooth-Geräte.
- **Aktien & Watchlist:** Klein (2×2) & Mittel (4×2) mit konfigurierbaren Tickern (Standard: S&P 500 `^GSPC`, NVIDIA `NVDA`, Apple `AAPL`, MSCI World `URTH`), Kurs, `%`-Badge & Sparkline-Chart via Yahoo Finance.
- **Musik / Now Playing (4×2):** Zeigt Albumcover, Titel, Interpret + funktionierende Buttons für **Zurück / Play-Pause / Weiter**.
- **Erinnerungen / To-Do:** Klein (2×2) & Mittel (4×2) — Aufgaben direkt auf dem Homescreen abhakbar.
- **Fotos:** Klein (2×2) & Groß (4×4) — eigene Galerie-Bilder randlos im 24dp-iOS-Rahmen (Tippen wechselt bei mehreren Fotos zum nächsten Bild).

---

## Teil 2: Standard-Android & Custom Launcher

Falls du auf einem beliebigen Android-Gerät einen Custom Launcher (**Nova Launcher**, **Lawnchair**, **Niagara**, **Smart Launcher**) nutzt:

1. Installiere **`output/iOS27_Dark_IconPack.apk`**.
2. Wähle in den Launcher-Einstellungen unter *Erscheinungsbild / Icon-Design* **iOS 27 Dark Icons** aus.
3. Das Paket bringt `appfilter.xml`, `drawable.xml` und `theme_resources.xml` inklusive Auto-Masking (`icon_back`, `icon_mask`, `icon_upon`) mit.

---

## Lokal selbst bauen

### Voraussetzungen
- Python 3.10+ mit `pycairo` und `pillow`
- JDK 17 & Android SDK (`compileSdk 36`)

### Befehle

```bash
# 1. Icons (192x192 & 432x432), Masken & Honor .hnt/.hwt erzeugen
python3 icons_generator/build_icons.py
python3 icons_generator/build_honor_hnt.py

# 2. Android-Widgets-App bauen
cd widgets-app && ./gradlew assembleDebug && cd ..
cp widgets-app/app/build/outputs/apk/debug/app-debug.apk output/iOS27_Widgets.apk

# 3. Standard-Android Icon-Pack-APK bauen
cd iconpack-apk && ./gradlew assembleDebug && cd ..
cp iconpack-apk/app/build/outputs/apk/debug/app-debug.apk output/iOS27_Dark_IconPack.apk
```

---

## Lizenz

Dieses Projekt steht unter der **MIT-Lizenz** (siehe [LICENSE](LICENSE)).
Alle Quellcodes, Icons, Masken und Widget-Grafiken wurden programmatisch erstellt und können frei genutzt, verändert und weitergegeben werden. Erwähnte App- und Markennamen gehören ihren jeweiligen Rechteinhabern.
