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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.robifr.ledger.databinding.SettingsFragmentBinding
import com.robifr.ledger.ui.settings.viewmodel.SettingsState
import com.robifr.ledger.ui.settings.viewmodel.SettingsViewModel
import com.robifr.ledger.util.getColorAttr
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {
  private var _fragmentBinding: SettingsFragmentBinding? = null
  val fragmentBinding: SettingsFragmentBinding
    get() = _fragmentBinding!!

  val settingsViewModel: SettingsViewModel by viewModels()
  private lateinit var _language: SettingsLanguage
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = SettingsFragmentBinding.inflate(inflater, container, false)
    _language = SettingsLanguage(this)
    _onBackPressed = OnBackPressedHandler(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    requireActivity().window.statusBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().window.navigationBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    settingsViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
  }

  fun finish() {
    findNavController().popBackStack()
  }

  private fun _onUiState(state: SettingsState) {
    _language.setLanguageUsed(state.languageUsed)
  }
}

private class OnBackPressedHandler(private val _fragment: SettingsFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    _fragment.finish()
  }
}
