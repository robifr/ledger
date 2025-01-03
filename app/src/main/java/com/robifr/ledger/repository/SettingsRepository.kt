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

import android.content.Context
import android.content.SharedPreferences
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.network.AppUpdater
import com.robifr.ledger.network.GithubReleaseModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class SettingsRepository(
    private val _sharedPreferences: SharedPreferences,
    private val _appUpdater: AppUpdater
) {
  private val _KEY_LANGUAGE_USED = "language_used"
  private val _KEY_LAST_CHECKED_TIME_FOR_APP_UPDATE = "last_checked_time_for_app_update"
  private val _KEY_CACHED_GITHUB_RELEASE = "cached_github_release"

  fun languageUsed(): LanguageOption {
    val languagePrefs: String? = _sharedPreferences.getString(_KEY_LANGUAGE_USED, null)
    return LanguageOption.entries.find { it.languageTag == languagePrefs }
        ?: LanguageOption.ENGLISH_US
  }

  suspend fun saveLanguageUsed(language: LanguageOption): Boolean =
      _sharedPreferences.edit().putString(_KEY_LANGUAGE_USED, language.languageTag).commit()

  fun lastCheckedTimeForAppUpdate(): Instant {
    val time: String? = _sharedPreferences.getString(_KEY_LAST_CHECKED_TIME_FOR_APP_UPDATE, null)
    return if (time != null) Instant.parse(time) else Instant.EPOCH
  }

  private suspend fun _saveLastCheckedTimeForAppUpdate(time: Instant): Boolean =
      _sharedPreferences
          .edit()
          .putString(_KEY_LAST_CHECKED_TIME_FOR_APP_UPDATE, time.toString())
          .commit()

  private fun _cachedGithubRelease(): GithubReleaseModel? =
      _sharedPreferences.getString(_KEY_CACHED_GITHUB_RELEASE, null)?.let {
        Json.decodeFromString(it)
      }

  private suspend fun _saveCachedGithubRelease(githubRelease: GithubReleaseModel): Boolean =
      _sharedPreferences
          .edit()
          .putString(
              _KEY_CACHED_GITHUB_RELEASE,
              Json.encodeToString(GithubReleaseModel.serializer(), githubRelease))
          .commit()

  suspend fun obtainLatestAppRelease(): GithubReleaseModel? =
      // GitHub's API rate limit for unauthenticated requests is 60 per hour.
      // The network check here is performed only once every 15 minutes.
      if (lastCheckedTimeForAppUpdate()
          .isBefore(ZonedDateTime.now(ZoneId.systemDefault()).minusMinutes(15).toInstant())) {
        _appUpdater.obtainLatestRelease()?.also {
          _saveLastCheckedTimeForAppUpdate(ZonedDateTime.now(ZoneId.systemDefault()).toInstant())
          _saveCachedGithubRelease(it)
        }
      } else {
        _cachedGithubRelease()
      }

  suspend fun downloadAndInstallApp(githubRelease: GithubReleaseModel) {
    _appUpdater.downloadAndInstallApp(githubRelease)
  }

  companion object {
    @Volatile private var _instance: SettingsRepository? = null

    @Synchronized
    fun instance(context: Context): SettingsRepository =
        _instance
            ?: SettingsRepository(
                    _settingsPreferences(context), AppUpdater(context, OkHttpClient()))
                .apply { _instance = this }

    private fun _settingsPreferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "com.robifr.ledger.settingsprefs", Context.MODE_PRIVATE)
  }
}
