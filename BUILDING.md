# 📱 Build-Anleitung

Diese Dokumentation beschreibt, wie das Projekt **Insulin-Rechner** kompiliert, getestet, auf Schwachstellen geprüft und als App installiert bzw. als Release erstellt wird.

---

## 📋 Inhaltsverzeichnis
1. [Voraussetzungen](#1-voraussetzungen)
2. [Build mit Android Studio (IDE)](#2-build-mit-android-studio-ide)
  - [Debug-Build starten](#debug-build-starten)
  - [Release-Build erstellen](#release-build-erstellen)
  - [Signatur-Schlüssel (Keystore) generieren](#signatur-schlüssel-keystore-generieren)
3. [Build über die Kommandozeile (Gradle CLI)](#3-build-über-die-kommandozeile-gradle-cli)
  - [Debug-Build (CLI)](#debug-build-cli)
  - [Release-Build (CLI)](#release-build-cli)
4. [Tests und Qualitätssicherung](#4-tests-und-qualitätssicherung)
5. [Sicherheitsprüfung und Abhängigkeiten](#5-sicherheitsprüfung-und-abhängigkeiten)
6. [Weitere Befehle und Flags](#6-weitere-befehle-und-flags)
7. [Fehlerbehebung](#7-fehlerbehebung)

---

## 1. Voraussetzungen

* **JDK 25** (Prüfung mit `java -version`). Für die Kommandozeile muss `JAVA_HOME` auf dieses JDK zeigen.
* **Android SDK** inkl. Build-Tools. Der Pfad steht in `local.properties` (`sdk.dir=...`) oder in der Umgebungsvariable `ANDROID_HOME`.
* **Gradle**, lokal installiert und über den Befehl `gradle` erreichbar.
* **Gerät oder Emulator** (mit aktiviertem USB-Debugging), nur für Installation und instrumentierte Tests.
* Optional: [Trivy](https://github.com/aquasecurity/trivy) (Schwachstellenscanner).

Alle Befehle in diesem Dokument verwenden `gradle`. Alternativ kann der Gradle Wrapper im Projektverzeichnis genutzt werden (`gradlew` unter Linux/macOS bzw. `gradlew.bat` unter Windows), die Befehle sind dann identisch.

---

## 2. Build mit Android Studio (IDE)

### Debug-Build starten
1. Das Projekt in Android Studio öffnen.
2. Warten, bis die Gradle-Synchronisierung abgeschlossen ist.
3. Im Reiter **Build Variants** (links unten) die Variante `debug` auswählen.
4. Ein verbundenes Android-Gerät oder einen Emulator im oberen Dropdown-Menü auswählen.
5. Auf den **Play-Button (Run ▶)** klicken oder `Shift + F10` drücken.

### Release-Build erstellen
1. In der Menüleiste **Build** → **Generate Signed Bundle / APK...** wählen.
2. Das gewünschte Format auswählen (Android App Bundle `.aab` für den Play Store oder APK `.apk` zur direkten Installation).
3. Den Keystore-Pfad und die Zugangsdaten angeben (bzw. einen neuen Keystore erstellen).
4. Als Build-Variante `release` auswählen und den Vorgang mit **Finish** abschließen.

### Signatur-Schlüssel (Keystore) generieren
1. Im Dialog *Generate Signed Bundle / APK* auf **Create new...** klicken.
2. Speicherort festlegen (z. B. `keystore.jks`) und Passwörter vergeben.
3. Zertifikatsdetails (Alias, Gültigkeit in Jahren) ausfüllen und bestätigen.

Alternativ über die Kommandozeile:
```bash
keytool -genkeypair -v -keystore keystore.jks -alias <alias> -keyalg RSA -keysize 4096 -validity 10000
```

---

## 3. Build über die Kommandozeile (Gradle CLI)

Die Befehle werden im Projektverzeichnis ausgeführt und laufen über den lokal installierten `gradle`-Prozess.

### Debug-Build
* **Kompilieren:**
  ```bash
  gradle assembleDebug
  ```
  *(Ausgabe unter: `app/build/outputs/apk/debug/app-debug.apk`)*

* **Installieren und Starten (auf verbundenem Gerät):**
  ```bash
  gradle installDebug
  ```

### Release-Build
* **Release-APK:** Kompiliert und optimiert (R8) nur die Release-Variante.
  ```bash
  gradle assembleRelease
  ```
  *(Ausgabe unter: `app/build/outputs/apk/release/app-release-unsigned.apk`)*

* **Release-Variante auf dem Gerät installieren:** Funktioniert nur mit signierter Release-APK.
  ```bash
  gradle installRelease
  ```
  
### Oder

* **Alles bauen und prüfen:** Baut Debug- und Release-APK und führt Lint sowie die Unit-Tests aller Varianten aus. Erzeugt kein App Bundle und führt keine instrumentierten Tests aus.
  ```bash
  gradle build
  ```
  
## 4. Tests und Qualitätssicherung

* **Unit-Tests (JVM):** Berechnungslogik, KI-JSON-Parser und Kryptografie, ohne Gerät.
  ```bash
  gradle testDebugUnitTest
  ```
  *(Bericht unter: `app/build/reports/tests/testDebugUnitTest/index.html`)*

* **Instrumentierte Tests (Gerät oder Emulator):** Tagebuch-DAOs, Datenbank-Migrationen, UI und Android-Komponenten.
  ```bash
  gradle connectedDebugAndroidTest
  ```
  *(Bericht unter: `app/build/reports/androidTests/connected/`)*

* **Alle instrumentierten Tests auf allen verbundenen Geräten:**
  ```bash
  gradle connectedCheck
  ```

* **Statische Code-Analyse (Lint):** Prüft den Quellcode auf potenzielle Fehler, Performance-Probleme und Warnungen.
  ```bash
  gradle lint
  ```
  *(Berichte unter: `app/build/reports/`, z. B. `lint-results-debug.html`)*

* **Lint und Unit-Tests aller Varianten:**
  ```bash
  gradle check
  ```

* **Vollständige Prüfung vor einem Release:** Sauberer Build mit Lint, Unit-Tests und instrumentierten Tests.
  ```bash
  gradle clean build connectedCheck
  ```

* **Einzelne Unit-Testklasse oder -methode:**
  ```bash
  gradle testDebugUnitTest --tests "<package>.<ClassName>"
  gradle testDebugUnitTest --tests "<package>.<ClassName>.<methodName>"
  ```

* **Einzelne instrumentierte Testklasse:**
  ```bash
  gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<package>.<ClassName>
  ```

---

## 5. Sicherheitsprüfung und Abhängigkeiten

Das Projekt nutzt moderne Android- und Gradle-Standards zur Überprüfung von Abhängigkeiten und Sicherheitslücken.

### Lokale Schwachstellenprüfung (Sonatype Scan)
Das Projekt ist mit dem **Sonatype Scan Plugin** ausgestattet, das über den Gradle Version Catalog (`libs.versions.toml`) deklariert ist.

* **Lokalen Scan ausführen:**
  ```bash
  gradle ossIndexAudit --info
  ```
  (Hinweis: Für den API-Zugriff auf den Sonatype OSS Index wird ein kostenloser Account auf [ossindex.sonatype.org](https://ossindex.sonatype.org) benötigt.) \
  Die Zugangsdaten werden sicher und lokal in der Datei `local.properties` eingetragen:
  ```properties
  SONATYPE_USERNAME=your.email@example.com
  SONATYPE_API_KEY=your_api_token
  ```

### Abhängigkeiten untersuchen
* **Transitive Abhängigkeiten untersuchen:**
  ```bash
  gradle app:dependencyInsight --configuration debugRuntimeClasspath --dependency <name>
  ```
* **Vollständigen Abhängigkeitsbaum ausgeben:**
  ```bash
  gradle app:dependencies
  ```

### Lokale Schwachstellenprüfung (Trivy CLI)
[Trivy](https://github.com/aquasecurity/trivy) ist ein performanter, Open-Source-Sicherheitsscanner von Aqua Security, der den lokalen Abhängigkeitsbaum (gestützt auf die GitHub Advisory Database / GHSA) scannt.

* **Installation von Trivy:**
  * **Windows (via Winget):**
    ```powershell
    winget install AquaSecurity.Trivy --scope user
    ```
  * **Linux (Ubuntu / Debian):**
    ```bash
    sudo apt-get install wget apt-transport-https gnupg lsb-release
    wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/trivy.gpg > /dev/null
    echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee /etc/apt/sources.list.d/trivy.list
    sudo apt-get update
    sudo apt-get install trivy
    ```
  * **Alternativ (macOS / Docker / weitere Plattformen):** Siehe offizielle [Trivy Installationsanleitung](https://aquasecurity.github.io/trivy/latest/getting-started/installation/).

* **Grundlegender Scan mit Lockfile:**
  ```bash
  trivy fs --dependency-tree app/gradle.lockfile
  ```
* **Nur kritische und hohe Schwachstellen anzeigen:**
  ```bash
  trivy fs --dependency-tree app/gradle.lockfile --severity HIGH,CRITICAL
  ```
* **Wichtige Trivy-Flags:**
  * `--scanners vuln`: Aktiviert explizit den Schwachstellen-Scanner (Standard).
  * `--format json`: Gibt das Ergebnis im JSON-Format aus (ideal für automatisierte Auswertungen).
  * `--output report.json`: Speichert den Scan-Bericht direkt in eine Datei.
  * `--ignore-unfixed`: Ignoriert Schwachstellen, für die vom Maintainer noch kein Patch veröffentlicht wurde.

### Gradle Dependency Locking (Reproduzierbare Builds)
Das Projekt verwendet Gradle Dependency Locking (`dependencyLocking { lockAllConfigurations() }`), um sicherzustellen, dass exakt dieselben Versionen aller direkten und transitiven Bibliotheken verwendet werden. Weichen die aufgelösten Versionen vom Lockfile ab, schlägt der Build fehl.

* **Lockfiles erstellen oder aktualisieren:**
  Bei Hinzufügung neuer Bibliotheken oder Versionsupdates müssen die Lockfiles aktualisiert werden:
  ```bash
  gradle dependencies --write-locks
  ```
  *(Dadurch wird die Datei `app/gradle.lockfile` aktualisiert und die fixen Versionen werden im Git-Repository versioniert).*
* **Nur eine einzelne Abhängigkeit aktualisieren:**
  ```bash
  gradle dependencies --update-locks <group>:<module>
  ```
* **Zweck:** Verhindert unerwartete Änderungen an transitiven Abhängigkeiten im Hintergrund und garantiert reproduzierbare Builds auf jedem System.

### Kryptografische Verifikation (Gradle Dependency Verification)
Gradle unterstützt die native Verifikation von Checksummen (SHA-256) aller heruntergeladenen Abhängigkeiten:
```bash
gradle --write-verification-metadata sha256 build
```
*(Die Prüfsummen landen in `gradle/verification-metadata.xml` und werden versioniert. Neue Einträge vor dem Commit prüfen.)*

---

## 6. Weitere Befehle und Flags

* **Versionsangaben in der README aktualisieren:** Liest App-Version, AGP, Gradle Wrapper, SQLCipher u. a. aus den Konfigurationsdateien.
  ```bash
  gradle updateReadme
  ```
* **Alle verfügbaren Tasks anzeigen:**
  ```bash
  gradle tasks --all
  ```
* **Nützliche Flags** (an jeden Befehl anhängbar):
  * `--info` / `--debug`: Ausführlichere Ausgabe.
  * `--refresh-dependencies`: Löst Abhängigkeiten neu auf und lädt sie erneut herunter.
  * `--rerun`: Führt den angegebenen Task erneut aus, auch wenn er aktuell ist (`--rerun-tasks` für alle Tasks).
  * `--continue`: Führt nach einem Fehler weitere Tasks aus und meldet alle Fehler am Ende.
  * `-x <task>`: Schließt einen Task aus, z. B. `gradle build -x lint`.
  * `--dry-run`: Zeigt die auszuführenden Tasks, ohne sie auszuführen.

---

## 7. Fehlerbehebung

* **Gerät wird nicht erkannt / Offline:**
  Mit `adb devices` prüfen. USB-Debugging in den Entwickleroptionen des Android-Geräts deaktivieren und erneut aktivieren; Verbindung autorisieren. Bei Bedarf `adb kill-server && adb start-server`.
* **`gradle` wird nicht gefunden:**
  Gradle installieren und zum `PATH` hinzufügen oder den Gradle Wrapper (`gradlew`) im Stammverzeichnis des Projekts verwenden.
* **Falsche JDK-Version:**
  `gradle --version` zeigt das verwendete JDK. `JAVA_HOME` muss auf JDK 25 zeigen.
* **Release-APK lässt sich nicht installieren:**
  `app-release-unsigned.apk` ist unsigniert und muss zuerst signiert werden (siehe [Release-APK signieren und installieren](#release-apk-signieren-und-installieren)).
* **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`:**
  Die installierte Version hat eine andere Signatur (z. B. Debug statt Release). Zuerst deinstallieren: `adb uninstall <applicationId>`.
* **Build schlägt wegen Lockfile fehl:**
  Abhängigkeiten wurden geändert, das Lockfile nicht. Mit `gradle dependencies --write-locks` aktualisieren.
* **Clean Build bei Cache-Problemen:**
  ```bash
  gradle clean assembleDebug
  ```
  Hängende Daemons beendet `gradle --stop`.