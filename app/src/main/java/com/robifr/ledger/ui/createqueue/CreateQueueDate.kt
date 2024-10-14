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

import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.robifr.ledger.R
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CreateQueueDate(private val _fragment: CreateQueueFragment) {
  init {
    _fragment.fragmentBinding.date.setOnClickListener {
      MaterialDatePicker.Builder.datePicker()
          .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
          .setTitleText(R.string.createQueue_date_selectDate)
          .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
          .setCalendarConstraints(
              CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build())
          .setSelection(
              _fragment.createQueueViewModel.uiState.safeValue.date
                  .toLocalDate()
                  .atStartOfDay()
                  .atZone(ZoneId.of("UTC")) // Material only accept UTC time.
                  .toInstant()
                  .toEpochMilli())
          .build()
          .apply {
            show(_fragment.childFragmentManager, CreateQueueDate::class.java.toString())
            addOnPositiveButtonClickListener { date: Long? ->
              _fragment.createQueueViewModel.onDateChanged(
                  Instant.ofEpochMilli(date ?: 0L).atZone(ZoneId.systemDefault()))
            }
          }
    }
  }

  fun setInputtedDate(date: ZonedDateTime) {
    _fragment.fragmentBinding.date.setText(
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy").format(date))
  }
}
