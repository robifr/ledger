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

package io.github.robifr.ledger.ui.settings.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.robifr.ledger.BuildConfig
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.display.AppTheme
import io.github.robifr.ledger.data.display.LanguageOption
import io.github.robifr.ledger.di.IoDispatcher
import io.github.robifr.ledger.network.NetworkState
import io.github.robifr.ledger.repository.SettingsRepository
import io.github.robifr.ledger.ui.common.StringResource
import io.github.robifr.ledger.ui.common.StringResourceType
import io.github.robifr.ledger.ui.common.state.SafeLiveData
import io.github.robifr.ledger.ui.common.state.SafeMutableLiveData
import io.github.robifr.ledger.ui.common.state.SnackbarState
import io.github.robifr.ledger.ui.common.state.updateEvent
import io.github.robifr.ledger.ui.main.RequiredPermission
import io.github.robifr.ledger.util.VersionComparator
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
  private val _uiEvent: SafeMutableLiveData<SettingsEvent> = SafeMutableLiveData(SettingsEvent())
  val uiEvent: SafeLiveData<SettingsEvent>
    get() = _uiEvent

  private val _uiState: SafeMutableLiveData<SettingsState> =
      SafeMutableLiveData(
          SettingsState(
              appTheme = _settingsRepository.appTheme(),
              isAppThemeDialogShown = false,
              languageUsed = _settingsRepository.languageUsed(),
              isLanguageDialogShown = false,
              lastCheckedTimeForAppUpdate =
                  _settingsRepository.lastCheckedTimeForAppUpdate().atZone(ZoneId.systemDefault()),
              githubRelease = null))
  val uiState: SafeLiveData<SettingsState>
    get() = _uiState

  fun onAppThemeChanged(appTheme: AppTheme) {
    _uiState.setValue(_uiState.safeValue.copy(appTheme = appTheme))
    AppCompatDelegate.setDefaultNightMode(appTheme.defaultNightMode)
    viewModelScope.launch(_dispatcher) { _settingsRepository.saveAppTheme(appTheme) }
  }

  fun onAppThemeDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isAppThemeDialogShown = true))
  }

  fun onAppThemeDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isAppThemeDialogShown = false))
  }

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
    viewModelScope.launch(_dispatcher) {
      if (!NetworkState.isInternetAvailable(context)) {
        if (shouldSnackbarShown) {
          _onSnackbarShown(StringResource(R.string.settings_noInternetConnectionForAppUpdates))
        }
        return@launch
      }
      _settingsRepository.obtainLatestAppRelease()?.let {
        if (VersionComparator.isNewVersionNewer(
            BuildConfig.VERSION_NAME, it.tagName.removePrefix("v"))) {
          _onDialogShown(SettingsDialogState.UpdateAvailable(it))
        } else {
          if (shouldSnackbarShown) {
            _onSnackbarShown(StringResource(R.string.settings_noUpdatesWereAvailable))
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
      _onDialogShown(SettingsDialogState.UnknownSourceInstallation)
      return
    }
    viewModelScope.launch(_dispatcher) {
      _uiState.safeValue.githubRelease?.let { _settingsRepository.downloadAndInstallApp(it) }
    }
  }

  fun onActivityResultForUnknownSourceInstallation() {
    _uiState.safeValue.githubRelease?.let {
      _onDialogShown(SettingsDialogState.UpdateAvailable(it))
    }
  }

  private fun _onSnackbarShown(messageRes: StringResourceType) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SnackbarState(messageRes),
          onSet = { this?.copy(snackbar = it) },
          onReset = { this?.copy(snackbar = null) })
    }
  }

  private fun _onDialogShown(state: SettingsDialogState) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = state,
          onSet = { this?.copy(dialog = it) },
          onReset = { this?.copy(dialog = null) })
    }
  }
}
