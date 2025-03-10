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

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import io.github.robifr.ledger.InstantTaskExecutorExtension
import io.github.robifr.ledger.LifecycleOwnerExtension
import io.github.robifr.ledger.LifecycleTestOwner
import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.display.AppTheme
import io.github.robifr.ledger.data.display.LanguageOption
import io.github.robifr.ledger.network.GithubReleaseModel
import io.github.robifr.ledger.network.NetworkState
import io.github.robifr.ledger.repository.SettingsRepository
import io.github.robifr.ledger.ui.main.RequiredPermission
import io.github.robifr.ledger.util.VersionComparator
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.appTheme)
          .describedAs("Update current app theme")
          .isEqualTo(
              AppTheme.DARK,
          )
      it.assertThatCode {
            verify { AppCompatDelegate.setDefaultNightMode(any()) }
            coVerify { _settingsRepository.saveAppTheme(any()) }
          }
          .describedAs("Immediately apply and save the current app theme")
          .doesNotThrowAnyException()
    }
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
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                appTheme = AppTheme.DARK,
                isAppThemeDialogShown = isShown,
                languageUsed = LanguageOption.INDONESIA))
  }

  @Test
  fun `on language changed`() {
    mockkStatic(AppCompatDelegate::class)
    coEvery { _settingsRepository.saveLanguageUsed(any()) } returns true
    _viewModel.onLanguageChanged(LanguageOption.INDONESIA)
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.languageUsed)
          .describedAs("Update current used language")
          .isEqualTo(LanguageOption.INDONESIA)
      it.assertThatCode {
            verify { AppCompatDelegate.setApplicationLocales(any()) }
            coVerify { _settingsRepository.saveLanguageUsed(any()) }
          }
          .describedAs("Immediately apply and save the current used language")
          .doesNotThrowAnyException()
    }
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
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                appTheme = AppTheme.DARK,
                languageUsed = LanguageOption.INDONESIA,
                isLanguageDialogShown = isShown))
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on check for app update with internet not available`(shouldSnackbarShown: Boolean) {
    mockkObject(NetworkState)
    coEvery { NetworkState.isInternetAvailable(any()) } returns false
    _viewModel.onCheckForAppUpdate(mockk(), shouldSnackbarShown)
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
          .describedAs("Notify unavailable internet via snackbar")
          .apply { if (shouldSnackbarShown) isNotNull() else isNull() }
      it.assertThatCode { coVerify(exactly = 0) { _settingsRepository.obtainLatestAppRelease() } }
          .describedAs("Prevent fetching updates when the internet is unavailable")
          .doesNotThrowAnyException()
    }
  }

  @Test
  fun `on check for app update with latest release not available`() {
    mockkObject(NetworkState)
    coEvery { NetworkState.isInternetAvailable(any()) } returns true
    val oldUiState: SettingsState = _viewModel.uiState.safeValue

    coEvery { _settingsRepository.obtainLatestAppRelease() } returns null
    _viewModel.onCheckForAppUpdate(mockk())
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve all values when the latest release is null")
        .isEqualTo(oldUiState)
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
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.dialog?.data)
          .describedAs("Show available app update via alert dialog")
          .isEqualTo(
              if (isUpdateAvailableDialogShown) SettingsDialogState.UpdateAvailable(githubRelease)
              else null)
      it.assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
          .describedAs("Notify unavailable app update via snackbar")
          .apply { if (isNoUpdateAvailableSnackbarShown) isNotNull() else isNull() }
      it.assertThat(_viewModel.uiState.safeValue)
          .describedAs("Update last checked time and GitHub release model")
          .isEqualTo(
              oldUiState.copy(
                  lastCheckedTimeForAppUpdate = lastCheckedTime.atZone(ZoneId.systemDefault()),
                  githubRelease = githubRelease))
    }
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
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.dialog?.data)
          .describedAs("Show unknown source installation permission dialog if not granted")
          .isEqualTo(
              if (!isInstallationPermissionGranted) SettingsDialogState.UnknownSourceInstallation
              else null)
      it.assertThatCode {
            coVerify(exactly = if (isInstallationPermissionGranted) 1 else 0) {
              _settingsRepository.downloadAndInstallApp(eq(_githubRelease))
            }
          }
          .describedAs("Update app using the fetched GitHub release data")
          .doesNotThrowAnyException()
    }
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
    assertThatCode {
          verify(exactly = 2) {
            _uiEventObserver.onChanged(
                match { it.dialog?.data == SettingsDialogState.UpdateAvailable(_githubRelease) })
          }
        }
        .describedAs("Re-show app update dialog after returning from settings activity")
        .doesNotThrowAnyException()
  }
}
