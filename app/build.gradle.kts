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
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.google.dagger.hilt.android)
  id(libs.plugins.jetbrains.kotlin.android.get().pluginId)
  id(libs.plugins.jetbrains.kotlin.parcelize.get().pluginId)
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
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.core)
  implementation(libs.androidx.navigation.fragment)
  implementation(libs.androidx.navigation.ui)
  implementation(libs.androidx.webkit)
  implementation(libs.google.android.material)

  implementation(libs.androidx.room.runtime)
  annotationProcessor(libs.androidx.room.compiler)

  implementation(libs.google.dagger.hilt.android)
  annotationProcessor(libs.google.dagger.hilt.android.compiler)

  implementation(libs.jetbrains.kotlin.stdlib)
  implementation(libs.jetbrains.kotlin.test)

  testImplementation(libs.junit.jupiter)

  debugImplementation(libs.squareup.leakcanary.android)
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
