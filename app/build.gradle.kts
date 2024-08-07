/**
 * Copyright (c) 2024 Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
  kotlin("android")
  id("com.android.application")
  id("com.google.dagger.hilt.android")
  id("kotlin-parcelize")
}

android {
  compileSdk = 34
  namespace = "com.robifr.ledger"
  buildToolsVersion = "34.0.0"

  defaultConfig {
    applicationId = "com.robifr.ledger"
    minSdk = 30
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    multiDexEnabled = true

    javaCompileOptions {
      annotationProcessorOptions {
        arguments["room.schemaLocation"] = "$projectDir/schemas"
        arguments["room.incremental"] = "true"
        arguments["room.expandProjection"] = "true"
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      manifestPlaceholders["app_name"] = "@string/app_name"
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
    }

    debug {
      applicationIdSuffix = ".debug"
      isDebuggable = true
      manifestPlaceholders["app_name"] = "@string/app_name_debug"
    }
  }

  buildFeatures {
    viewBinding = true
    buildConfig = true
  }

  packaging { resources.excludes.add("META-INF/LICENSE") }

  lint {
    ignoreWarnings = true
    checkAllWarnings = false
    showAll = true
    abortOnError = false
    disable.add("MenuTitle")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin { jvmToolchain(17) }

  testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.webkit:webkit:1.11.0")

  implementation("androidx.room:room-runtime:2.6.1")
  annotationProcessor("androidx.room:room-compiler:2.6.1")

  implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
  implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

  implementation("com.google.android.material:material:1.12.0")

  implementation("com.google.dagger:hilt-android:2.51.1")
  annotationProcessor("com.google.dagger:hilt-android-compiler:2.51.1")

  implementation("androidx.core:core-ktx:1.13.1")
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.23")
  implementation("org.jetbrains.kotlin:kotlin-test")

  testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")

  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

  debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}

tasks.register<Exec>("npmClean") {
  val npm: String = if (Os.isFamily(Os.FAMILY_WINDOWS)) "npm.cmd" else "npm"
  val clean: String = if (Os.isFamily(Os.FAMILY_WINDOWS)) "clean:windows" else "clean:unix"

  commandLine(npm, "run", clean)
}

tasks.register<Exec>("npmBuild") {
  // Prevent rebuilding by checking if the file is up-to-date
  inputs.files(fileTree("src/main/assets/libs"))
  outputs.dir("build/js")

  val npm: String = if (Os.isFamily(Os.FAMILY_WINDOWS)) "npm.cmd" else "npm"

  doFirst { if (!File(projectDir, "node_modules").exists()) commandLine(npm, "install") }
  commandLine(npm, "run", "build")
}

tasks.named("clean").dependsOn("npmClean")

tasks.named("preBuild").dependsOn("npmBuild")
