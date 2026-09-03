// Top-level build file where you can add configuration options common to all sub-projects/modules.
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
          useVersion("4.5.15")
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