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

package com.robifr.ledger.ui.createproduct

import android.text.Editable
import com.google.android.material.textfield.TextInputEditText
import com.robifr.ledger.ui.CurrencyTextWatcher
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductViewModel

class CreateProductPrice(private val _fragment: CreateProductFragment) {
  private val _priceTextWatcher: PriceTextWatcher =
      PriceTextWatcher(_fragment.createProductViewModel, _fragment.fragmentBinding.price)

  init {
    _fragment.fragmentBinding.price.addTextChangedListener(_priceTextWatcher)
  }

  fun setInputtedPriceText(formattedPrice: String) {
    if (_fragment.fragmentBinding.price.getText().toString() == formattedPrice) return
    // Remove listener to prevent any sort of formatting.
    _fragment.fragmentBinding.price.removeTextChangedListener(_priceTextWatcher)
    _fragment.fragmentBinding.price.setText(formattedPrice)
    _fragment.fragmentBinding.price.setSelection(formattedPrice.length)
    _fragment.fragmentBinding.price.addTextChangedListener(_priceTextWatcher)
  }
}

private class PriceTextWatcher(
    private val _createProductViewModel: CreateProductViewModel,
    view: TextInputEditText
) : CurrencyTextWatcher(view) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _createProductViewModel.onPriceTextChanged(newText())
  }
}
