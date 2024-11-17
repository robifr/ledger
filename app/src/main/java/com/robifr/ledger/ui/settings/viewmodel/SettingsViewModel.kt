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

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.SettingsRepository
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
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
  private val _uiState: SafeMutableLiveData<SettingsState> =
      SafeMutableLiveData(SettingsState(_settingsRepository.languageUsed()))
  val uiState: SafeLiveData<SettingsState>
    get() = _uiState

  fun onLanguageChanged(language: LanguageOption) {
    _uiState.setValue(_uiState.safeValue.copy(languageUsed = language))
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.languageTag))
    viewModelScope.launch(_dispatcher) { _settingsRepository.saveLanguageUsed(language) }
  }
}
