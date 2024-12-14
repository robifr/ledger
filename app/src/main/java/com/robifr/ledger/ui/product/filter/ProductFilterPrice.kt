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

package com.robifr.ledger.ui.product.filter

import android.text.Editable
import android.widget.EditText
import com.robifr.ledger.components.CurrencyTextWatcher
import com.robifr.ledger.data.display.ProductFilters
import com.robifr.ledger.databinding.ProductDialogFilterBinding
import com.robifr.ledger.ui.product.ProductFragment

class ProductFilterPrice(
    fragment: ProductFragment,
    private val _dialogBinding: ProductDialogFilterBinding
) {
  private val _minPriceTextWatcher: MinPriceTextWatcher =
      MinPriceTextWatcher(fragment, _dialogBinding.filterPrice.minimumPrice)
  private val _maxPriceTextWatcher: MaxPriceTextWatcher =
      MaxPriceTextWatcher(fragment, _dialogBinding.filterPrice.maximumPrice)

  init {
    _dialogBinding.filterPrice.minimumPrice.addTextChangedListener(_minPriceTextWatcher)
    _dialogBinding.filterPrice.maximumPrice.addTextChangedListener(_maxPriceTextWatcher)
  }

  /** @param formattedMinPrice Formatted text of min [price][ProductFilters.filteredPrice]. */
  fun setFilteredMinPriceText(formattedMinPrice: String) {
    if (_dialogBinding.filterPrice.minimumPrice.text.toString() == formattedMinPrice) return
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.filterPrice.minimumPrice.removeTextChangedListener(_minPriceTextWatcher)
    _dialogBinding.filterPrice.minimumPrice.setText(formattedMinPrice)
    _dialogBinding.filterPrice.minimumPrice.addTextChangedListener(_minPriceTextWatcher)
  }

  /** @param formattedMaxPrice Formatted text of max [price][ProductFilters.filteredPrice]. */
  fun setFilteredMaxPriceText(formattedMaxPrice: String) {
    if (_dialogBinding.filterPrice.maximumPrice.text.toString() == formattedMaxPrice) return
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.filterPrice.maximumPrice.removeTextChangedListener(_maxPriceTextWatcher)
    _dialogBinding.filterPrice.maximumPrice.setText(formattedMaxPrice)
    _dialogBinding.filterPrice.maximumPrice.addTextChangedListener(_maxPriceTextWatcher)
  }
}

private class MinPriceTextWatcher(private val _fragment: ProductFragment, view: EditText) :
    CurrencyTextWatcher(view) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.productViewModel.filterView.onMinPriceTextChanged(newText())
  }
}

private class MaxPriceTextWatcher(private val _fragment: ProductFragment, view: EditText) :
    CurrencyTextWatcher(view) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.productViewModel.filterView.onMaxPriceTextChanged(newText())
  }
}
