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

package io.github.robifr.ledger.ui.customer.filter

import android.text.Editable
import android.widget.EditText
import io.github.robifr.ledger.data.display.CustomerFilters
import io.github.robifr.ledger.databinding.CustomerDialogFilterBinding
import io.github.robifr.ledger.ui.common.CurrencyTextWatcher
import io.github.robifr.ledger.ui.customer.CustomerFragment

class CustomerFilterBalance(
    fragment: CustomerFragment,
    private val _dialogBinding: CustomerDialogFilterBinding
) {
  private val _minBalanceTextWatcher: MinBalanceTextWatcher =
      MinBalanceTextWatcher(fragment, _dialogBinding.filterBalance.minimumBalance)
  private val _maxBalanceTextWatcher: MaxBalanceTextWatcher =
      MaxBalanceTextWatcher(fragment, _dialogBinding.filterBalance.maximumBalance)

  init {
    _dialogBinding.filterBalance.minimumBalance.addTextChangedListener(_minBalanceTextWatcher)
    _dialogBinding.filterBalance.maximumBalance.addTextChangedListener(_maxBalanceTextWatcher)
  }

  /**
   * @param formattedMinBalance Formatted text of min [balance][CustomerFilters.filteredBalance].
   */
  fun setFilteredMinBalanceText(formattedMinBalance: String) {
    if (_dialogBinding.filterBalance.minimumBalance.text.toString() == formattedMinBalance) return
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.filterBalance.minimumBalance.removeTextChangedListener(_minBalanceTextWatcher)
    _dialogBinding.filterBalance.minimumBalance.setText(formattedMinBalance)
    _dialogBinding.filterBalance.minimumBalance.addTextChangedListener(_minBalanceTextWatcher)
  }

  /**
   * @param formattedMaxBalance Formatted text of max [balance][CustomerFilters.filteredBalance].
   */
  fun setFilteredMaxBalanceText(formattedMaxBalance: String) {
    if (_dialogBinding.filterBalance.maximumBalance.text.toString() == formattedMaxBalance) return
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.filterBalance.maximumBalance.removeTextChangedListener(_maxBalanceTextWatcher)
    _dialogBinding.filterBalance.maximumBalance.setText(formattedMaxBalance)
    _dialogBinding.filterBalance.maximumBalance.addTextChangedListener(_maxBalanceTextWatcher)
  }
}

private class MinBalanceTextWatcher(private val _fragment: CustomerFragment, view: EditText) :
    CurrencyTextWatcher(view) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.customerViewModel.filterView.onMinBalanceTextChanged(newText())
  }
}

private class MaxBalanceTextWatcher(private val _fragment: CustomerFragment, view: EditText) :
    CurrencyTextWatcher(view) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.customerViewModel.filterView.onMaxBalanceTextChanged(newText())
  }
}
