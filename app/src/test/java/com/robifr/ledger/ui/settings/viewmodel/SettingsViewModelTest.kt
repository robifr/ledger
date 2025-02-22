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
import com.robifr.ledger.data.display.AppTheme
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.network.GithubReleaseModel
import com.robifr.ledger.network.NetworkState
import com.robifr.ledger.repository.SettingsRepository
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
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

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
  private lateinit var _uiEventObserver: Observer<SettingsEvent>

  private val _githubRelease: GithubReleaseModel =
      GithubReleaseModel(
          tagName = "v1.1.1",
          size = 100,
          publishedAt = "2024-01-01T00:00:00Z",
          browserDownloadUrl = "https://example.com")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _networkState = mockk()
    _settingsRepository = mockk()
    _permission = mockk()
    _uiEventObserver = mockk(relaxed = true)

    every { _settingsRepository.appTheme() } returns AppTheme.FOLLOW_SYSTEM
    every { _settingsRepository.languageUsed() } returns LanguageOption.ENGLISH_US
    every { _settingsRepository.lastCheckedTimeForAppUpdate() } returns Instant.now()
    _viewModel = SettingsViewModel(_dispatcher, _settingsRepository, _permission)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on app theme changed`() {
    mockkStatic(AppCompatDelegate::class)
    coEvery { _settingsRepository.saveAppTheme(any()) } returns true
    _viewModel.onAppThemeChanged(AppTheme.DARK)
    assertAll(
        {
          assertEquals(
              AppTheme.DARK, _viewModel.uiState.safeValue.appTheme, "Update current app theme")
        },
        {
          assertDoesNotThrow("Immediately apply and save the current app theme") {
            verify { AppCompatDelegate.setDefaultNightMode(any()) }
            coVerify { _settingsRepository.saveAppTheme(any()) }
          }
        })
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on app theme dialog shown`(isShown: Boolean) {
    mockkStatic(AppCompatDelegate::class)
    coEvery { _settingsRepository.saveAppTheme(any()) } returns true
    _viewModel.onAppThemeChanged(AppTheme.DARK)
    coEvery { _settingsRepository.saveLanguageUsed(any()) } returns true
    _viewModel.onLanguageChanged(LanguageOption.INDONESIA)

    if (isShown) _viewModel.onAppThemeDialogShown() else _viewModel.onAppThemeDialogClosed()
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            appTheme = AppTheme.DARK,
            isAppThemeDialogShown = isShown,
            languageUsed = LanguageOption.INDONESIA),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
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

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on language dialog shown`(isShown: Boolean) {
    mockkStatic(AppCompatDelegate::class)
    coEvery { _settingsRepository.saveAppTheme(any()) } returns true
    _viewModel.onAppThemeChanged(AppTheme.DARK)
    coEvery { _settingsRepository.saveLanguageUsed(any()) } returns true
    _viewModel.onLanguageChanged(LanguageOption.INDONESIA)

    if (isShown) _viewModel.onLanguageDialogShown() else _viewModel.onLanguageDialogClosed()
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            appTheme = AppTheme.DARK,
            languageUsed = LanguageOption.INDONESIA,
            isLanguageDialogShown = isShown),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on check for app update with internet not available`(shouldSnackbarShown: Boolean) {
    mockkObject(NetworkState)
    coEvery { NetworkState.isInternetAvailable(any()) } returns false
    _viewModel.onCheckForAppUpdate(mockk(), shouldSnackbarShown)
    assertAll(
        {
          assertThat(
              "Notify unavailable internet via snackbar",
              _viewModel.uiEvent.safeValue.snackbar?.data,
              if (shouldSnackbarShown) notNullValue() else nullValue(),
          )
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
    coEvery { NetworkState.isInternetAvailable(any()) } returns true
    val oldUiState: SettingsState = _viewModel.uiState.safeValue

    coEvery { _settingsRepository.obtainLatestAppRelease() } returns null
    _viewModel.onCheckForAppUpdate(mockk())
    assertEquals(
        oldUiState,
        _viewModel.uiState.safeValue,
        "Preserve all values when the latest release is null")
  }

  private fun `_on check for app update with internet and latest release available cases`():
      Array<Array<Any>> =
      arrayOf(
          arrayOf(true, true, true, false, Instant.now(), _githubRelease),
          arrayOf(false, false, true, true, Instant.now(), _githubRelease),
          arrayOf(false, false, false, false, Instant.now(), _githubRelease))

  @ParameterizedTest
  @MethodSource("_on check for app update with internet and latest release available cases")
  fun `on check for app update with internet and latest release available`(
      isNewVersionNewer: Boolean,
      isUpdateAvailableDialogShown: Boolean,
      shouldSnackbarShown: Boolean,
      isNoUpdateAvailableSnackbarShown: Boolean,
      lastCheckedTime: Instant,
      githubRelease: GithubReleaseModel
  ) {
    mockkObject(NetworkState)
    coEvery { NetworkState.isInternetAvailable(any()) } returns true
    coEvery { _settingsRepository.obtainLatestAppRelease() } returns githubRelease
    val oldUiState: SettingsState = _viewModel.uiState.safeValue

    mockkObject(VersionComparator)
    every { VersionComparator.isNewVersionNewer(any(), any()) } returns isNewVersionNewer
    coEvery { _settingsRepository.lastCheckedTimeForAppUpdate() } returns lastCheckedTime
    _viewModel.onCheckForAppUpdate(mockk(), shouldSnackbarShown)
    assertAll(
        {
          assertEquals(
              if (isUpdateAvailableDialogShown) SettingsDialogState.UpdateAvailable(githubRelease)
              else null,
              _viewModel.uiEvent.safeValue.dialog?.data,
              "Show available app update via alert dialog")
        },
        {
          assertThat(
              "Notify unavailable app update via snackbar",
              _viewModel.uiEvent.safeValue.snackbar?.data,
              if (isNoUpdateAvailableSnackbarShown) notNullValue() else nullValue())
        },
        {
          assertEquals(
              oldUiState.copy(
                  lastCheckedTimeForAppUpdate = lastCheckedTime.atZone(ZoneId.systemDefault()),
                  githubRelease = githubRelease),
              _viewModel.uiState.safeValue,
              "Update last checked time and GitHub release model")
        })
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on update app with fetched data`(isInstallationPermissionGranted: Boolean) {
    mockkObject(NetworkState)
    coEvery { NetworkState.isInternetAvailable(any()) } returns true
    mockkObject(VersionComparator)
    every { VersionComparator.isNewVersionNewer(any(), any()) } returns true
    coEvery { _settingsRepository.obtainLatestAppRelease() } returns _githubRelease
    _viewModel.onCheckForAppUpdate(mockk())
    _viewModel.uiEvent.safeValue.dialog?.onConsumed()

    every { _permission.isUnknownSourceInstallationGranted() } returns
        isInstallationPermissionGranted
    coEvery { _settingsRepository.downloadAndInstallApp(any()) } just Runs
    _viewModel.onUpdateApp()
    assertAll(
        {
          assertEquals(
              if (!isInstallationPermissionGranted) SettingsDialogState.UnknownSourceInstallation
              else null,
              _viewModel.uiEvent.safeValue.dialog?.data,
              "Show unknown source installation permission dialog if not granted")
        },
        {
          assertDoesNotThrow("Update app using the fetched GitHub release data") {
            coVerify(exactly = if (isInstallationPermissionGranted) 1 else 0) {
              _settingsRepository.downloadAndInstallApp(eq(_githubRelease))
            }
          }
        })
  }

  @Test
  fun `on activity result for unknown source installation with fetched data`() {
    mockkObject(NetworkState)
    coEvery { NetworkState.isInternetAvailable(any()) } returns true
    mockkObject(VersionComparator)
    every { VersionComparator.isNewVersionNewer(any(), any()) } returns true
    coEvery { _settingsRepository.obtainLatestAppRelease() } returns _githubRelease
    _viewModel.onCheckForAppUpdate(mockk())

    _viewModel.onActivityResultForUnknownSourceInstallation()
    assertDoesNotThrow("Re-show app update dialog after returning from settings activity") {
      verify(exactly = 2) {
        _uiEventObserver.onChanged(
            match { it.dialog?.data == SettingsDialogState.UpdateAvailable(_githubRelease) })
      }
    }
  }
}
