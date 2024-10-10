/**
 * Copyright 2024 Robi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  id(libs.plugins.android.application.get().pluginId)
  id(libs.plugins.google.dagger.hilt.android.get().pluginId)
  id(libs.plugins.google.devtools.ksp.get().pluginId)
  id(libs.plugins.jetbrains.kotlin.android.get().pluginId)
  id(libs.plugins.jetbrains.kotlin.plugin.parcelize.get().pluginId)
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
    versionName = "1.1.0"
    multiDexEnabled = true

    ksp {
      arg("room.schemaLocation", "${projectDir}/schemas")
      arg("room.incremental", "true")
      arg("room.expandProjection", "true")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      manifestPlaceholders["app_name"] = "@string/appName"
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
    }

    debug {
      applicationIdSuffix = ".debug"
      isDebuggable = true
      manifestPlaceholders["app_name"] = "@string/appName_debug"
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

  // TODO: Remove after Kotlin migration.
  kotlinOptions { freeCompilerArgs = listOf("-Xjvm-default=all") }

  kotlin { jvmToolchain(17) }

  testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.core)
  implementation(libs.androidx.navigation.fragment)
  implementation(libs.androidx.navigation.ui)
  implementation(libs.androidx.webkit)
  implementation(libs.google.android.material)

  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)

  implementation(libs.google.dagger.hilt.android)
  ksp(libs.google.dagger.hilt.android.compiler)

  implementation(libs.jetbrains.kotlin.stdlib)
  implementation(libs.jetbrains.kotlinx.coroutines.core)

  testImplementation(libs.androidx.arch.core.testing)
  testImplementation(libs.io.mockk)
  testImplementation(libs.jetbrains.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.mockito.core)

  debugImplementation(libs.squareup.leakcanary.android)
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    showStandardStreams = true
    showExceptions = true
    showCauses = true
    showStackTraces = true
    exceptionFormat = TestExceptionFormat.FULL
  }
}

tasks.register<Exec>("downloadD3Js") {
  val version: String = libs.versions.d3.get()
  val url: String = "https://cdn.jsdelivr.net/npm/d3@${version}"
  val dir: File = file("src/main/assets/libs/").apply { mkdirs() }
  val file: File = File(dir, "d3.js")
  // Prevent re-downloading when rebuilding the project.
  onlyIf { !file.exists() || !file.readText().contains(version) }
  commandLine("curl", url, "-o", file.absolutePath)
}

tasks.named("preBuild").dependsOn("downloadD3Js")
