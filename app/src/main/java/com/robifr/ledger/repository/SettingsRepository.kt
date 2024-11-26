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

package com.robifr.ledger.repository

import android.content.SharedPreferences
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class SettingsRepository
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _sharedPreferences: SharedPreferences
) {
  private val _KEY_LANGUAGE_USED = "language_used"

  fun languageUsed(): LanguageOption {
    val languagePrefs: String? =
        _sharedPreferences.getString(_KEY_LANGUAGE_USED, LanguageOption.ENGLISH_US.languageTag)
    return LanguageOption.entries.find { it.languageTag == languagePrefs }
        ?: LanguageOption.ENGLISH_US
  }

  suspend fun saveLanguageUsed(language: LanguageOption): Boolean =
      withContext(_dispatcher) {
        _sharedPreferences.edit().putString(_KEY_LANGUAGE_USED, language.languageTag).commit()
      }
}