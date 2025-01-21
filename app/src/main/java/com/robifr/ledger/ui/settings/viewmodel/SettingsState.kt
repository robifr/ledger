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

package com.robifr.ledger.ui.settings.viewmodel

import com.robifr.ledger.data.display.AppTheme
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.network.GithubReleaseModel
import java.time.ZoneId
import java.time.ZonedDateTime

data class SettingsState(
    val appTheme: AppTheme,
    val isAppThemeDialogShown: Boolean,
    val languageUsed: LanguageOption,
    val isLanguageDialogShown: Boolean,
    val lastCheckedTimeForAppUpdate: ZonedDateTime,
    val githubRelease: GithubReleaseModel?,
) {
  fun isLastCheckedTimeForAppUpdatePastMidNight(): Boolean {
    val now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    return lastCheckedTimeForAppUpdate.isBefore(now.toLocalDate().atStartOfDay(now.zone))
  }
}
