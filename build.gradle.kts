// Top-level build file where you can add configuration options common to all subprojects / modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

// 1. For build plugins
  buildscript {
    configurations.all {
      resolutionStrategy.eachDependency {
        if (requested.group == "io.netty") {
          useVersion("4.1.137.Final")
        }
        if (requested.group == "org.bouncycastle") {
          useVersion("1.85")
        }
      }
    }
  }

// 2. App, lint, and test dependencies
allprojects {
  configurations.all {
    resolutionStrategy.eachDependency {
      if (requested.group == "io.netty") {
        useVersion("4.1.137.Final")
      }
      if (requested.group == "org.bouncycastle") {
        useVersion("1.85")
      }
      if (requested.group == "org.apache.httpcomponents") {
        if (requested.name == "httpclient" || requested.name == "httpmime") {
          useVersion("4.5.14")
        } else if (requested.name == "httpcore") {
          useVersion("4.4.16")
        }
      }
      if (requested.group == "org.apache.commons") {
        if (requested.name == "commons-lang3") {
          useVersion("3.20.0")
        } else if (requested.name == "commons-compress") {
          useVersion("1.28.0")
        }
      }
    }
  }
}

abstract class UpdateReadmeTask : DefaultTask() {
  @get:InputFile
  abstract val readmeFile: RegularFileProperty

  @get:InputFile
  abstract val libsFile: RegularFileProperty

  @get:InputFile
  abstract val wrapperFile: RegularFileProperty

  @get:InputFile
  abstract val appBuildFile: RegularFileProperty

  @TaskAction
  fun update() {
    val readme = readmeFile.get().asFile
    val libs = libsFile.get().asFile
    val wrapper = wrapperFile.get().asFile
    val appBuild = appBuildFile.get().asFile

    if (!readme.exists()) return

    var readmeText = readme.readText()

    // 1. Extract versionName & versionCode from app/build.gradle.kts
    val appBuildText = appBuild.readText()
    val versionNameMatch = Regex("""versionName\s*=\s*"([^"]+)"""").find(appBuildText)
    val versionCodeMatch = Regex("""versionCode\s*=\s*(\d+)""").find(appBuildText)

    if (versionNameMatch != null && versionCodeMatch != null) {
      val versionName = versionNameMatch.groupValues[1]
      val versionCode = versionCodeMatch.groupValues[1]
      readmeText = readmeText.replace(
        Regex("""\*\*Version\*\*: `[^`]+` \(VersionCode: `[^`]+`\)"""),
        "**Version**: `$versionName` (VersionCode: `$versionCode`)"
      )
    }

    // 2. Extract versions from libs.versions.toml & gradle-wrapper.properties
    if (libs.exists()) {
      val libsText = libs.readText()
      val agpMatch = Regex("""agp\s*=\s*"([^"]+)"""").find(libsText)
      val sqlcipherMatch = Regex("""sqlcipherAndroid\s*=\s*"([^"]+)"""").find(libsText)

      var gradleVersion = ""
      if (wrapper.exists()) {
        val wrapperText = wrapper.readText()
        val gradleMatch = Regex("""gradle-([\d.]+)-""").find(wrapperText)
        if (gradleMatch != null) {
          gradleVersion = gradleMatch.groupValues[1]
        }
      }

      if (agpMatch != null) {
        val agpVersion = agpMatch.groupValues[1]
        val replacement = if (gradleVersion.isNotEmpty()) {
          "**Android Gradle Plugin (AGP)**: `$agpVersion` (optimiert für Gradle $gradleVersion)"
        } else {
          "**Android Gradle Plugin (AGP)**: `$agpVersion`"
        }
        readmeText = readmeText.replace(
          Regex("""\*\*Android Gradle Plugin \(AGP\)\*\*: `[^`]+`(?:\s*\(optimiert für Gradle [^)]+\))?"""),
          replacement
        )
      }

      if (sqlcipherMatch != null) {
        val sqlcipherVersion = sqlcipherMatch.groupValues[1]
        readmeText = readmeText.replace(
          Regex("""sqlcipher-android:[^`)]+"""),
          "sqlcipher-android:$sqlcipherVersion"
        )
      }
    }

    readme.writeText(readmeText)
    logger.lifecycle("README.md versions successfully updated!")
  }
}

tasks.register<UpdateReadmeTask>("updateReadme") {
  group = "documentation"
  description = "Updates version numbers in README.md based on app/build.gradle.kts, libs.versions.toml, and gradle-wrapper.properties"
  readmeFile.set(layout.projectDirectory.file("README.md"))
  libsFile.set(layout.projectDirectory.file("gradle/libs.versions.toml"))
  wrapperFile.set(layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.properties"))
  appBuildFile.set(layout.projectDirectory.file("app/build.gradle.kts"))
}