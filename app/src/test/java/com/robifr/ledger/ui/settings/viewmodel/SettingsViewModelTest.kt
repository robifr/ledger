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
import androidx.lifecycle.Observer
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.network.GithubReleaseModel
import com.robifr.ledger.network.NetworkState
import com.robifr.ledger.repository.SettingsRepository
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.util.VersionComparator
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(
    InstantTaskExecutorExtension::class,
    MainCoroutineExtension::class,
    LifecycleOwnerExtension::class)
class SettingsViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _networkState: NetworkState
  private lateinit var _settingsRepository: SettingsRepository
  private lateinit var _viewModel: SettingsViewModel
  private lateinit var _snackbarStateObserver: Observer<SnackbarState>

  private val _githubRelease: GithubReleaseModel =
      GithubReleaseModel("v1.1.1", "2024-01-01T00:00:00Z", "https://example.com")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _networkState = mockk()
    _settingsRepository = mockk()
    _snackbarStateObserver = mockk(relaxed = true)

    every { _settingsRepository.languageUsed() } returns LanguageOption.ENGLISH_US
    every { _settingsRepository.lastCheckedTimeForAppUpdate() } returns Instant.now()
    _viewModel = SettingsViewModel(_dispatcher, _settingsRepository)
    _viewModel.snackbarState.observe(_lifecycleOwner, _snackbarStateObserver)
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

  @Test
  fun `on check for app update with internet not available`() {
    mockkObject(NetworkState)
    every { NetworkState.isInternetAvailable(any()) } returns false
    _viewModel.onCheckForAppUpdate(mockk())
    assertAll(
        {
          assertDoesNotThrow("Notify via snackbar if the internet is unavailable") {
            coVerify { _snackbarStateObserver.onChanged(any()) }
          }
        },
        {
          assertDoesNotThrow("Prevent fetching updates when the internet is unavailable") {
            coVerify(exactly = 0) { _settingsRepository.obtainLatestAppRelease() }
          }
        })
  }

  @Test
  fun `on check for app update with latest release not available`() {
    mockkObject(NetworkState)
    every { NetworkState.isInternetAvailable(any()) } returns true
    val oldUiState: SettingsState = _viewModel.uiState.safeValue

    coEvery { _settingsRepository.obtainLatestAppRelease() } returns null
    _viewModel.onCheckForAppUpdate(mockk())
    assertAll(
        {
          assertTrue(
              _viewModel.uiState.safeValue.lastCheckedTimeForAppUpdate.isEqual(
                  oldUiState.lastCheckedTimeForAppUpdate),
              "Don't update last checked time for app update")
        },
        {
          assertDoesNotThrow("Don't save the last checked time for app update") {
            coVerify(exactly = 0) { _settingsRepository.saveLastCheckedTimeForAppUpdate(any()) }
          }
        })
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on check for app update with internet and latest release available`(
      isNewVersionNewer: Boolean
  ) {
    mockkObject(NetworkState)
    every { NetworkState.isInternetAvailable(any()) } returns true
    val oldUiState: SettingsState = _viewModel.uiState.safeValue

    coEvery { _settingsRepository.obtainLatestAppRelease() } returns _githubRelease
    coEvery { _settingsRepository.saveLastCheckedTimeForAppUpdate(any()) } returns true
    mockkObject(VersionComparator)
    every { VersionComparator.isNewVersionNewer(any(), any()) } returns isNewVersionNewer
    _viewModel.onCheckForAppUpdate(mockk())
    assertAll(
        {
          if (isNewVersionNewer) {
            assertTrue(
                _viewModel.uiState.safeValue.lastCheckedTimeForAppUpdate.isAfter(
                    oldUiState.lastCheckedTimeForAppUpdate),
                "Update last checked time for app update")
          } else {
            assertDoesNotThrow("Notify via snackbar if no updates were available") {
              coVerify { _snackbarStateObserver.onChanged(any()) }
            }
          }
        },
        {
          assertDoesNotThrow("Immediately save the last checked time for app update") {
            coVerify { _settingsRepository.saveLastCheckedTimeForAppUpdate(any()) }
          }
        })
  }
}
