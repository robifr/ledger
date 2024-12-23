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
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.robifr.ledger.R
import com.robifr.ledger.databinding.SettingsFragmentBinding
import com.robifr.ledger.databinding.SettingsGeneralBinding
import com.robifr.ledger.network.GithubReleaseModel
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.settings.viewmodel.SettingsState
import com.robifr.ledger.ui.settings.viewmodel.SettingsViewModel
import com.robifr.ledger.util.getColorAttr
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class SettingsFragment : Fragment() {
  private var _fragmentBinding: SettingsFragmentBinding? = null
  val fragmentBinding: SettingsFragmentBinding
    get() = _fragmentBinding!!

  private var _generalBinding: SettingsGeneralBinding? = null
  val generalBinding: SettingsGeneralBinding
    get() = _generalBinding!!

  val settingsViewModel: SettingsViewModel by activityViewModels()
  private lateinit var _language: SettingsLanguage
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = SettingsFragmentBinding.inflate(inflater, container, false)
    _generalBinding = SettingsGeneralBinding.bind(fragmentBinding.root)
    _language = SettingsLanguage(this)
    _onBackPressed = OnBackPressedHandler(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    requireActivity().window.statusBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().window.navigationBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    // Use the activity's lifecycle owner to prevent the app from closing when the system
    // back button is pressed after a configuration change. Just ensure that it's removed
    // when this fragment is finished, to avoid a crash when closing the app.
    requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), _onBackPressed)
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    generalBinding.appUpdateLayout.setOnClickListener { settingsViewModel.onCheckForAppUpdate() }
    settingsViewModel.snackbarState.observe(viewLifecycleOwner, ::_onSnackbarState)
    settingsViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
    settingsViewModel.appUpdateModel.observe(viewLifecycleOwner, ::_onAppUpdateModel)
  }

  fun finish() {
    findNavController().popBackStack()
    _onBackPressed.remove()
  }

  private fun _onSnackbarState(state: SnackbarState) {
    Snackbar.make(
            fragmentBinding.root as View,
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onUiState(state: SettingsState) {
    _language.setLanguageUsed(state.languageUsed)
    generalBinding.appUpdateLastChecked.text =
        getString(
            R.string.settings_lastChecked_x,
            state.lastCheckedTimeForAppUpdate.format(
                DateTimeFormatter.ofPattern(
                    getString(
                        settingsViewModel.uiState.safeValue.languageUsed.detailedDateFormat))))
  }

  private fun _onAppUpdateModel(model: GithubReleaseModel) {
    val dateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern(
            getString(settingsViewModel.uiState.safeValue.languageUsed.fullDateFormat))
    AppUpdateAvailableDialog(requireContext())
        .openDialog(
            updateVersion = model.tagName,
            updateDate =
                ZonedDateTime.parse(model.publishedAt, DateTimeFormatter.ISO_DATE_TIME)
                    .format(dateFormat),
            onUpdate = { settingsViewModel.onUpdateApp() })
  }
}

private class OnBackPressedHandler(private val _fragment: SettingsFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    _fragment.finish()
  }
}
