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

import android.text.Editable
import android.widget.EditText
import com.robifr.ledger.data.display.QueueFilters
import com.robifr.ledger.databinding.QueueDialogFilterBinding
import com.robifr.ledger.ui.CurrencyTextWatcher
import com.robifr.ledger.ui.queue.QueueFragment

class QueueFilterTotalPrice(
    fragment: QueueFragment,
    private val _dialogBinding: QueueDialogFilterBinding
) {
  private val _minPriceTextWatcher: MinTotalPriceTextWatcher =
      MinTotalPriceTextWatcher(fragment, _dialogBinding.filterTotalPrice.minimumTotalPrice)
  private val _maxPriceTextWatcher: MaxTotalPriceTextWatcher =
      MaxTotalPriceTextWatcher(fragment, _dialogBinding.filterTotalPrice.maximumTotalPrice)

  init {
    _dialogBinding.filterTotalPrice.minimumTotalPrice.addTextChangedListener(_minPriceTextWatcher)
    _dialogBinding.filterTotalPrice.maximumTotalPrice.addTextChangedListener(_maxPriceTextWatcher)
  }

  /**
   * @param formattedMinTotalPrice Formatted text of min
   *   [total price][QueueFilters.filteredTotalPrice].
   */
  fun setFilteredMinTotalPriceText(formattedMinTotalPrice: String) {
    if (_dialogBinding.filterTotalPrice.minimumTotalPrice.text.toString() ==
        formattedMinTotalPrice) {
      return
    }
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.filterTotalPrice.minimumTotalPrice.removeTextChangedListener(
        _minPriceTextWatcher)
    _dialogBinding.filterTotalPrice.minimumTotalPrice.setText(formattedMinTotalPrice)
    _dialogBinding.filterTotalPrice.minimumTotalPrice.addTextChangedListener(_minPriceTextWatcher)
  }

  /**
   * @param formattedMaxTotalPrice Formatted text of max
   *   [total price][QueueFilters.filteredTotalPrice].
   */
  fun setFilteredMaxTotalPriceText(formattedMaxTotalPrice: String) {
    if (_dialogBinding.filterTotalPrice.maximumTotalPrice.text.toString() ==
        formattedMaxTotalPrice) {
      return
    }
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.filterTotalPrice.maximumTotalPrice.removeTextChangedListener(
        _maxPriceTextWatcher)
    _dialogBinding.filterTotalPrice.maximumTotalPrice.setText(formattedMaxTotalPrice)
    _dialogBinding.filterTotalPrice.maximumTotalPrice.addTextChangedListener(_maxPriceTextWatcher)
  }
}

private class MinTotalPriceTextWatcher(private val _fragment: QueueFragment, view: EditText) :
    CurrencyTextWatcher(view) {
  init {
    _maximumAmount = Long.MAX_VALUE.toBigDecimal()
  }

  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.queueViewModel.filterView.onMinTotalPriceTextChanged(newText())
  }
}

private class MaxTotalPriceTextWatcher(private val _fragment: QueueFragment, view: EditText) :
    CurrencyTextWatcher(view) {
  init {
    _maximumAmount = Long.MAX_VALUE.toBigDecimal()
  }

  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.queueViewModel.filterView.onMaxTotalPriceTextChanged(newText())
  }
}
