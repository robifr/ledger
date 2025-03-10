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

package io.github.robifr.ledger.ui.dashboard

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.util.Pair
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.display.QueueDate
import io.github.robifr.ledger.databinding.DashboardDialogDateBinding
import io.github.robifr.ledger.util.ClassPath
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class DashboardDate(
    fragment: DashboardFragment,
    dateChip: () -> Chip,
    onDialogShown: () -> Unit,
    private val _onDialogClosed: () -> Unit,
    private val _onDateChanged: (QueueDate) -> Unit
) {
  private val _dialogBinding: DashboardDialogDateBinding =
      DashboardDialogDateBinding.inflate(fragment.layoutInflater).apply {
        customButton.setOnClickListener {
          _customDatePickerDialog.show(
              fragment.requireActivity().supportFragmentManager,
              ClassPath.simpleName(DashboardDate::class.java))
        }
      }
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(fragment.requireContext(), R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
        setOnDismissListener { _onDialogClosed() }
      }
  private val _customDatePickerDialog: MaterialDatePicker<Pair<Long, Long>> =
      MaterialDatePicker.Builder.dateRangePicker()
          .setTheme(MaterialR.style.ThemeOverlay_Material3_MaterialCalendar)
          .setTitleText(R.string.dashboard_date_selectDateRange)
          .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
          .build()
          .apply {
            addOnPositiveButtonClickListener { date: Pair<Long?, Long?>? ->
              date?.first?.let { startDate ->
                date.second?.let { endDate ->
                  _onDateChanged(
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
              _onDialogClosed()
            }
          }

  init {
    dateChip().setOnClickListener { onDialogShown() }
  }

  fun showDialog(selectedDateRange: () -> QueueDate.Range) {
    // Remove listener to prevent callback being called during `check()` and `clearCheck()`.
    _dialogBinding.radioGroup.setOnCheckedChangeListener(null)
    // Custom range uses classic button. They aren't supposed to get selected.
    if (selectedDateRange() != QueueDate.Range.CUSTOM) {
      _dialogBinding.radioGroup.findViewWithTag<View>(selectedDateRange().toString())?.id?.let {
        _dialogBinding.radioGroup.check(it)
      }
    } else {
      // When the custom range get selected (button), all the other radios have to be unchecked.
      _dialogBinding.radioGroup.clearCheck()
    }
    _dialogBinding.radioGroup.setOnCheckedChangeListener { group: RadioGroup?, radioId ->
      group?.findViewById<RadioButton>(radioId)?.tag?.let {
        _onDateChanged(QueueDate(QueueDate.Range.valueOf(it.toString())))
      }
      _onDialogClosed()
    }
    _dialog.show()
  }

  fun dismissDialog() {
    _dialog.dismiss()
  }
}
