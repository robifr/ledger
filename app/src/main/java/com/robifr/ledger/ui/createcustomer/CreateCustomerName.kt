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

package com.robifr.ledger.ui.createcustomer

import android.text.Editable
import com.google.android.material.textfield.TextInputEditText
import com.robifr.ledger.ui.EditTextWatcher
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerViewModel

class CreateCustomerName(private val _fragment: CreateCustomerFragment) {
  private val _nameTextWatcher: NameTextWatcher =
      NameTextWatcher(_fragment.createCustomerViewModel, _fragment.fragmentBinding.name)

  init {
    _fragment.fragmentBinding.name.addTextChangedListener(_nameTextWatcher)
  }

  fun setInputtedNameText(name: String, errorMessage: String?) {
    if (_fragment.fragmentBinding.name.text.toString() != name) {
      // Remove listener to prevent any sort of formatting although there isn't.
      _fragment.fragmentBinding.name.removeTextChangedListener(_nameTextWatcher)
      _fragment.fragmentBinding.name.setText(name)
      _fragment.fragmentBinding.name.setSelection(name.length)
      _fragment.fragmentBinding.name.addTextChangedListener(_nameTextWatcher)
    }
    _fragment.fragmentBinding.nameLayout.setError(errorMessage)
  }
}

private class NameTextWatcher(
    private val _createCustomerViewModel: CreateCustomerViewModel,
    editText: TextInputEditText
) : EditTextWatcher(editText) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _createCustomerViewModel.onNameTextChanged(newText())
  }
}
