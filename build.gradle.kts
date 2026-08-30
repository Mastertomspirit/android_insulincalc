// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

// Helper funktion

val overrideDependencies = Action<DependencyResolveDetails> {
  when (requested.group) {
    "io.netty" -> useVersion("4.1.137.Final")
    "org.bouncycastle" -> useVersion("1.84")
    "org.apache.httpcomponents" -> useVersion("4.5.14")
    "org.apache.commons" -> useVersion("3.18.0")
  }
}

// 1. For build plugins
  buildscript {
    configurations.all {
      resolutionStrategy.eachDependency {
        if (requested.group == "io.netty") {
          useVersion("4.1.137.Final")
        }
        if (requested.group == "org.bouncycastle") {
          useVersion("1.84")
        }
      }
    }
  }

// 2. App and test dependencies
  allprojects {
    configurations.all {
      resolutionStrategy.eachDependency {
        if (requested.group == "io.netty") {
          useVersion("4.1.137.Final")
        }
        if (requested.group == "org.bouncycastle") {
          useVersion("1.84")
        }
        if (requested.group == "org.apache.httpcomponents") {
          useVersion("4.5.14")
        }
        if (requested.group == "org.apache.commons") {
          useVersion("3.18.0")
        }
      }
    }
  }