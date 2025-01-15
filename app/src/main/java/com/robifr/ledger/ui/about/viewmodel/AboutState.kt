/**
 * Copyright 2025 Robi
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

package com.robifr.ledger.ui.about.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.robifr.ledger.BuildConfig
import com.robifr.ledger.data.display.LanguageOption
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AboutState {
  fun appVersion(): String = BuildConfig.VERSION_NAME

  fun lastUpdatedDate(context: Context): String {
    val languageUsed: LanguageOption =
        LanguageOption.entries.find {
          it.languageTag == AppCompatDelegate.getApplicationLocales().toLanguageTags()
        } ?: LanguageOption.ENGLISH_US
    val lastUpdatedDate: ZonedDateTime =
        Instant.ofEpochMilli(
                context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime)
            .atZone(ZoneId.systemDefault())
    return lastUpdatedDate.format(
        DateTimeFormatter.ofPattern(context.getString(languageUsed.fullDateFormat)))
  }
}
