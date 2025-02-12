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
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.display.QueueFilters
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.text.ParseException

class QueueFilterViewModel(private val _onReloadFromInitialPage: () -> Unit) {
  private val _uiState: SafeMutableLiveData<QueueFilterState> =
      SafeMutableLiveData(
          QueueFilterState(
              isDialogShown = false,
              isNullCustomerShown = true,
              customerIds = listOf(),
              date = QueueDate(QueueDate.Range.ALL_TIME),
              status = QueueModel.Status.entries.toSet(),
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

  fun onDialogShown() {
    _uiState.setValue(uiState.safeValue.copy(isDialogShown = true))
  }

  fun onDialogClosed() {
    _uiState.setValue(uiState.safeValue.copy(isDialogShown = false))
    _onFiltersChanged()
    _onReloadFromInitialPage()
  }

  fun _onFiltersChanged(filters: QueueFilters = parseInputtedFilters()) {
    onNullCustomerShown(filters.isNullCustomerShown)
    onCustomerIdsChanged(filters.filteredCustomerIds)
    onStatusChanged(filters.filteredStatus)
    onDateChanged(filters.filteredDate)
    onMinTotalPriceTextChanged(
        filters.filteredTotalPrice.first?.let {
          CurrencyFormat.formatCents(it, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
    onMaxTotalPriceTextChanged(
        filters.filteredTotalPrice.second?.let {
          CurrencyFormat.formatCents(it, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
  }

  fun parseInputtedFilters(): QueueFilters {
    // All these nullable value to represent unbounded range.
    var minTotalPrice: BigDecimal? = null
    try {
      if (_uiState.safeValue.formattedMinTotalPrice.isNotBlank()) {
        minTotalPrice =
            CurrencyFormat.parseToCents(
                _uiState.safeValue.formattedMinTotalPrice,
                AppCompatDelegate.getApplicationLocales().toLanguageTags())
      }
    } catch (_: ParseException) {}

    var maxTotalPrice: BigDecimal? = null
    try {
      if (_uiState.safeValue.formattedMaxTotalPrice.isNotBlank()) {
        maxTotalPrice =
            CurrencyFormat.parseToCents(
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
