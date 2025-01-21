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

package com.robifr.ledger.ui.customer.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.data.display.CustomerFilterer
import com.robifr.ledger.data.display.CustomerFilters
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.text.ParseException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerFilterViewModel(
    private val _viewModel: CustomerViewModel,
    private val _dispatcher: CoroutineDispatcher,
    private val _selectAllCustomers: suspend () -> List<CustomerModel>
) {
  private val _filterer: CustomerFilterer = CustomerFilterer()

  private val _uiState: SafeMutableLiveData<CustomerFilterState> =
      SafeMutableLiveData(
          CustomerFilterState(
              isDialogShown = false,
              formattedMinBalance = "",
              formattedMaxBalance = "",
              formattedMinDebt = "",
              formattedMaxDebt = ""))
  val uiState: SafeLiveData<CustomerFilterState>
    get() = _uiState

  fun onMinBalanceTextChanged(formattedMinBalance: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedMinBalance = formattedMinBalance))
  }

  fun onMaxBalanceTextChanged(formattedMaxBalance: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedMaxBalance = formattedMaxBalance))
  }

  fun onMinDebtTextChanged(formattedMinDebt: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedMinDebt = formattedMinDebt))
  }

  fun onMaxDebtTextChanged(formattedMaxDebt: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedMaxDebt = formattedMaxDebt))
  }

  fun onDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isDialogShown = true))
  }

  fun onDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isDialogShown = false))
    _viewModel.viewModelScope.launch(_dispatcher) {
      _selectAllCustomers().let {
        withContext(Dispatchers.Main) { _onFiltersChanged(customers = it) }
      }
    }
  }

  fun _onFiltersChanged(
      filters: CustomerFilters = _parseInputtedFilters(),
      customers: List<CustomerModel> = _viewModel.uiState.safeValue.customers
  ) {
    _filterer.filters = filters
    onMinBalanceTextChanged(
        filters.filteredBalance.first?.let {
          CurrencyFormat.format(
              it.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
    onMaxBalanceTextChanged(
        filters.filteredBalance.second?.let {
          CurrencyFormat.format(
              it.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
    onMinDebtTextChanged(
        filters.filteredDebt.first?.let {
          CurrencyFormat.format(it, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
    onMaxDebtTextChanged(
        filters.filteredDebt.second?.let {
          CurrencyFormat.format(it, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
    _viewModel.onCustomersChanged(_filterer.filter(customers))
  }

  private fun _parseInputtedFilters(): CustomerFilters {
    // All these nullable value to represent unbounded range.
    var minBalance: Long? = null
    try {
      if (_uiState.safeValue.formattedMinBalance.isNotBlank()) {
        minBalance =
            CurrencyFormat.parse(
                    _uiState.safeValue.formattedMinBalance,
                    AppCompatDelegate.getApplicationLocales().toLanguageTags())
                .toLong()
      }
    } catch (_: ParseException) {}

    var maxBalance: Long? = null
    try {
      if (_uiState.safeValue.formattedMaxBalance.isNotBlank()) {
        maxBalance =
            CurrencyFormat.parse(
                    _uiState.safeValue.formattedMaxBalance,
                    AppCompatDelegate.getApplicationLocales().toLanguageTags())
                .toLong()
      }
    } catch (_: ParseException) {}

    var minDebt: BigDecimal? = null
    try {
      if (_uiState.safeValue.formattedMinDebt.isNotBlank()) {
        minDebt =
            CurrencyFormat.parse(
                _uiState.safeValue.formattedMinDebt,
                AppCompatDelegate.getApplicationLocales().toLanguageTags())
      }
    } catch (_: ParseException) {}

    var maxDebt: BigDecimal? = null
    try {
      if (_uiState.safeValue.formattedMaxDebt.isNotBlank()) {
        maxDebt =
            CurrencyFormat.parse(
                _uiState.safeValue.formattedMaxDebt,
                AppCompatDelegate.getApplicationLocales().toLanguageTags())
      }
    } catch (_: ParseException) {}

    return CustomerFilters(
        filteredBalance = minBalance to maxBalance, filteredDebt = minDebt to maxDebt)
  }
}
