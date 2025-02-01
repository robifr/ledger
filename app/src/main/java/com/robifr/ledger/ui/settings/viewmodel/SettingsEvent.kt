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

import com.robifr.ledger.network.GithubReleaseModel
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.UiEvent

data class SettingsEvent(
    val snackbar: UiEvent<SnackbarState>? = null,
    val dialog: UiEvent<SettingsDialogState>? = null
)

sealed interface SettingsDialogState {
  data class UpdateAvailable(val githubRelease: GithubReleaseModel) : SettingsDialogState

  data object UnknownSourceInstallation : SettingsDialogState
}
