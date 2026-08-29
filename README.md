# 💉 Insulin-Rechner & Mahlzeiten-Assistent (v1.2.1)

Eine moderne, datenschutzorientierte und intuitive Android-App zur schnellen und präzisen Berechnung von Mahlzeiten- und Korrekturinsulin für Menschen mit Diabetes (Typ 1 & Typ 3). Entwickelt mit **Kotlin**, **Jetpack Compose (Material 3)**, militärischer **SQLCipher AES-256 Datenbankverschlüsselung**, robuster Offline-Speicherung via **Room** und intelligenter Mahlzeitenerkennung via **Gemini AI**.

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

### 3. 🔒 Zero-Knowledge & SQLCipher AES-256 Verschlüsselung
* **Hardware-gestützte Sicherheit**: Vollständige 256-Bit-Verschlüsselung aller lokalen Datenbankdaten (`SQLCipher`) mit Schlüsseln, die im hardware-gesicherten **Android KeyStore** (`MasterKey` & `EncryptedSharedPreferences`) verwaltet werden.
* **Automatische Migration**: Vorherige unverschlüsselte SQLite-Datenbanken werden beim Start automatisch und verlustfrei in verschlüsselte SQLCipher-Datenbanken migriert.
* **Vollständige Datensouveränität**: Keine Weitergabe von Patientendaten an Drittanbieter; alle Berechnungen und Tagebucheinträge bleiben geschützt auf dem Gerät.

### 4. 🤖 KI-Mahlzeitenschätzer (Gemini AI)
* Mahlzeiten und Zutaten in natürlicher Sprache beschreiben (z. B. *"2 Scheiben Vollkornbrot mit Käse und ein kleiner Apfel"*).
* Die integrierte KI schätzt die Kohlenhydrate, Broteinheiten und Portionsgrößen zuverlässig und überträgt die Werte mit einem Klick direkt in den Rechner.
* **Offline-Fallback**: Robuste Notfall-Schätzung und klare Fehlerbehandlung bei fehlender Internetverbindung.

### 5. 📖 Digitales Tagebuch & Backup-Manager
* Protokollierung aller Berechnungen mit Zeitstempel, Mahlzeit, BE/KE, Blutzucker und berechneten Insulineinheiten.
* Filterung nach Zeiträumen (Alle, Heute, 7 Tage, 30 Tage).
* **Backup & Restore**: Vollständige Datensicherung (JSON / CSV) zur sicheren Mitnahme bei Gerätewechsel oder für Arztberichte.

### 6. 🎨 Modernes Material 3 Design
* **6 harmonische Farbschemata**:
  * 🩺 *Medizinisch Türkis* (Standard)
  * 🌊 *Ozean Blau*
  * 🍃 *Smaragd Grün*
  * 🌅 *Sonnenuntergang Bernstein*
  * 🍇 *Beere & Violett*
  * 🌌 *Mitternacht AMOLED* (Tiefschwarz & stromsparend)
* **Darstellungsmodi**: System, Hell und Dunkel (Dark Mode).

---

## 🛠️ Technische Details & Architektur

* **Version**: `1.1.3` (VersionCode: `3`)
* **Programmiersprache**: 100% Kotlin (mit Kotlin Coroutines & Flow)
* **Java-Laufzeit / Toolchain**: Java 21 (LTS)
* **Android Gradle Plugin (AGP)**: `9.1.0` (optimiert für Gradle 9.3.1)
* **UI-Framework**: Jetpack Compose mit Material Design 3 (M3)
* **Architektur**: Clean MVVM (Model-View-ViewModel) mit unidirektionalem Datenfluss
* **Lokale Persistenz & Sicherheit**: Android Room Database mit SQLCipher (`net.zetetic:sqlcipher-android:4.18.0`) und AndroidX Security Crypto (`1.1.0`)
* **Netzwerk & Serialisierung**: Retrofit 3, OkHttp 5 & Moshi
* **Release-Optimierung**: R8 / ProGuard vorkonfiguriert (`app/proguard-rules.pro`)
* **Continuous Integration**: Automatische Dependabot-Dependency-Updates (`.github/dependabot.yaml`)
* **Kompatibilität**:
  * `minSdk = 30` (Android 11+)
  * `targetSdk = 37` & `compileSdk = 37`

---

## 📱 Installation & Bauen

Ausführliche Anleitungen für **Debug-Builds**, **Release-Builds**, das Erstellen eines **Keystores** sowie Informationen zur Veröffentlichung auf **F-Droid** findest du in der separaten Datei:

👉 **[Hier geht's zur Installationsanleitung (INSTALL.md)](./INSTALL.md)**

### Schnellstart per Terminal (Gradle CLI):
```bash
# Debug-APK kompilieren:
./gradlew assembleDebug

# Release-APK kompilieren (für R8/ProGuard optimiert):
./gradlew assembleRelease
```

---

## 🧪 Tests & Qualitätssicherung

Das Projekt enthält automatisierte Unit- und Szenario-Tests in **Java** zur Verifikation der Berechnungslogik, KI-JSON-Parser-Resilienz, Tagebuch-DAOs und Kryptografie:
```bash
./gradlew testDebugUnitTest
```

---

## ⚠️ Wichtiger medizinischer Hinweis

> **Haftungsausschluss**: Diese App dient als digitaler Rechenhelfer und Orientierungshilfe. Sie ersetzt **nicht** die ärztliche Beratung, Diagnostik oder Therapieempfehlung durch Fachpersonal (Diabetologe/Diabetologin). Alle Insulindosierungen und Faktoren sollten stets in Absprache mit medizinischem Fachpersonal festgelegt und eigenverantwortlich überprüft werden.
