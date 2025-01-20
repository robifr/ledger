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

package com.robifr.ledger.preferences

import android.content.Context
import android.content.SharedPreferences
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.network.GithubReleaseModel
import java.time.Instant

class SettingsPreferences(val _context: Context) {
  val sharedPreferences: SharedPreferences =
      _context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

  companion object {
    const val FILE_NAME: String = "com.robifr.ledger.settingsprefs"
    /** Key for the IETF BCP 47 language tag from [LanguageOption.languageTag]. */
    const val KEY_LANGUAGE_USED: String = "language_used"
    /** Key for the last checked time in [Instant] for app update. */
    const val KEY_LAST_CHECKED_TIME_FOR_APP_UPDATE: String = "last_checked_time_for_app_update"
    /** Key for the cached [GithubReleaseModel] for app update. */
    const val KEY_CACHED_GITHUB_RELEASE: String = "cached_github_release"
  }
}
