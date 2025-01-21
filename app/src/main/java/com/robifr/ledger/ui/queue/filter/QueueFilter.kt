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

package com.robifr.ledger.ui.queue.filter

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.robifr.ledger.R
import com.robifr.ledger.databinding.QueueDialogFilterBinding
import com.robifr.ledger.ui.queue.QueueFragment

class QueueFilter(private val _fragment: QueueFragment) {
  private val _dialogBinding: QueueDialogFilterBinding =
      QueueDialogFilterBinding.inflate(_fragment.layoutInflater)
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_fragment.requireContext(), R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
        setOnDismissListener { _fragment.queueViewModel.filterView.onDialogClosed() }
      }
  val filterCustomer: QueueFilterCustomer = QueueFilterCustomer(_dialog, _fragment, _dialogBinding)
  val filterDate: QueueFilterDate = QueueFilterDate(_fragment, _dialogBinding)
  val filterStatus: QueueFilterStatus = QueueFilterStatus(_fragment, _dialogBinding)
  val filterTotalPrice: QueueFilterTotalPrice = QueueFilterTotalPrice(_fragment, _dialogBinding)

  init {
    _fragment.fragmentBinding.filtersChip.setText(R.string.queue_filters)
    _fragment.fragmentBinding.filtersChip.setOnClickListener {
      _fragment.queueViewModel.filterView.onDialogShown()
    }
  }

  fun showDialog() {
    _dialog.show()
  }

  fun dismissDialog() {
    _dialog.dismiss()
    _dialog.currentFocus?.clearFocus()
  }
}
