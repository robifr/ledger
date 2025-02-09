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

package com.robifr.ledger.ui.customer.filter

import android.text.Editable
import android.widget.EditText
import com.robifr.ledger.data.display.CustomerFilters
import com.robifr.ledger.databinding.CustomerDialogFilterBinding
import com.robifr.ledger.ui.common.CurrencyTextWatcher
import com.robifr.ledger.ui.customer.CustomerFragment

class CustomerFilterDebt(
    fragment: CustomerFragment,
    private val _dialogBinding: CustomerDialogFilterBinding
) {
  private val _minDebtTextWatcher: MinDebtTextWatcher =
      MinDebtTextWatcher(fragment, _dialogBinding.filterDebt.minimumDebt)
  private val _maxDebtTextWatcher: MaxDebtTextWatcher =
      MaxDebtTextWatcher(fragment, _dialogBinding.filterDebt.maximumDebt)

  init {
    _dialogBinding.filterDebt.minimumDebt.addTextChangedListener(_minDebtTextWatcher)
    _dialogBinding.filterDebt.maximumDebt.addTextChangedListener(_maxDebtTextWatcher)
  }

  /** @param formattedMinDebt Formatted text of min [debt][CustomerFilters.filteredDebt]. */
  fun setFilteredMinDebtText(formattedMinDebt: String) {
    if (_dialogBinding.filterDebt.minimumDebt.text.toString() == formattedMinDebt) return
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.filterDebt.minimumDebt.removeTextChangedListener(_minDebtTextWatcher)
    _dialogBinding.filterDebt.minimumDebt.setText(formattedMinDebt)
    _dialogBinding.filterDebt.minimumDebt.addTextChangedListener(_minDebtTextWatcher)
  }

  /** @param formattedMaxDebt Formatted text of max [debt][CustomerFilters.filteredDebt]. */
  fun setFilteredMaxDebtText(formattedMaxDebt: String) {
    if (_dialogBinding.filterDebt.maximumDebt.text.toString() == formattedMaxDebt) return
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.filterDebt.maximumDebt.removeTextChangedListener(_maxDebtTextWatcher)
    _dialogBinding.filterDebt.maximumDebt.setText(formattedMaxDebt)
    _dialogBinding.filterDebt.maximumDebt.addTextChangedListener(_maxDebtTextWatcher)
  }
}

private class MinDebtTextWatcher(private val _fragment: CustomerFragment, view: EditText) :
    CurrencyTextWatcher(view = view, _maximumAmount = Long.MAX_VALUE.toBigDecimal()) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.customerViewModel.filterView.onMinDebtTextChanged(newText())
  }
}

private class MaxDebtTextWatcher(private val _fragment: CustomerFragment, view: EditText) :
    CurrencyTextWatcher(view = view, _maximumAmount = Long.MAX_VALUE.toBigDecimal()) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.customerViewModel.filterView.onMaxDebtTextChanged(newText())
  }
}
