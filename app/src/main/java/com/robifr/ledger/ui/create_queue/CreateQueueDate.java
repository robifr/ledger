/**
 * Copyright (c) 2022-present Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package com.robifr.ledger.ui.create_queue;

import android.view.View;
import androidx.annotation.NonNull;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.robifr.ledger.R;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class CreateQueueDate implements View.OnClickListener {
  @NonNull private final CreateQueueFragment _fragment;

  public CreateQueueDate(@NonNull CreateQueueFragment fragment) {
    this._fragment = Objects.requireNonNull(fragment);

    this._fragment.fragmentBinding().date.setOnClickListener(this);
  }

  @Override
  public void onClick(@NonNull View view) {
    Objects.requireNonNull(view);

    final ZonedDateTime inputtedDate =
        this._fragment.createQueueViewModel().inputtedDate().getValue();

    switch (view.getId()) {
      case R.id.date -> {
        final MaterialDatePicker.Builder<Long> pickerBuilder =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setTheme(R.style.MaterialDatePicker)
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR);

        if (inputtedDate != null) {
          pickerBuilder.setSelection(
              inputtedDate
                  .toLocalDate()
                  .atStartOfDay()
                  .atZone(ZoneId.of("UTC")) // Material only accept UTC time.
                  .toInstant()
                  .toEpochMilli());
        }

        final MaterialDatePicker<Long> picker = pickerBuilder.build();
        picker.show(this._fragment.getChildFragmentManager(), CreateQueueDate.class.toString());
        picker.addOnPositiveButtonClickListener(
            date -> {
              final ZonedDateTime parsedDate =
                  Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault());
              this._fragment.createQueueViewModel().onDateChanged(parsedDate);
            });
      }
    }
  }

  public void setInputtedDate(@NonNull ZonedDateTime date) {
    Objects.requireNonNull(date);

    final String formatted = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy").format(date);
    this._fragment.fragmentBinding().date.setText(formatted);
  }
}
