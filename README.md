# 💉 Insulin-Rechner & Mahlzeiten-Assistent

Eine moderne, übersichtliche und intuitive Android-App zur schnellen und präzisen Berechnung von Mahlzeiten- und Korrekturinsulin für Menschen mit Diabetes (Typ 1 & Typ 3). Entwickelt mit **Kotlin**, **Jetpack Compose (Material 3)** und intelligenter Mahlzeitenerkennung via **Gemini AI**.

---

## 🌟 Hauptfunktionen

### 1. 🧮 Präzise Bolus-Berechnung
* **Flexible Einheiten**: Standardmäßig auf **Broteinheiten (BE, 12g KH)** eingestellt – nahtlos umschaltbar auf **Kohlenhydrateinheiten (KE / KHE, 10g KH)** oder reine **Gramm Kohlenhydrate (g KH)**.
* **Echtzeit-Umrechnung**: Alle drei Einheiten (BE, KE, g KH) werden parallel und transparent angezeigt.
* **Feinjustierung in 0,05er-Schritten**: Individuelle Insulin-Faktoren können exakt in 0,05 IE-Schritten angepasst werden (z. B. `0,45`, `0,50`, `0,55`, `1,25` IE).

### 2. ⏰ Tageszeitabhängige Faktoren & Blutzuckerkorrektur
* **Automatische Tageszeiterkennung**: Erkennt Morgens, Mittags, Abends und Nachts und wählt automatisch den passenden Faktor.
* **Korrektur-Bolus**: Berechnet anhand des aktuellen Blutzuckerwertes, des individuellen Zielwerts und des Korrekturfaktors die benötigte Korrektur-Dosis.
* **Hypoglykämie-Schutz**: Sicherheitswarnungen bei niedrigen Blutzuckerwerten (< 70 mg/dl) mit Hinweisen zur Kohlenhydrataufnahme (z. B. Traubenzucker).

### 3. 🤖 KI-Mahlzeitenschätzer (Gemini AI)
* Mahlzeiten und Zutaten in natürlicher Sprache beschreiben (z. B. *"2 Scheiben Vollkornbrot mit Käse und ein kleiner Apfel"*).
* Die integrierte KI schätzt die Kohlenhydrate, Broteinheiten und Portionsgrößen zuverlässig und überträgt die Werte mit einem Klick direkt in den Rechner.

### 4. 📖 Digitales Tagebuch & Verlauf
* Protokollierung aller Berechnungen mit Zeitstempel, Mahlzeit, BE/KE, Blutzucker und berechneten Insulineinheiten.
* Filterung nach Zeiträumen (Alle, Heute, 7 Tage, 30 Tage).
* Export- und Teilen-Funktion für Arztbesuche oder Diabetesschulungen.

### 5. 🎨 Design & Barrierefreiheit
* **6 moderne Farbschemata**:
  * 🩺 *Medizinisch Türkis* (Standard)
  * 🌊 *Ozean Blau*
  * 🍃 *Smaragd Grün*
  * 🌅 *Sonnenuntergang Bernstein*
  * 🍇 *Beere & Violett*
  * 🌌 *Mitternacht AMOLED* (Tiefschwarz & akkuschonend)
* **Darstellungsmodi**: System, Hell und Dunkel (Dark Mode).

---

## 🛠️ Technische Details

* **Programmiersprache**: 100% Kotlin
* **UI-Framework**: Jetpack Compose mit Material Design 3 (M3)
* **Architektur**: MVVM (Model-View-ViewModel) mit StateFlow & Coroutines
* **Lokale Datenbank**: Android Room Database (SQLite) für offlinefähige und sichere Datenspeicherung
* **KI-Integration**: Google Generative AI / Gemini API
* **Mindestanforderung**: Android 8.0 (API Level 26) oder höher

---

## 📱 APK erstellen und auf dem Smartphone installieren

### Weg 1: Mit Android Studio (Empfohlen)
1. Lade das Projekt herunter (Export als `.zip`) und entpacke das Archiv.
2. Öffne **Android Studio** und wähle **Open** ➔ Wähle den entpackten Projektordner aus.
3. Klicke in der oberen Menüleiste auf:
   ```
   Build ➔ Build Bundle(s) / APK(s) ➔ Build APK(s)
   ```
4. Nach Abschluss des Builds erscheint unten rechts eine Meldung mit dem Link **`locate`**.
5. Die generierte Datei `app-debug.apk` kannst du direkt auf dein Smartphone kopieren (z. B. via Google Drive, WhatsApp Web oder E-Mail) und installieren.

### Weg 2: Über die Befehlszeile (Gradle)
Führe im Projektverzeichnis folgenden Befehl im Terminal aus:
```bash
./gradlew assembleDebug
```
Die fertige APK befindet sich im Verzeichnis:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ⚠️ Wichtiger medizinischer Hinweis

> **Haftungsausschluss**: Diese App dient als digitaler Rechenhelfer und Orientierungshilfe. Sie ersetzt **nicht** die ärztliche Beratung, Diagnostik oder Therapieempfehlung durch Fachpersonal (Diabetologe/Diabetologin). Alle Insulindosierungen und Faktoren sollten stets in Absprache mit medizinischem Fachpersonal festgelegt und eigenverantwortlich überprüft werden.
