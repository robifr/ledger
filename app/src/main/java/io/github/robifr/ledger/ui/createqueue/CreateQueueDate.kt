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

package io.github.robifr.ledger.ui.createqueue

import androidx.annotation.StringRes
import com.google.android.material.R as MaterialR
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.robifr.ledger.R
import io.github.robifr.ledger.util.ClassPath
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CreateQueueDate(private val _fragment: CreateQueueFragment) {
  init {
    _fragment.fragmentBinding.date.setOnClickListener {
      _dialog()
          .show(
              _fragment.requireActivity().supportFragmentManager,
              ClassPath.simpleName(CreateQueueDate::class.java))
    }
  }

  fun setInputtedDate(date: ZonedDateTime, @StringRes dateFormat: Int) {
    _fragment.fragmentBinding.date.setText(
        DateTimeFormatter.ofPattern(_fragment.getString(dateFormat)).format(date))
  }

  private fun _dialog(): MaterialDatePicker<Long> =
      MaterialDatePicker.Builder.datePicker()
          .setTheme(MaterialR.style.ThemeOverlay_Material3_MaterialCalendar)
          .setTitleText(R.string.createQueue_date_selectDate)
          .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
          .setCalendarConstraints(
              CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build())
          // Always recreate the dialog everytime it opens, so that the date selection
          // is handled properly.
          .setSelection(
              _fragment.createQueueViewModel.uiState.safeValue.date
                  .toLocalDate()
                  .atStartOfDay()
                  .atZone(ZoneId.of("UTC")) // Material only accept UTC time.
                  .toInstant()
                  .toEpochMilli())
          .build()
          .apply {
            addOnPositiveButtonClickListener { date: Long? ->
              date?.let {
                _fragment.createQueueViewModel.onDateChanged(
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
              }
            }
          }
}
