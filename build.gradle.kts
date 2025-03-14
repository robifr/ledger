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

buildscript {
  repositories {
    google()
    mavenCentral()
  }

  dependencies {
    classpath(libs.app.cash.licensee.gradle.plugin)
    classpath(libs.jetbrains.kotlin.gradle.plugin)
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.app.cash.licensee) apply false
  alias(libs.plugins.diffplug.spotless) apply false
  alias(libs.plugins.google.dagger.hilt.android) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
}

allprojects {
  apply<SpotlessPlugin>()
  extensions.configure<SpotlessExtension> {
    java {
      target("src/**/*.java")
      targetExclude("${layout.buildDirectory}/**")
      googleJavaFormat()
      toggleOffOn()
      trimTrailingWhitespace()
      removeUnusedImports()
      licenseHeaderFile(file("${rootDir}/gradle/spotless/license_header.txt"))
    }

    kotlin {
      target("src/**/*.kt")
      targetExclude("${layout.buildDirectory}/**")
      ktfmt()
      toggleOffOn()
      trimTrailingWhitespace()
      licenseHeaderFile(file("${rootDir}/gradle/spotless/license_header.txt"))
    }

    kotlinGradle {
      target("**/*.gradle.kts")
      targetExclude("${layout.buildDirectory}/**")
      ktfmt()
      licenseHeaderFile(file("${rootDir}/gradle/spotless/license_header.txt"), "^\\w+")
    }

    // TODO: Uncomment once the bug is fixed, https://github.com/diffplug/spotless/issues/2410.
    // javascript {
    //  target("**/assets/**/*.js")
    //  targetExclude("**/assets/libs/**/*.js", "${layout.buildDirectory}/**")
    //  prettier().config(mapOf("tabWidth" to 2, "useTabs" to false, "printWidth" to 100))
    //  licenseHeaderFile(
    //      file("${rootDir}/gradle/spotless/license_header.txt"), "\"use strict\"|^\\w+")
    // }

    python {
      target("scripts/**/*.py")
      licenseHeaderFile(file("${rootDir}/gradle/spotless/license_header_py.txt"), "^\\w+")
    }

    format("xml") {
      target("**/src/**/*.xml")
      targetExclude("${layout.buildDirectory}/**")
      // Set delimiter to match either xml tag or comment, to prevent comment being removed when
      // placed below xml header tag.
      // <xml .../>
      // <!-- Any comment here shouldn't be replaced with header license. -->
      licenseHeaderFile(file("${rootDir}/gradle/spotless/license_header_xml.txt"), "^(<\\w+|<!--)")
    }

    // TODO: Uncomment once the bug is fixed, https://github.com/diffplug/spotless/issues/2410.
    // format("html") {
    //  target("**/src/main/assets/**/*.html")
    //  targetExclude("${layout.buildDirectory}/**")
    //  prettier()
    //  licenseHeaderFile(file("${rootDir}/gradle/spotless/license_header_html.txt"), "<!DOCTYPE")
    // }
  }

  gradle.projectsEvaluated {
    tasks {
      withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
      }

      create<Delete>("clear") { delete = setOf(layout.buildDirectory) }
    }
  }
}
