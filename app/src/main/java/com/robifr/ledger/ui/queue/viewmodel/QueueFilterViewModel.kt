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

package com.robifr.ledger.ui.queue.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.display.QueueFilterer
import com.robifr.ledger.data.display.QueueFilters
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.text.ParseException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QueueFilterViewModel(
    private val _viewModel: QueueViewModel,
    private val _dispatcher: CoroutineDispatcher,
    private val _selectAllQueues: suspend () -> List<QueueModel>
) {
  private val _filterer: QueueFilterer = QueueFilterer()

  private val _uiState: SafeMutableLiveData<QueueFilterState> =
      SafeMutableLiveData(
          QueueFilterState(
              isNullCustomerShown = _filterer.filters.isNullCustomerShown,
              customerIds = _filterer.filters.filteredCustomerIds,
              date = _filterer.filters.filteredDate,
              status = _filterer.filters.filteredStatus,
              formattedMinTotalPrice = "",
              formattedMaxTotalPrice = ""))
  val uiState: SafeLiveData<QueueFilterState>
    get() = _uiState

  fun onCustomerIdsChanged(customerIds: List<Long>) {
    _uiState.setValue(_uiState.safeValue.copy(customerIds = customerIds))
  }

  fun onNullCustomerShown(isShown: Boolean) {
    _uiState.setValue(_uiState.safeValue.copy(isNullCustomerShown = isShown))
  }

  fun onDateChanged(date: QueueDate) {
    _uiState.setValue(_uiState.safeValue.copy(date = date))
  }

  fun onStatusChanged(status: Set<QueueModel.Status>) {
    _uiState.setValue(_uiState.safeValue.copy(status = status))
  }

  fun onMinTotalPriceTextChanged(formattedMinTotalPrice: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedMinTotalPrice = formattedMinTotalPrice))
  }

  fun onMaxTotalPriceTextChanged(formattedMaxTotalPrice: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedMaxTotalPrice = formattedMaxTotalPrice))
  }

  fun onDialogClosed() {
    _viewModel.viewModelScope.launch(_dispatcher) {
      _selectAllQueues().let {
        withContext(Dispatchers.Main) { _onFiltersChanged(_parseInputtedFilters(), it) }
      }
    }
  }

  fun _onFiltersChanged(
      filters: QueueFilters = _parseInputtedFilters(),
      queues: List<QueueModel> = _viewModel.uiState.safeValue.queues
  ) {
    _filterer.filters = filters
    onNullCustomerShown(filters.isNullCustomerShown)
    onCustomerIdsChanged(filters.filteredCustomerIds)
    onStatusChanged(filters.filteredStatus)
    onDateChanged(filters.filteredDate)
    onMinTotalPriceTextChanged(
        filters.filteredTotalPrice.first?.let {
          CurrencyFormat.format(it, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
    onMaxTotalPriceTextChanged(
        filters.filteredTotalPrice.second?.let {
          CurrencyFormat.format(it, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
    _viewModel.onQueuesChanged(_filterer.filter(queues))
  }

  private fun _parseInputtedFilters(): QueueFilters {
    // All these nullable value to represent unbounded range.
    var minTotalPrice: BigDecimal? = null
    try {
      if (_uiState.safeValue.formattedMinTotalPrice.isNotBlank()) {
        minTotalPrice =
            CurrencyFormat.parse(
                _uiState.safeValue.formattedMinTotalPrice,
                AppCompatDelegate.getApplicationLocales().toLanguageTags())
      }
    } catch (_: ParseException) {}

    var maxTotalPrice: BigDecimal? = null
    try {
      if (_uiState.safeValue.formattedMaxTotalPrice.isNotBlank()) {
        maxTotalPrice =
            CurrencyFormat.parse(
                _uiState.safeValue.formattedMaxTotalPrice,
                AppCompatDelegate.getApplicationLocales().toLanguageTags())
      }
    } catch (_: ParseException) {}

    return QueueFilters(
        filteredCustomerIds = _uiState.safeValue.customerIds,
        isNullCustomerShown = _uiState.safeValue.isNullCustomerShown,
        filteredStatus = _uiState.safeValue.status,
        filteredDate = _uiState.safeValue.date,
        filteredTotalPrice = minTotalPrice to maxTotalPrice)
  }
}
