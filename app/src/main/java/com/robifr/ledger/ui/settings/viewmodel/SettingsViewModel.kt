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

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.BuildConfig
import com.robifr.ledger.R
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.network.GithubReleaseModel
import com.robifr.ledger.network.NetworkState
import com.robifr.ledger.repository.SettingsRepository
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import com.robifr.ledger.util.VersionComparator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _settingsRepository: SettingsRepository
) : ViewModel() {
  private val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  private val _uiState: SafeMutableLiveData<SettingsState> =
      SafeMutableLiveData(
          SettingsState(
              languageUsed = _settingsRepository.languageUsed(),
              lastCheckedTimeForAppUpdate =
                  _settingsRepository.lastCheckedTimeForAppUpdate().atZone(ZoneId.systemDefault())))
  val uiState: SafeLiveData<SettingsState>
    get() = _uiState

  private val _appUpdateModel: SingleLiveEvent<GithubReleaseModel> = SingleLiveEvent()
  val appUpdateModel: LiveData<GithubReleaseModel>
    get() = _appUpdateModel

  fun onLanguageChanged(language: LanguageOption) {
    _uiState.setValue(_uiState.safeValue.copy(languageUsed = language))
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.languageTag))
    viewModelScope.launch(_dispatcher) { _settingsRepository.saveLanguageUsed(language) }
  }

  fun onCheckForAppUpdate(context: Context) {
    if (!NetworkState.isInternetAvailable(context)) {
      _snackbarState.setValue(
          SnackbarState(StringResource(R.string.settings_noInternetConnectionForAppUpdates)))
      return
    }
    viewModelScope.launch(_dispatcher) {
      _settingsRepository.obtainLatestAppRelease()?.let {
        if (VersionComparator.isNewVersionNewer(
            BuildConfig.VERSION_NAME, it.tagName.removePrefix("v"))) {
          _appUpdateModel.postValue(it)
        } else {
          _snackbarState.postValue(
              SnackbarState(StringResource(R.string.settings_noUpdatesWereAvailable)))
        }
        val now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
        _uiState.postValue(_uiState.safeValue.copy(lastCheckedTimeForAppUpdate = now))
        _settingsRepository.saveLastCheckedTimeForAppUpdate(now.toInstant())
      }
    }
  }

  fun onUpdateApp() {
    viewModelScope.launch(_dispatcher) {
      _appUpdateModel.value?.let {
        _settingsRepository.downloadAndInstallApp(it.browserDownloadUrl)
      }
    }
  }
}
