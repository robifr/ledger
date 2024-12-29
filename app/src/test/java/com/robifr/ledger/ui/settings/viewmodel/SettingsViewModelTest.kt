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
import com.robifr.ledger.ui.main.RequiredPermission
import com.robifr.ledger.util.VersionComparator
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
  private lateinit var _permission: RequiredPermission
  private lateinit var _viewModel: SettingsViewModel
  private lateinit var _snackbarStateObserver: Observer<SnackbarState>
  private lateinit var _dialogStateObserver: Observer<SettingsDialogState>

  private val _githubRelease: GithubReleaseModel =
      GithubReleaseModel("v1.1.1", "2024-01-01T00:00:00Z", "https://example.com")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _networkState = mockk()
    _settingsRepository = mockk()
    _permission = mockk()
    _snackbarStateObserver = mockk(relaxed = true)
    _dialogStateObserver = mockk(relaxed = true)

    every { _settingsRepository.languageUsed() } returns LanguageOption.ENGLISH_US
    every { _settingsRepository.lastCheckedTimeForAppUpdate() } returns Instant.now()
    _viewModel = SettingsViewModel(_dispatcher, _settingsRepository, _permission)
    _viewModel.snackbarState.observe(_lifecycleOwner, _snackbarStateObserver)
    _viewModel.dialogState.observe(_lifecycleOwner, _dialogStateObserver)
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
          assertDoesNotThrow("Notify unavailable internet via snackbar") {
            verify { _snackbarStateObserver.onChanged(any()) }
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

  private fun `_on check for app update with internet and latest release available cases`():
      Array<Array<Any>> =
      arrayOf(
          arrayOf(true, 1, 0, true, _githubRelease, 1),
          arrayOf(false, 0, 1, true, _githubRelease, 1))

  @ParameterizedTest
  @MethodSource("_on check for app update with internet and latest release available cases")
  fun `on check for app update with internet and latest release available`(
      isNewVersionNewer: Boolean,
      updateAvailableDialogNotifyCount: Int,
      noUpdateAvailableSnackbarNotifyCount: Int,
      isLastCheckedTimeUpdated: Boolean,
      githubRelease: GithubReleaseModel,
      saveLastCheckedTimeCallCount: Int
  ) {
    mockkObject(NetworkState)
    every { NetworkState.isInternetAvailable(any()) } returns true
    coEvery { _settingsRepository.obtainLatestAppRelease() } returns githubRelease
    val oldUiState: SettingsState = _viewModel.uiState.safeValue

    coEvery { _settingsRepository.saveLastCheckedTimeForAppUpdate(any()) } returns true
    mockkObject(VersionComparator)
    every { VersionComparator.isNewVersionNewer(any(), any()) } returns isNewVersionNewer
    _viewModel.onCheckForAppUpdate(mockk())
    assertAll(
        {
          assertDoesNotThrow("Show available app update via alert dialog") {
            verify(exactly = updateAvailableDialogNotifyCount) {
              _dialogStateObserver.onChanged(eq(UpdateAvailableDialogState(githubRelease)))
            }
          }
        },
        {
          assertDoesNotThrow("Notify unavailable app update via snackbar") {
            verify(exactly = noUpdateAvailableSnackbarNotifyCount) {
              _snackbarStateObserver.onChanged(any())
            }
          }
        },
        {
          assertEquals(
              isLastCheckedTimeUpdated,
              _viewModel.uiState.safeValue.lastCheckedTimeForAppUpdate.isAfter(
                  oldUiState.lastCheckedTimeForAppUpdate),
              "Update last checked time for app update whenever ")
        },
        {
          assertEquals(
              githubRelease,
              _viewModel.uiState.safeValue.githubRelease,
              "Update GitHub release model with the fetched data")
        },
        {
          assertDoesNotThrow("Immediately save the last checked time for app update") {
            coVerify(exactly = saveLastCheckedTimeCallCount) {
              _settingsRepository.saveLastCheckedTimeForAppUpdate(any())
            }
          }
        })
  }

  private fun `_on update app with fetched data cases`(): Array<Array<Any>> =
      arrayOf(arrayOf(true, 0, 1), arrayOf(false, 1, 0))

  @ParameterizedTest
  @MethodSource("_on update app with fetched data cases")
  fun `on update app with fetched data`(
      isUnknownSourceInstallationPermissionGranted: Boolean,
      unknownSourceInstallationDialogNotifyCount: Int,
      downloadAppCallCount: Int
  ) {
    mockkObject(NetworkState)
    every { NetworkState.isInternetAvailable(any()) } returns true
    mockkObject(VersionComparator)
    every { VersionComparator.isNewVersionNewer(any(), any()) } returns true
    coEvery { _settingsRepository.obtainLatestAppRelease() } returns _githubRelease
    coEvery { _settingsRepository.saveLastCheckedTimeForAppUpdate(any()) } returns true
    _viewModel.onCheckForAppUpdate(mockk())

    every { _permission.isUnknownSourceInstallationGranted() } returns
        isUnknownSourceInstallationPermissionGranted
    coEvery { _settingsRepository.downloadAndInstallApp(any()) } just Runs
    _viewModel.onUpdateApp()
    assertAll(
        {
          assertDoesNotThrow("Show unknown source installation permission dialog if not granted") {
            verify(exactly = unknownSourceInstallationDialogNotifyCount) {
              _dialogStateObserver.onChanged(eq(UnknownSourceInstallationDialogState))
            }
          }
        },
        {
          assertDoesNotThrow("Update app using the fetched GitHub release data") {
            coVerify(exactly = downloadAppCallCount) {
              _settingsRepository.downloadAndInstallApp(eq(_githubRelease))
            }
          }
        })
  }

  @Test
  fun `on activity result for unknown source installation with fetched data`() {
    mockkObject(NetworkState)
    every { NetworkState.isInternetAvailable(any()) } returns true
    mockkObject(VersionComparator)
    every { VersionComparator.isNewVersionNewer(any(), any()) } returns true
    coEvery { _settingsRepository.obtainLatestAppRelease() } returns _githubRelease
    coEvery { _settingsRepository.saveLastCheckedTimeForAppUpdate(any()) } returns true
    _viewModel.onCheckForAppUpdate(mockk())

    _viewModel.onActivityResultForUnknownSourceInstallation()
    assertDoesNotThrow("Re-show app update dialog after returning from settings activity") {
      verify(exactly = 2) {
        _dialogStateObserver.onChanged(eq(UpdateAvailableDialogState(_githubRelease)))
      }
    }
  }
}
