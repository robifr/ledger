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

package io.github.robifr.ledger.ui.settings

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import io.github.robifr.ledger.R
import io.github.robifr.ledger.network.GithubReleaseModel
import io.github.robifr.ledger.ui.main.RequiredPermission
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SettingsAppUpdate(private val _fragment: SettingsFragment) {
  private val _permission: RequiredPermission = RequiredPermission(_fragment.requireContext())
  private val _unknownSourceInstallationPermissionLauncher: ActivityResultLauncher<Intent> =
      _fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        _fragment.settingsViewModel.onActivityResultForUnknownSourceInstallation()
      }

  init {
    _fragment.fragmentBinding.appUpdateLayer.setOnClickListener {
      _fragment.settingsViewModel.onCheckForAppUpdate(_fragment.requireContext())
    }
  }

  fun setLastChecked(lastCheckedTime: ZonedDateTime, @StringRes dateFormat: Int) {
    _fragment.fragmentBinding.appUpdateLastChecked.text =
        _fragment.getString(
            R.string.settings_lastChecked_x,
            lastCheckedTime.format(DateTimeFormatter.ofPattern(_fragment.getString(dateFormat))))
  }

  fun showUpdateAvailableDialog(
      githubRelease: GithubReleaseModel,
      @StringRes dateFormat: Int,
      onDismiss: () -> Unit
  ) {
    AppUpdateAvailable(_fragment.requireContext())
        .showDialog(
            updateVersion = githubRelease.tagName,
            updateDate =
                ZonedDateTime.parse(githubRelease.publishedAt, DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern(_fragment.getString(dateFormat))),
            updateSize = githubRelease.sizeInMb(),
            onUpdate = { _fragment.settingsViewModel.onUpdateApp() },
            onDismiss = onDismiss)
  }

  fun showUnknownSourceInstallationPermissionDialog(onDismiss: () -> Unit) {
    _permission.showUnknownSourceInstallationDialog(
        onGrant = {
          _unknownSourceInstallationPermissionLauncher.launch(
              _permission.unknownSourceInstallationIntent())
        },
        onDismiss = onDismiss)
  }
}
