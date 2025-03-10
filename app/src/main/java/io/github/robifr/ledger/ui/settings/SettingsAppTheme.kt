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

import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.display.AppTheme
import io.github.robifr.ledger.databinding.SettingsDialogThemeBinding

class SettingsAppTheme(private val _fragment: SettingsFragment) {
  private val _dialogBinding: SettingsDialogThemeBinding =
      SettingsDialogThemeBinding.inflate(_fragment.layoutInflater)
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_fragment.requireContext(), R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
        setOnDismissListener { _fragment.settingsViewModel.onAppThemeDialogClosed() }
      }

  init {
    _fragment.fragmentBinding.appThemeLayer.setOnClickListener {
      _fragment.settingsViewModel.onAppThemeDialogShown()
    }
  }

  fun setAppTheme(appTheme: AppTheme) {
    _fragment.fragmentBinding.appTheme.setText(appTheme.stringRes)
  }

  fun showDialog(selectedAppTheme: AppTheme) {
    _dialogBinding.radioGroup.findViewWithTag<RadioButton>(selectedAppTheme.toString())?.id?.let {
      _dialogBinding.radioGroup.check(it)
    }
    _dialogBinding.radioGroup.setOnCheckedChangeListener { group: RadioGroup?, radioId ->
      group?.findViewById<RadioButton>(radioId)?.tag?.let {
        _fragment.settingsViewModel.onAppThemeChanged(AppTheme.valueOf(it.toString()))
      }
      _fragment.settingsViewModel.onAppThemeDialogClosed()
    }
    _dialog.show()
  }

  fun dismissDialog() {
    _dialog.dismiss()
  }
}
