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

package com.robifr.ledger.ui.createqueue

import android.view.View
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.robifr.ledger.R
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.databinding.CreateQueueDialogStatusBinding

class CreateQueueStatus(private val _fragment: CreateQueueFragment) {
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_fragment.requireContext(), R.style.BottomSheetDialog)
  private val _dialogBinding: CreateQueueDialogStatusBinding =
      CreateQueueDialogStatusBinding.inflate(_fragment.layoutInflater)

  init {
    _dialog.setContentView(_dialogBinding.root)
    _fragment.fragmentBinding.status.setOnClickListener { _openDialog() }
  }

  fun setInputtedStatus(status: QueueModel.Status) {
    _fragment.fragmentBinding.status.setText(status.resourceString)
  }

  private fun _openDialog() {
    _dialogBinding.radioGroup.check(
        _dialogBinding.radioGroup
            .findViewWithTag<View>(
                _fragment.createQueueViewModel.uiState.safeValue.status.toString())
            .id)
    _dialogBinding.radioGroup.setOnCheckedChangeListener { group: RadioGroup?, radioId ->
      _fragment.createQueueViewModel.onStatusChanged(
          QueueModel.Status.valueOf(group?.findViewById<View>(radioId)?.tag.toString()))
      _dialog.dismiss()
    }
    _dialog.behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
    _dialog.show()
  }
}
