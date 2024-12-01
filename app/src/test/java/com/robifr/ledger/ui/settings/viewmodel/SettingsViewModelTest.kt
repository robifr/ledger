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
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.repository.SettingsRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class SettingsViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _settingsRepository: SettingsRepository
  private lateinit var _viewModel: SettingsViewModel

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _settingsRepository = mockk()

    every { _settingsRepository.languageUsed() } returns LanguageOption.ENGLISH_US
    _viewModel = SettingsViewModel(_dispatcher, _settingsRepository)
  }

  @Test
  fun `on language changed`() {
    mockkStatic(AppCompatDelegate::class)
    coEvery { _settingsRepository.saveLanguageUsed(any()) } returns true
    _viewModel.onLanguageChanged(LanguageOption.INDONESIA)
    assertAll(
        {
          assertEquals(
              LanguageOption.INDONESIA,
              _viewModel.uiState.safeValue.languageUsed,
              "Update current used language")
        },
        {
          assertDoesNotThrow("Immediately apply and save the current used language") {
            verify { AppCompatDelegate.setApplicationLocales(any()) }
            coVerify { _settingsRepository.saveLanguageUsed(any()) }
          }
        })
  }
}
