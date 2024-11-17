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

package com.robifr.ledger.ui.queue

import android.graphics.drawable.StateListDrawable
import android.widget.RadioButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.robifr.ledger.R
import com.robifr.ledger.data.display.QueueSortMethod
import com.robifr.ledger.databinding.QueueDialogSortByBinding

class QueueSort(private val _fragment: QueueFragment) {
  private val _dialogBinding: QueueDialogSortByBinding =
      QueueDialogSortByBinding.inflate(_fragment.layoutInflater).apply {
        for (sortBy in QueueSortMethod.SortBy.entries) {
          // Don't use `RadioGroup.OnCheckedChangeListener` interface,
          // cause that wouldn't work when user re-select same radio to revert sort order.
          radioGroup.findViewWithTag<RadioButton>(sortBy.toString())?.setOnClickListener {
            _fragment.queueViewModel.onSortMethodChanged(sortBy)
            _dialog.dismiss()
          }
        }
      }
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_fragment.requireContext(), R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
      }

  init {
    _fragment.fragmentBinding.sortByChip.setText(R.string.queue_sortBy)
    _fragment.fragmentBinding.sortByChip.setOnClickListener {
      _dialogBinding.radioGroup
          .findViewWithTag<RadioButton>(
              _fragment.queueViewModel.uiState.safeValue.sortMethod.sortBy.toString())
          ?.let {
            _dialogBinding.radioGroup.check(it.id)
            _updateRadioIcon(it, _fragment.queueViewModel.uiState.safeValue.sortMethod.isAscending)
          }
      _dialog.show()
    }
  }

  private fun _updateRadioIcon(radio: RadioButton, isAscending: Boolean) {
    val icon: Int =
        if (isAscending) R.drawable.icon_arrow_upward else R.drawable.icon_arrow_downward
    radio.setCompoundDrawablesWithIntrinsicBounds(
        StateListDrawable().apply {
          // State to show the icon when they're checked.
          addState(
              intArrayOf(android.R.attr.state_checked),
              _fragment.requireContext().getDrawable(icon))
          // The default state to hide the icon by setting it to transparent.
          addState(
              intArrayOf(),
              _fragment.requireContext().getDrawable(R.drawable.icon_radio_check_hideable))
        },
        null,
        null,
        null)
  }
}
