/**
 * Copyright (c) 2024 Robi
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

package com.robifr.ledger.ui.createcustomer;

import android.text.Editable;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.robifr.ledger.ui.EditTextWatcher;
import java.util.Objects;

public class CreateCustomerName {
  @NonNull private final CreateCustomerFragment _fragment;
  @NonNull private final NameTextWatcher _nameTextWatcher;

  public CreateCustomerName(@NonNull CreateCustomerFragment fragment) {
    this._fragment = Objects.requireNonNull(fragment);
    this._nameTextWatcher = new NameTextWatcher(this._fragment.fragmentBinding().name);

    this._fragment.fragmentBinding().name.addTextChangedListener(this._nameTextWatcher);
  }

  public void setInputtedNameText(@NonNull String name) {
    Objects.requireNonNull(name);

    final String currentText = this._fragment.fragmentBinding().name.getText().toString();
    if (currentText.equals(name)) return;

    // Remove listener to prevent any sort of formatting although there isn't.
    this._fragment.fragmentBinding().name.removeTextChangedListener(this._nameTextWatcher);
    this._fragment.fragmentBinding().name.setText(name);
    this._fragment.fragmentBinding().name.setSelection(name.length());
    this._fragment.fragmentBinding().name.addTextChangedListener(this._nameTextWatcher);
  }

  public void setError(@Nullable String message) {
    this._fragment.fragmentBinding().nameLayout.setError(message);
  }

  private class NameTextWatcher extends EditTextWatcher {
    public NameTextWatcher(@NonNull EditText view) {
      super(view);
    }

    @Override
    public void afterTextChanged(@NonNull Editable editable) {
      super.afterTextChanged(editable);
      CreateCustomerName.this._fragment.createCustomerViewModel().onNameTextChanged(this.newText());
    }
  }
}
