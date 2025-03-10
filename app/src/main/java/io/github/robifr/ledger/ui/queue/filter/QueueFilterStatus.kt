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

package io.github.robifr.ledger.ui.queue.filter

import android.view.View
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.github.robifr.ledger.data.display.QueueFilters
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.databinding.QueueDialogFilterBinding
import io.github.robifr.ledger.ui.queue.QueueFragment

class QueueFilterStatus(
    private val _fragment: QueueFragment,
    private val _dialogBinding: QueueDialogFilterBinding
) : ChipGroup.OnCheckedStateChangeListener {
  init {
    _dialogBinding.filterStatus.chipGroup.setOnCheckedStateChangeListener(this)
  }

  override fun onCheckedChanged(group: ChipGroup, checkedIds: List<Int>) {
    _fragment.queueViewModel.filterView.onStatusChanged(
        checkedIds
            .mapNotNull { id ->
              group.findViewById<View>(id)?.tag?.let { QueueModel.Status.valueOf(it.toString()) }
            }
            .toHashSet())
  }

  /** @param status [QueueFilters.filteredStatus] */
  fun setFilteredStatus(status: Set<QueueModel.Status>) {
    for (view in _dialogBinding.filterStatus.chipGroup.children) {
      // Remove listener to prevent unintended updates to both view model
      // and the chip itself when manually set status.
      _dialogBinding.filterStatus.chipGroup.setOnCheckedStateChangeListener(null)
      (view as? Chip)?.isChecked = status.contains(QueueModel.Status.valueOf(view.tag.toString()))
      _dialogBinding.filterStatus.chipGroup.setOnCheckedStateChangeListener(this)
    }
  }
}
