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
import androidx.annotation.StringRes
import androidx.core.util.Pair
import androidx.core.view.isVisible
import com.google.android.material.R as MaterialR
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.display.QueueDate
import io.github.robifr.ledger.data.display.QueueFilters
import io.github.robifr.ledger.databinding.QueueDialogFilterBinding
import io.github.robifr.ledger.ui.queue.QueueFragment
import io.github.robifr.ledger.util.ClassPath
import java.time.Instant
import java.time.LocalTime
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
                      QueueDate(
                          Instant.ofEpochMilli(startDate)
                              .atZone(ZoneId.systemDefault())
                              .toLocalDate()
                              .atStartOfDay(ZoneId.systemDefault()),
                          Instant.ofEpochMilli(endDate)
                              .atZone(ZoneId.systemDefault())
                              .toLocalDate()
                              .atTime(LocalTime.MAX)
                              .atZone(ZoneId.systemDefault())))
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
            QueueDate(QueueDate.Range.valueOf(it.toString())))
      }
      break // Chip group should only be allowed to select single chip at time.
    }
  }

  /** @param date [QueueFilters.filteredDate] */
  fun setFilteredDate(date: QueueDate, @StringRes dateFormat: Int) {
    // Remove listener to prevent unintended updates to both view model and the chip itself
    // when manually set the date, like `QueueDate.Range.CUSTOM`.
    _dialogBinding.filterDate.chipGroup.setOnCheckedStateChangeListener(null)
    _dialogBinding.filterDate.chipGroup.findViewWithTag<Chip>(date.range.toString())?.isChecked =
        true
    _dialogBinding.filterDate.chipGroup.setOnCheckedStateChangeListener(this)
    _dialogBinding.filterDate.chipGroup
        .findViewWithTag<Chip>(QueueDate.Range.CUSTOM.toString())
        ?.apply {
          val format: DateTimeFormatter =
              DateTimeFormatter.ofPattern(_fragment.getString(dateFormat))
          // Hide custom range chip when it's not being selected, and show otherwise.
          isVisible = date.range == QueueDate.Range.CUSTOM
          text =
              _fragment.getString(
                  QueueDate.Range.CUSTOM.stringRes,
                  date.dateStart.format(format),
                  date.dateEnd.format(format))
        }
  }
}
