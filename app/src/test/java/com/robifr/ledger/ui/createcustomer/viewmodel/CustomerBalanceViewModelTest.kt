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
import androidx.core.os.LocaleListCompat
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.util.CurrencyFormat
import io.mockk.clearAllMocks
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class CustomerBalanceViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _createCustomerViewModel: CreateCustomerViewModel
  private lateinit var _viewModel: CustomerBalanceViewModel

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en-US"))
    _createCustomerViewModel = CreateCustomerViewModel(_dispatcher, mockk())
    _viewModel = _createCustomerViewModel.balanceView
  }

  @ParameterizedTest
  @ValueSource(longs = [500L, Long.MAX_VALUE])
  fun `on add balance amount text changed`(currentBalance: Long) {
    val currentFormattedAmount: String = "$0"
    _viewModel.onAddBalanceAmountTextChanged(currentFormattedAmount)
    _createCustomerViewModel.onBalanceChanged(currentBalance)

    val amountToAdd: Long = 100L
    val formattedAmountToAdd: String = "$1.00"
    _viewModel.onAddBalanceAmountTextChanged(formattedAmountToAdd)
    assertThat(_viewModel.addBalanceState.safeValue.formattedAmount)
        .describedAs("Revert the state if the added balance exceeds the maximum allowed")
        .isEqualTo(
            if ((currentBalance.toBigDecimal() + amountToAdd.toBigDecimal()).compareTo(
                Long.MAX_VALUE.toBigDecimal()) > 0) {
              currentFormattedAmount
            } else {
              formattedAmountToAdd
            })
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 500L])
  fun `on add balance amount text changed result add button enabled`(amount: Long) {
    _viewModel.onAddBalanceAmountTextChanged(CurrencyFormat.format(amount.toBigDecimal(), "en-US"))
    assertThat(_viewModel.addBalanceState.safeValue.isAddButtonEnabled)
        .describedAs("Enable add button when the amount is more than zero")
        .isEqualTo(amount > 0)
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on add dialog shown`(isShown: Boolean) {
    _viewModel.onAddBalanceAmountTextChanged("$1")

    if (isShown) _viewModel.onAddBalanceDialogShown() else _viewModel.onAddBalanceDialogClosed()
    assertThat(_viewModel.addBalanceState.safeValue)
        .describedAs("Preserve other fields when the dialog shown and reset when closed")
        .isEqualTo(
            if (isShown) {
              _viewModel.addBalanceState.safeValue.copy(isDialogShown = true)
            } else {
              CustomerBalanceAddState(
                  isDialogShown = false, formattedAmount = "", isAddButtonEnabled = false)
            })
  }

  @Test
  fun `on add balance submitted`() {
    _viewModel.onAddBalanceAmountTextChanged("$1.00")
    _viewModel.onAddBalanceSubmitted()
    assertThat(_createCustomerViewModel.uiState.safeValue)
        .describedAs("Add balance to state by submitted amount after submission")
        .isEqualTo(_createCustomerViewModel.uiState.safeValue.copy(balance = 100L))
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 100L])
  fun `on withdraw amount text changed`(currentBalance: Long) {
    val currentFormattedAmount: String = "$0"
    _createCustomerViewModel.onBalanceChanged(currentBalance)
    _viewModel.onWithdrawAmountTextChanged(currentFormattedAmount)

    val withdrawAmount: Long = 100L
    val formattedAmountToReduce: String = "$1.00"
    val balanceAfter: BigDecimal = (currentBalance - withdrawAmount).toBigDecimal()
    val isBalanceSufficient: Boolean = balanceAfter.compareTo(0.toBigDecimal()) >= 0
    _viewModel.onWithdrawAmountTextChanged(formattedAmountToReduce)
    assertThat(_viewModel.withdrawBalanceState.safeValue)
        .describedAs("Revert the state if the balance to withdraw is insufficient")
        .isEqualTo(
            _viewModel.withdrawBalanceState.safeValue.copy(
                formattedAmount =
                    if (isBalanceSufficient) formattedAmountToReduce else currentFormattedAmount,
                availableAmountToWithdraw =
                    if (isBalanceSufficient) balanceAfter.toLong() else currentBalance))
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 500L])
  fun `on withdraw balance amount text changed result withdraw button enabled`(amount: Long) {
    _viewModel.onWithdrawAmountTextChanged(CurrencyFormat.format(amount.toBigDecimal(), "en-US"))
    assertThat(_viewModel.withdrawBalanceState.safeValue.isWithdrawButtonEnabled)
        .describedAs("Enable withdraw button when the amount is more than zero")
        .isEqualTo(amount > 0)
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on withdraw dialog shown`(isShown: Boolean) {
    _viewModel.onWithdrawAmountTextChanged("$100")
    _createCustomerViewModel.onBalanceChanged(100L)

    if (isShown) _viewModel.onWithdrawBalanceDialogShown()
    else _viewModel.onWithdrawBalanceDialogClosed()
    assertThat(_viewModel.withdrawBalanceState.safeValue)
        .describedAs("Preserve other fields when the dialog shown and reset when closed")
        .isEqualTo(
            if (isShown) {
              _viewModel.withdrawBalanceState.safeValue.copy(isDialogShown = true)
            } else {
              CustomerBalanceWithdrawState(
                  isDialogShown = false,
                  formattedAmount = "",
                  availableAmountToWithdraw = 100L,
                  isWithdrawButtonEnabled = false)
            })
  }

  @Test
  fun `on withdraw balance submitted`() {
    _createCustomerViewModel.onBalanceChanged(100L)
    _viewModel.onWithdrawAmountTextChanged("$1.00")

    _viewModel.onWithdrawBalanceSubmitted()
    assertThat(_createCustomerViewModel.uiState.safeValue)
        .describedAs("Reduce balance in state by submitted amount after submission")
        .isEqualTo(_createCustomerViewModel.uiState.safeValue.copy(balance = 0L))
  }
}
