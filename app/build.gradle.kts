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

import java.util.Properties
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  id(libs.plugins.android.application.get().pluginId)
  id(libs.plugins.app.cash.licensee.get().pluginId)
  id(libs.plugins.google.dagger.hilt.android.get().pluginId)
  id(libs.plugins.google.devtools.ksp.get().pluginId)
  id(libs.plugins.jetbrains.kotlin.android.get().pluginId)
  id(libs.plugins.jetbrains.kotlin.plugin.parcelize.get().pluginId)
  id(libs.plugins.jetbrains.kotlin.plugin.serialization.get().pluginId)
  id(libs.plugins.jetbrains.kotlinx.kover.get().pluginId)
}

android {
  compileSdk = 35
  namespace = "com.robifr.ledger"
  buildToolsVersion = "34.0.0"

  defaultConfig {
    applicationId = "com.robifr.ledger"
    minSdk = 30
    targetSdk = 35
    versionCode = 1
    versionName = "2.0.2"
    multiDexEnabled = true
    setProperty("archivesBaseName", "ledger-v${versionName}")

    ksp {
      arg("room.schemaLocation", "${projectDir}/schemas")
      arg("room.incremental", "true")
      arg("room.expandProjection", "true")
    }
  }

  signingConfigs {
    create("release") {
      val file: File = rootProject.file("keystore.properties")
      if (file.exists()) {
        val properties: Properties = Properties().apply { load(file.inputStream()) }
        keyAlias = properties.getProperty("key.alias")
        keyPassword = properties.getProperty("key.password")
        storeFile = file("${rootDir}/${properties.getProperty("key.storeFile")}")
        storePassword = properties.getProperty("key.storePassword")
        storeType = "PKCS12"
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      manifestPlaceholders["appLabel"] = "@string/appName"
      manifestPlaceholders["activityLauncherName"] = ".ui.main.MainActivity"
      signingConfig = signingConfigs.getByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
      buildConfigField("String", "DATABASE_FILE_NAME", "\"data.db\"")
    }

    debug {
      applicationIdSuffix = ".debug"
      isDebuggable = true
      manifestPlaceholders["appLabel"] = "@string/appName_debug"
      manifestPlaceholders["activityLauncherName"] = ".ui.main.MainActivity"
      buildConfigField("String", "DATABASE_FILE_NAME", "\"data.db\"")
    }

    create("qa") {
      initWith(getByName("release"))
      applicationIdSuffix = ".qa"
      manifestPlaceholders["appLabel"] = "@string/appName_qa"
      manifestPlaceholders["activityLauncherName"] = ".ui.main.QaMainActivity"
      buildConfigField("String", "DATABASE_FILE_NAME", "\"qa/data.db\"")
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
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.core)
  implementation(libs.androidx.navigation.fragment)
  implementation(libs.androidx.navigation.ui)
  implementation(libs.androidx.viewpager2)
  implementation(libs.androidx.webkit)
  implementation(libs.google.android.material)
  implementation(libs.squareup.okhttp3.okhttp)

  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  implementation(libs.google.dagger.hilt.android)
  ksp(libs.google.dagger.hilt.android.compiler)

  implementation(libs.jetbrains.kotlin.stdlib)
  implementation(libs.jetbrains.kotlinx.coroutines.core)
  implementation(libs.jetbrains.kotlinx.serialization.json)

  testImplementation(libs.androidx.arch.core.testing)
  testImplementation(libs.jetbrains.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.mockk)

  debugImplementation(libs.squareup.leakcanary.android)

  "qaImplementation"(libs.datafaker)
}

licensee { allow("Apache-2.0") }

kover {
  currentProject.createVariant("main") { add("debug") }

  reports {
    filters.excludes {
      packages("**.databinding", "dagger.hilt.*", "hilt_aggregated_deps")
      classes("**.R.class", "**.R\$*.class", "**.BuildConfig", "**.*_*")
    }
  }
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

tasks.named("preBuild") { dependsOn("downloadD3Js") }

tasks.register<Exec>("setupPythonEnvironment") {
  val python: String =
      if (System.getProperty("os.name").lowercase().contains("windows")) "python" else "python3"
  commandLine(python, "-m", "venv", "${rootDir}/venv")
}

tasks.register<Exec>("updateThirdPartyLicenses") {
  val python: String =
      if (System.getProperty("os.name").lowercase().contains("windows")) "venv\\Scripts\\python"
      else "./venv/bin/python"
  commandLine(
      python,
      "./../scripts/format_licensee.py",
      "--input-file",
      "./build/reports/licensee/androidRelease/artifacts.json",
      "--output-file",
      "./src/main/res/raw/third_party_licenses.txt")
}

tasks.named("updateThirdPartyLicenses") { dependsOn("setupPythonEnvironment") }

tasks.named("licensee") { finalizedBy("updateThirdPartyLicenses") }
