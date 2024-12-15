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

package com.robifr.ledger.ui.createcustomer.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMediatorLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.text.ParseException

class CustomerBalanceViewModel(private val _createCustomerViewModel: CreateCustomerViewModel) {
  private val _addBalanceState: SafeMutableLiveData<CustomerBalanceAddState> =
      SafeMutableLiveData(CustomerBalanceAddState(""))
  val addBalanceState: SafeLiveData<CustomerBalanceAddState>
    get() = _addBalanceState

  private val _withdrawBalanceState: SafeMediatorLiveData<CustomerBalanceWithdrawState> =
      SafeMediatorLiveData(
              CustomerBalanceWithdrawState(formattedAmount = "", availableAmountToWithdraw = 0L))
          .apply {
            addSource(_createCustomerViewModel.uiState.toLiveData()) {
              _withdrawBalanceState.setValue(
                  _withdrawBalanceState.safeValue.copy(availableAmountToWithdraw = it.balance))
            }
          }
  val withdrawBalanceState: SafeLiveData<CustomerBalanceWithdrawState>
    get() = _withdrawBalanceState

  fun onAddBalanceDialogClosed() {
    _addBalanceState.setValue(CustomerBalanceAddState(""))
  }

  fun onAddBalanceSubmitted() {
    _createCustomerViewModel.onBalanceChanged(
        _createCustomerViewModel.uiState.safeValue.balance + _parseInputtedBalanceAmount())
  }

  fun onBalanceAmountTextChanged(formattedAmount: String) {
    val amountToAdd: BigDecimal =
        try {
          CurrencyFormat.parse(
              formattedAmount, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } catch (_: ParseException) {
          0.toBigDecimal()
        }
    val balanceAfter: BigDecimal =
        _createCustomerViewModel.uiState.safeValue.balance.toBigDecimal() + amountToAdd
    _addBalanceState.setValue(
        _addBalanceState.safeValue.copy(
            // Revert back when trying to add larger than maximum allowed.
            formattedAmount =
                if (balanceAfter.compareTo(Long.MAX_VALUE.toBigDecimal()) > 0) {
                  _addBalanceState.safeValue.formattedAmount
                } else {
                  formattedAmount
                }))
  }

  fun onWithdrawBalanceDialogClosed() {
    _withdrawBalanceState.setValue(
        CustomerBalanceWithdrawState("", _createCustomerViewModel.uiState.safeValue.balance))
  }

  fun onWithdrawBalanceSubmitted() {
    _createCustomerViewModel.onBalanceChanged(
        _createCustomerViewModel.uiState.safeValue.balance - _parseInputtedWithdrawAmount())
  }

  fun onWithdrawAmountTextChanged(formattedAmount: String) {
    val amountToWithdraw: BigDecimal =
        try {
          CurrencyFormat.parse(
              formattedAmount, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } catch (_: ParseException) {
          0.toBigDecimal()
        }
    val balanceAfter: BigDecimal =
        _createCustomerViewModel.uiState.safeValue.balance.toBigDecimal() - amountToWithdraw
    val isLeftOverBalanceAvailable: Boolean = balanceAfter.compareTo(0.toBigDecimal()) >= 0
    _withdrawBalanceState.setValue(
        _withdrawBalanceState.safeValue.copy(
            // Revert back when when there's no more available balance to withdraw.
            formattedAmount =
                if (isLeftOverBalanceAvailable) formattedAmount
                else _withdrawBalanceState.safeValue.formattedAmount,
            availableAmountToWithdraw =
                if (isLeftOverBalanceAvailable) balanceAfter.toLong()
                else _withdrawBalanceState.safeValue.availableAmountToWithdraw))
  }

  private fun _parseInputtedBalanceAmount(): Long =
      try {
        CurrencyFormat.parse(
                _addBalanceState.safeValue.formattedAmount,
                AppCompatDelegate.getApplicationLocales().toLanguageTags())
            .toLong()
      } catch (_: ParseException) {
        0L
      }

  private fun _parseInputtedWithdrawAmount(): Long =
      try {
        CurrencyFormat.parse(
                _withdrawBalanceState.safeValue.formattedAmount,
                AppCompatDelegate.getApplicationLocales().toLanguageTags())
            .toLong()
      } catch (_: ParseException) {
        0L
      }
}
