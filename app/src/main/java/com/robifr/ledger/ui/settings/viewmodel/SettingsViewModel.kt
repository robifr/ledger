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
import com.robifr.ledger.network.NetworkState
import com.robifr.ledger.repository.SettingsRepository
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import com.robifr.ledger.ui.main.RequiredPermission
import com.robifr.ledger.util.VersionComparator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _settingsRepository: SettingsRepository,
    private val _permission: RequiredPermission
) : ViewModel() {
  private val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  private val _uiState: SafeMutableLiveData<SettingsState> =
      SafeMutableLiveData(
          SettingsState(
              languageUsed = _settingsRepository.languageUsed(),
              isLanguageDialogShown = false,
              lastCheckedTimeForAppUpdate =
                  _settingsRepository.lastCheckedTimeForAppUpdate().atZone(ZoneId.systemDefault()),
              githubRelease = null))
  val uiState: SafeLiveData<SettingsState>
    get() = _uiState

  private val _dialogState: SingleLiveEvent<SettingsDialogState> = SingleLiveEvent()
  val dialogState: SingleLiveEvent<SettingsDialogState>
    get() = _dialogState

  fun onLanguageChanged(language: LanguageOption) {
    _uiState.setValue(_uiState.safeValue.copy(languageUsed = language))
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.languageTag))
    viewModelScope.launch(_dispatcher) { _settingsRepository.saveLanguageUsed(language) }
  }

  fun onLanguageDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isLanguageDialogShown = true))
  }

  fun onLanguageDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isLanguageDialogShown = false))
  }

  fun onCheckForAppUpdate(context: Context, shouldSnackbarShown: Boolean = true) {
    if (!NetworkState.isInternetAvailable(context)) {
      if (shouldSnackbarShown) {
        _snackbarState.setValue(
            SnackbarState(StringResource(R.string.settings_noInternetConnectionForAppUpdates)))
      }
      return
    }
    viewModelScope.launch(_dispatcher) {
      _settingsRepository.obtainLatestAppRelease()?.let {
        if (VersionComparator.isNewVersionNewer(
            BuildConfig.VERSION_NAME, it.tagName.removePrefix("v"))) {
          _dialogState.postValue(UpdateAvailableDialogState(it))
        } else {
          if (shouldSnackbarShown) {
            _snackbarState.postValue(
                SnackbarState(StringResource(R.string.settings_noUpdatesWereAvailable)))
          }
        }
        _uiState.postValue(
            _uiState.safeValue.copy(
                lastCheckedTimeForAppUpdate =
                    _settingsRepository
                        .lastCheckedTimeForAppUpdate()
                        .atZone(ZoneId.systemDefault()),
                githubRelease = it))
      }
    }
  }

  fun onUpdateApp() {
    if (!_permission.isUnknownSourceInstallationGranted()) {
      _dialogState.setValue(UnknownSourceInstallationDialogState)
      return
    }
    viewModelScope.launch(_dispatcher) {
      _uiState.safeValue.githubRelease?.let { _settingsRepository.downloadAndInstallApp(it) }
    }
  }

  fun onActivityResultForUnknownSourceInstallation() {
    _uiState.safeValue.githubRelease?.let { _dialogState.postValue(UpdateAvailableDialogState(it)) }
  }
}
