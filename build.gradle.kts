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

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
  repositories {
    google()
    mavenCentral()
  }

  dependencies { classpath(libs.jetbrains.kotlin.gradle.plugin) }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.diffplug.spotless) apply false
  alias(libs.plugins.google.dagger.hilt.android) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.jetbrains.kotlin.jvm) apply false
}

allprojects {
  apply<SpotlessPlugin>()
  extensions.configure<SpotlessExtension> {
    java {
      target("**/*.java")
      targetExclude("${layout.buildDirectory}/**")
      googleJavaFormat()
      toggleOffOn()
      trimTrailingWhitespace()
      removeUnusedImports()
      licenseHeaderFile(file("${project.rootDir}/gradle/spotless/license_header.txt"))
    }

    kotlin {
      target("**/*.kt")
      targetExclude("${layout.buildDirectory}/**")
      ktfmt()
      toggleOffOn()
      trimTrailingWhitespace()
      licenseHeaderFile(file("${project.rootDir}/gradle/spotless/license_header.txt"))
    }

    kotlinGradle {
      target("**/*.gradle.kts")
      targetExclude("${layout.buildDirectory}/**")
      ktfmt()
      licenseHeaderFile(file("${project.rootDir}/gradle/spotless/license_header.txt"), "^\\w+")
    }

    javascript {
      target("**/assets/**/*.js")
      targetExclude("**/assets/libs/**/*.js", "${layout.buildDirectory}/**")
      prettier().config(mapOf("tabWidth" to 2, "useTabs" to false, "printWidth" to 100))
      licenseHeaderFile(
          file("${project.rootDir}/gradle/spotless/license_header.txt"), "\"use strict\"|^\\w+")
    }

    format("xml") {
      target("**/src/**/*.xml")
      targetExclude("${layout.buildDirectory}/**")

      // Set delimiter to match either xml tag or comment, to prevent comment being removed when
      // placed below xml header tag.
      // <xml .../><!-- Any comment here shouldn't be replaced with header license -->
      licenseHeaderFile(
          file("${project.rootDir}/gradle/spotless/license_header_xml.txt"), "^(<\\w+|<!--)")
    }

    format("html") {
      target("**/src/main/assets/**/*.html")
      targetExclude("${layout.buildDirectory}/**")
      prettier()
      licenseHeaderFile(
          file("${project.rootDir}/gradle/spotless/license_header_html.txt"), "<!DOCTYPE")
    }
  }

  gradle.projectsEvaluated() {
    tasks {
      withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
      }

      withType<Test>() {
        configureEach { useJUnitPlatform() }
        testLogging {
          showStandardStreams = true
          showExceptions = true
          showCauses = true
          showStackTraces = true
          exceptionFormat = TestExceptionFormat.FULL
        }
      }

      create<Delete>("clear") { delete = setOf(rootProject.layout.buildDirectory) }
    }
  }
}
