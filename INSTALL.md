# 📱 Installations- & Build-Anleitung (Debug & Release)

Herzlich willkommen! In dieser Anleitung erfährst du Schritt für Schritt und absolut verständlich, wie du das Projekt **Insulin Calculator** bauen, auf deinem Android-Gerät installieren oder für den Release (z. B. F-Droid oder Play Store) vorbereiten kannst.

---

## 📋 Inhaltsverzeichnis
1. [Voraussetzungen](#1-voraussetzungen)
2. [Variante A: Mit Android Studio (Empfohlen & am einfachsten)](#variante-a-mit-android-studio-ide)
   - [Debug-Modus starten (Entwicklung & Testen)](#-debug-modus-mit-android-studio)
   - [Release-Modus bauen (Signierte APK / Bundle erstellen)](#-release-modus-mit-android-studio)
   - [Eigenen Signatur-Schlüssel (Keystore) erstellen](#-eigenen-signatur-schlüssel-keystore-erstellen)
3. [Variante B: Ohne IDE (Reine Terminal- & Gradle-Befehle)](#variante-b-reine-terminal--gradle-befehle-cli)
   - [Debug-APK bauen und installieren](#-debug-build-per-terminal)
   - [Release-Build (APK & App Bundle) per CLI bauen](#-release-build-per-terminal)
4. [🤖 Veröffentlichung über F-Droid](#-veröffentlichung-über-f-droid)
   - [Wie F-Droid funktioniert](#1-wie-f-droid-funktioniert)
   - [Welche Schlüssel/Keys verwendet werden](#2-welche-schlüssel-f-droid-nutzt)
   - [Voraussetzungen im Quellcode für F-Droid](#3-voraussetzungen-für-f-droid)
5. [⚙️ R8 / ProGuard (`isMinifyEnabled`) Einstellungen](#-r8--proguard-isminifyenabled-einstellungen)
6. [⚖️ Unterschied zwischen Debug und Release auf einen Blick](#-unterschied-debug-vs-release)
7. [💡 Häufige Fehler & schnelle Lösungen](#-tipps--fehlerbehebung)

---

## 1. Voraussetzungen

Bevor es losgeht, stelle sicher, dass Folgendes vorhanden ist:

* **Java Development Kit (JDK):** Mindestens JDK 21 (empfohlen: JDK 21 oder JDK 25 passend zur Projektkonfiguration).
* **Android SDK:** Installiert über Android Studio oder Command Line Tools.
* **USB- / WLAN-Debugging am Smartphone:**
  * Öffne auf deinem Android-Gerät die **Einstellungen** → **Telefoninfo**.
  * Tippe 7-mal schnell auf die **Build-Nummer**, bis *„Du bist jetzt Entwickler!“* erscheint.
  * Gehe in die neuen **Entwickleroptionen** und aktiviere **USB-Debugging** (oder **Kabelloses Debugging / WLAN-Debugging**).
  * Verbinde dein Smartphone per USB-Kabel mit dem PC bzw. verbinde es im selben WLAN und bestätige die Abfrage auf dem Display.

---

## Variante A: Mit Android Studio (IDE)

### 🛠️ Debug-Modus mit Android Studio
*Ideal zum Ausprobieren, Testen neuer Funktionen und für Live-Änderungen.*

1. **Projekt öffnen:**  
   Starte Android Studio und wähle **Open** → wähle den Projektordner aus.
2. **Synchronisierung abwarten:**  
   Warte kurz, bis Gradle die Abhängigkeiten geladen hat (*„Gradle sync finished“* unten in der Statusleiste).
3. **Build-Variante prüfen:**  
   Klicke ganz links unten auf den Reiter **Build Variants** und stelle sicher, dass bei `:app` die Variante **`debug`** ausgewählt ist.
4. **Gerät auswählen & Starten:**  
   * Wähle oben in der Menüleiste dein angeschlossenes Android-Gerät (oder einen Emulator) im Dropdown-Menü aus.
   * Klicke auf den **grünen Play-Button (Run ▶)** oder drücke `Shift + F10`.
   * Die App wird automatisch gebaut, auf dein Handy übertragen und gestartet!

---

### 🚀 Release-Modus mit Android Studio
*Erstellt eine optimierte, verschlüsselte und geschrumpfte Version für den Alltag oder den Play Store.*

1. Gehe in der oberen Menüleiste auf **Build** → **Generate Signed Bundle / APK...**
2. Wähle aus:
   * **Android App Bundle (`.aab`):** Wenn du die App im Google Play Store hochladen möchtest.
   * **APK (`.apk`):** Wenn du eine fertige Installationsdatei haben möchtest, die du direkt per USB/Messenger/GitHub-Release auf Smartphones verteilen kannst.
3. Klicke auf **Next**.
4. **Keystore angeben** (siehe nächster Abschnitt für das Erstellen).
5. **Release-Variante wählen:**
   * Wähle **`release`** aus.
   * Klicke auf **Finish / Create**.
6. **Fertige Datei finden:**
   * Nach wenigen Sekunden erscheint unten rechts eine Benachrichtigung: *„Generate Signed APK: APKS generated successfully“*.
   * Klicke auf **locate**, um den Ordner mit der fertigen `.apk` oder `.aab` direkt im Datei-Explorer zu öffnen.

---

### 🔑 Eigenen Signatur-Schlüssel (Keystore) erstellen
Für eigene Release-APKs und den Google Play Store brauchst du einen eigenen Keystore. Den erstellst du so:

1. In Android Studio: **Build** → **Generate Signed Bundle / APK...** → **APK** wählen → **Next**.
2. Unter **Key store path** auf **Create new...** klicken.
3. Wähle einen Speicherort (z. B. `C:\Keys\my-release-key.jks`) und vergebe ein sicheres Passwort.
4. Fülle die Alias-Felder aus (z. B. Alias: `release-key`, Validity: `25` Jahre, Name: dein Name).
5. Klicke auf **OK**.
6. ⚠️ **Sehr wichtig:** Sichere diese `.jks`-Datei und deine Passwörter gut! Wenn du die Datei verlierst, können spätere App-Updates auf Geräten der Nutzer nicht mehr über die alte Version installiert werden.

---

## Variante B: Reine Terminal- / Gradle-Befehle (CLI)

Wenn du kein Android Studio geöffnet hast oder auf einem Server/Terminal arbeitest, kannst du den Gradle-Wrapper (`gradlew`) direkt über die Konsole (Eingabeaufforderung, PowerShell oder Linux/macOS Terminal) nutzen.

> 💡 **Hinweis für Windows:** Nutze `gradlew.bat` (oder `.\gradlew`).  
> 💡 **Hinweis für Mac/Linux:** Nutze `./gradlew` (vorher ggf. einmalig `chmod +x gradlew` ausführen).

---

### 🔨 Debug-Build per Terminal

#### 1. Debug-APK nur kompilieren (ohne Handy):
```bash
# Windows
gradlew.bat assembleDebug

# Mac / Linux
./gradlew assembleDebug
```
*Die fertige Datei liegt danach unter:*  
📁 `app/build/outputs/apk/debug/app-debug.apk`

---

#### 2. Debug-APK direkt auf angeschlossenem Smartphone installieren & starten:
```bash
# Windows
gradlew.bat installDebug

# Mac / Linux
./gradlew installDebug
```

---

### 📦 Release-Build per Terminal

#### 1. Release-APK bauen (Unsigned / Generisch):
```bash
# Windows
gradlew.bat assembleRelease

# Mac / Linux
./gradlew assembleRelease
```
*Die fertige Datei liegt danach unter:*  
📁 `app/build/outputs/apk/release/app-release-unsigned.apk`

---

#### 2. Release Android App Bundle für den Play Store (`.aab`) bauen:
```bash
# Windows
gradlew.bat bundleRelease

# Mac / Linux
./gradlew bundleRelease
```
*Die fertige Bundle-Datei liegt danach unter:*  
📁 `app/build/outputs/bundle/release/app-release.aab`

---

## 🤖 Veröffentlichung über F-Droid

F-Droid ist der bekannteste Open-Source-App-Store für Android. F-Droid funktioniert grundlegend anders als der Google Play Store:

### 1. Wie F-Droid funktioniert
* **Kein APK-Upload:** Du lädst bei F-Droid **keine** fertigen `.apk`-Dateien hoch!
* **Build aus Source:** Der F-Droid-Server zieht deinen Quellcode direkt aus deinem öffentlichen **GitHub-Repository** und kompiliert die APK komplett selbstständig (`assembleRelease`).
* **Wie man die App einreicht:**
  1. Erstelle ein Git-Tag für dein Release auf GitHub (z. B. `git tag v1.1.2 && git push --tags`).
  2. Reiche einen Merge Request im [F-Droid Data Repository](https://gitlab.com/fdroid/fdroiddata) mit den Metadaten deiner App (GitHub-Link, Lizenz, Tag-Name) ein.

### 2. Welche Schlüssel F-Droid nutzt
* F-Droid baut und signiert die offizielle F-Droid-Version mit **seinem eigenen offiziellen F-Droid-Master-Key**.
* Das garantiert den Nutzern, dass die APK zu 100% aus dem quelloffenen Code gebaut wurde und frei von Tracking/Proprietärem ist.
* Du musst F-Droid **keinen privaten Keystore** zur Verfügung stellen.

*(Hinweis: Falls du stattdessen ein eigenes persönliches F-Droid-Repository wie IzzyOnDroid nutzt, erstellst du die Release-APK mit deinem eigenen Keystore und lädst sie bei GitHub Releases hoch).*

### 3. Voraussetzungen für F-Droid
* Der gesamte Code muss unter einer Open-Source-Lizenz stehen (z. B. GPL, MIT, Apache 2.0).
* Keine unfreien / proprietären Werbe- oder Tracking-SDKs.

---

## ⚙️ R8 / ProGuard (`isMinifyEnabled`) Einstellungen

In `app/build.gradle.kts` gibt es unter `buildTypes.release` den Schalter `isMinifyEnabled`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true // oder false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

* **`isMinifyEnabled = false` (Standard / Einfach):**
  * Der Code wird nicht verschleiert und ungenutzte Klassen werden nicht entfernt.
  * **Vorteil:** Schnellster Build, null Risiko von Reflection-Fehlern.
  * **Nachteil:** Die APK-Datei ist etwas größer.
* **`isMinifyEnabled = true` (Optimiert / Empfohlen für Releases):**
  * Aktiviert das R8/ProGuard-Optimierungstool.
  * Entfernt ungenutzten Code und schrumpft die APK um bis zu 60%.
  * Dank der bereits eingerichteten Regeln in `app/proguard-rules.pro` bleiben Retrofit, Moshi und Room dabei voll funktionsfähig!
* **Muss man das in AI Studio ändern?**  
  Nein. In der AI-Studio-Cloud und bei schnellen Tests kannst du es bei `false` lassen. Wenn du eine finale Release-APK für Nutzer erstellen willst, kannst du es auf `true` setzen.

---

## ⚖️ Unterschied: Debug vs. Release

| Eigenschaft | 🛠️ Debug | 🚀 Release |
| :--- | :--- | :--- |
| **Zweck** | Entwickeln, Testen, Fehler suchen | Veröffentlichung, finale Nutzung |
| **Geschwindigkeit** | Normal | Spürbar schneller optimiert (R8/ProGuard) |
| **App-Dateigröße** | Größer (enthält Debug-Symbole) | Deutlich kleiner (durch Code-Shrinking) |
| **Code-Schutz** | Klartext | Verschleiert & geschützt (Obfuscation) |
| **Signatur** | Automatischer Android Debug-Key | Eigener Keystore (bzw. F-Droid Key) |

---

## 💡 Tipps & Fehlerbehebung

* **Fehler: *„Device offline“* oder Handy wird nicht erkannt:**  
  Zieh das USB-Kabel ab, steck es neu ein und prüfe, ob auf dem Handy ein Dialogfenster *„USB-Debugging zulassen?“* erscheint. Setze dort das Häkchen bei *„Von diesem Computer immer zulassen“* und bestätige mit **OK**.
* **Fehler: *„Gradle Wrapper not found“*:**  
  Stelle sicher, dass du dich im Hauptverzeichnis des Projekts befindest, in dem die Datei `gradlew` liegt.
* **Sauberer Neuaufbau (Clean Build):**  
  Wenn etwas mal hakt oder alte Cache-Dateien stören:
  ```bash
  ./gradlew clean assembleDebug
  ```
