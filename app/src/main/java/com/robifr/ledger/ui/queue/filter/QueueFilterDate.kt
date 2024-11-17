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

import android.view.View
import androidx.core.util.Pair
import androidx.core.view.isVisible
import com.google.android.material.R as MaterialR
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.robifr.ledger.R
import com.robifr.ledger.data.display.QueueDateKt
import com.robifr.ledger.data.display.QueueFilters
import com.robifr.ledger.databinding.QueueDialogFilterBinding
import com.robifr.ledger.ui.queue.QueueFragment
import com.robifr.ledger.util.ClassPath
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class QueueFilterDate(
    private val _fragment: QueueFragment,
    private val _dialogBinding: QueueDialogFilterBinding
) : ChipGroup.OnCheckedStateChangeListener {
  private val _datePickerDialog: MaterialDatePicker<Pair<Long?, Long?>?> =
      MaterialDatePicker.Builder.dateRangePicker()
          .setTheme(MaterialR.style.ThemeOverlay_Material3_MaterialCalendar)
          .setTitleText(R.string.queue_filterDate_selectDateRange)
          .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
          .build()
          .apply {
            addOnPositiveButtonClickListener { date: Pair<Long?, Long?>? ->
              date?.first?.let { startDate ->
                date.second?.let { endDate ->
                  _fragment.queueViewModel.filterView.onDateChanged(
                      QueueDateKt(
                          Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()),
                          Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault())))
                }
              }
            }
          }

  init {
    _dialogBinding.filterDate.chipGroup.setOnCheckedStateChangeListener(this)
    _dialogBinding.filterDate.customDateButton.setOnClickListener {
      _datePickerDialog.show(
          _fragment.requireActivity().supportFragmentManager,
          ClassPath.simpleName(QueueFilterDate::class.java))
    }
  }

  override fun onCheckedChanged(group: ChipGroup, checkedIds: List<Int>) {
    for (id in checkedIds) {
      group.findViewById<View>(id)?.tag?.let {
        _fragment.queueViewModel.filterView.onDateChanged(
            QueueDateKt(QueueDateKt.Range.valueOf(it.toString())))
      }
      break // Chip group should only be allowed to select single chip at time.
    }
  }

  /** @param date [QueueFilters.filteredDate] */
  fun setFilteredDate(date: QueueDateKt) {
    // Remove listener to prevent unintended updates to both view model and the chip itself
    // when manually set the date, like `QueueDate.Range#CUSTOM`.
    _dialogBinding.filterDate.chipGroup.setOnCheckedStateChangeListener(null)
    _dialogBinding.filterDate.chipGroup.findViewWithTag<Chip>(date.range.toString())?.isChecked =
        true
    _dialogBinding.filterDate.chipGroup.setOnCheckedStateChangeListener(this)
    _dialogBinding.filterDate.chipGroup
        .findViewWithTag<Chip>(QueueDateKt.Range.CUSTOM.toString())
        ?.apply {
          val format: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
          // Hide custom range chip when it's not being selected, and show otherwise.
          isVisible = date.range == QueueDateKt.Range.CUSTOM
          text =
              _fragment.getString(
                  QueueDateKt.Range.CUSTOM.stringRes,
                  date.dateStart.format(format),
                  date.dateEnd.format(format))
        }
  }
}
