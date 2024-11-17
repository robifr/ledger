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

import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.robifr.ledger.R
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.databinding.SettingsDialogLanguageBinding
import com.robifr.ledger.databinding.SettingsGeneralBinding

class SettingsLanguage(private val _fragment: SettingsFragment) {
  private val _generalBinding: SettingsGeneralBinding =
      SettingsGeneralBinding.bind(_fragment.fragmentBinding.root).apply {
        languageLayout.setOnClickListener {
          _dialogBinding.radioGroup
              .findViewWithTag<RadioButton>(
                  _fragment.settingsViewModel.uiState.safeValue.languageUsed.toString())
              ?.id
              ?.let { _dialogBinding.radioGroup.check(it) }
          _dialog.show()
        }
      }
  private val _dialogBinding: SettingsDialogLanguageBinding =
      SettingsDialogLanguageBinding.inflate(_fragment.layoutInflater).apply {
        radioGroup.setOnCheckedChangeListener { group: RadioGroup?, radioId ->
          group?.findViewById<RadioButton>(radioId)?.tag?.let {
            _fragment.settingsViewModel.onLanguageChanged(LanguageOption.valueOf(it.toString()))
          }
          _dialog.dismiss()
        }
      }
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_fragment.requireContext(), R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
      }

  fun setLanguageUsed(language: LanguageOption) {
    _generalBinding.language.setText(language.stringRes)
  }
}
