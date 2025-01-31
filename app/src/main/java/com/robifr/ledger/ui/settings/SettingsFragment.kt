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

package com.robifr.ledger.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.robifr.ledger.R
import com.robifr.ledger.databinding.SettingsFragmentBinding
import com.robifr.ledger.ui.OnBackPressedHandler
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.settings.viewmodel.SettingsDialogState
import com.robifr.ledger.ui.settings.viewmodel.SettingsEvent
import com.robifr.ledger.ui.settings.viewmodel.SettingsState
import com.robifr.ledger.ui.settings.viewmodel.SettingsViewModel
import com.robifr.ledger.ui.settings.viewmodel.UnknownSourceInstallationDialogState
import com.robifr.ledger.ui.settings.viewmodel.UpdateAvailableDialogState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {
  private var _fragmentBinding: SettingsFragmentBinding? = null
  val fragmentBinding: SettingsFragmentBinding
    get() = _fragmentBinding!!

  val settingsViewModel: SettingsViewModel by activityViewModels()
  private lateinit var _appTheme: SettingsAppTheme
  private lateinit var _language: SettingsLanguage
  private lateinit var _appUpdate: SettingsAppUpdate

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = SettingsFragmentBinding.inflate(inflater, container, false)
    _appTheme = SettingsAppTheme(this)
    _language = SettingsLanguage(this)
    _appUpdate = SettingsAppUpdate(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { view, insets ->
      val systemBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      val windowInsets: Insets =
          insets.getInsets(
              WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars())
      view.updatePadding(
          top = systemBarInsets.top, left = windowInsets.left, right = windowInsets.right)
      WindowInsetsCompat.CONSUMED
    }
    requireActivity()
        .onBackPressedDispatcher
        .addCallback(viewLifecycleOwner, OnBackPressedHandler { finish() })
    fragmentBinding.toolbar.setNavigationOnClickListener { finish() }
    fragmentBinding.aboutAppLayer.setOnClickListener {
      findNavController().navigate(R.id.aboutFragment)
    }
    settingsViewModel.uiEvent.observe(viewLifecycleOwner, ::_onUiEvent)
    settingsViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
  }

  fun finish() {
    findNavController().popBackStack()
  }

  private fun _onUiEvent(event: SettingsEvent) {
    event.snackbar?.let {
      _onSnackbarState(it.data)
      it.onConsumed()
    }
    event.dialog?.let { _onDialogState(state = it.data, onDismiss = { it.onConsumed() }) }
  }

  private fun _onSnackbarState(state: SnackbarState) {
    Snackbar.make(
            fragmentBinding.root as View,
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onUiState(state: SettingsState) {
    _appTheme.setAppTheme(state.appTheme)
    if (state.isAppThemeDialogShown) _appTheme.showDialog(state.appTheme)
    else _appTheme.dismissDialog()
    _language.setLanguageUsed(state.languageUsed)
    if (state.isLanguageDialogShown) _language.showDialog(state.languageUsed)
    else _language.dismissDialog()
    _appUpdate.setLastChecked(
        state.lastCheckedTimeForAppUpdate, state.languageUsed.detailedDateFormat)
  }

  private fun _onDialogState(state: SettingsDialogState, onDismiss: () -> Unit) {
    when (state) {
      is UpdateAvailableDialogState ->
          _appUpdate.showUpdateAvailableDialog(
              state.githubRelease,
              settingsViewModel.uiState.safeValue.languageUsed.fullDateFormat,
              onDismiss)
      is UnknownSourceInstallationDialogState ->
          _appUpdate.showUnknownSourceInstallationPermissionDialog(onDismiss)
    }
  }
}
