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

import com.robifr.ledger.InstantTaskExecutorRuleForJUnit5
import com.robifr.ledger.MainCoroutineRule
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorRuleForJUnit5::class, MainCoroutineRule::class)
open class CustomerBalanceViewModelTest {
  protected open lateinit var _viewModel: CreateCustomerViewModel

  @BeforeEach
  open fun beforeEach() {
    _viewModel = CreateCustomerViewModel(mock())
  }

  @Test
  fun `on add dialog state changed`() {
    _viewModel.balanceView.onShowAddBalanceDialog()
    _viewModel.balanceView.onBalanceAmountTextChanged("$100")
    assertEquals(
        CustomerBalanceAddState(isDialogShown = true, formattedAmount = "$100"),
        _viewModel.balanceView.addBalanceState.safeValue,
        "Preserve all values except for the one changed")
  }

  @ParameterizedTest
  @ValueSource(longs = [500L, Long.MAX_VALUE])
  fun `on balance amount text changed`(currentBalance: Long) {
    val currentFormattedAmount: String = CurrencyFormat.format(0.toBigDecimal(), "")
    _viewModel.balanceView.onBalanceAmountTextChanged(currentFormattedAmount)
    _viewModel.onBalanceChanged(currentBalance)

    val amountToAdd: Long = 100
    val formattedAmountToAdd: String = CurrencyFormat.format(amountToAdd.toBigDecimal(), "")
    _viewModel.balanceView.onBalanceAmountTextChanged(formattedAmountToAdd)
    assertEquals(
        if (_viewModel.uiState.safeValue.balance
            .toBigDecimal()
            .add(amountToAdd.toBigDecimal())
            .compareTo(Long.MAX_VALUE.toBigDecimal()) > 0) {
          currentFormattedAmount
        } else {
          formattedAmountToAdd
        },
        _viewModel.balanceView.addBalanceState.safeValue.formattedAmount,
        "Revert the state if the added balance exceeds the maximum allowed")
  }

  @Test
  fun `on add dialog closed`() {
    assertEquals(
        CustomerBalanceAddState(isDialogShown = false, formattedAmount = ""),
        _viewModel.balanceView.addBalanceState.safeValue,
        "Reset the add balance state when dialog closes")
  }

  @Test
  fun `on add balance submitted`() {
    _viewModel.balanceView.onBalanceAmountTextChanged("$100")
    _viewModel.balanceView.onAddBalanceSubmitted()
    assertEquals(
        _viewModel.uiState.safeValue.copy(balance = 100L),
        _viewModel.uiState.safeValue,
        "Add balance to UI state by submitted amount after submission")
  }

  @Test
  fun `on withdraw dialog state changed`() {
    _viewModel.balanceView.onShowWithdrawBalanceDialog()
    _viewModel.balanceView.onWithdrawAmountTextChanged("$0")
    assertEquals(
        CustomerBalanceWithdrawState(
            isDialogShown = true, formattedAmount = "$0", availableAmountToWithdraw = 0L),
        _viewModel.balanceView.withdrawBalanceState.safeValue,
        "Preserve all values except for the one changed")
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 100L])
  fun `on withdraw amount text changed`(currentBalance: Long) {
    val currentFormattedAmount: String = CurrencyFormat.format(0.toBigDecimal(), "")
    _viewModel.onBalanceChanged(currentBalance)
    _viewModel.balanceView.onWithdrawAmountTextChanged(currentFormattedAmount)

    val withdrawAmount: Long = 100L
    val formattedAmountToReduce: String = CurrencyFormat.format(withdrawAmount.toBigDecimal(), "")
    val balanceAfter: BigDecimal =
        _viewModel.uiState.safeValue.balance.toBigDecimal().subtract(withdrawAmount.toBigDecimal())
    val isBalanceSufficient: Boolean = balanceAfter.compareTo(0.toBigDecimal()) >= 0
    _viewModel.balanceView.onWithdrawAmountTextChanged(formattedAmountToReduce)
    assertEquals(
        _viewModel.balanceView.withdrawBalanceState.safeValue.copy(
            formattedAmount =
                if (isBalanceSufficient) formattedAmountToReduce else currentFormattedAmount,
            availableAmountToWithdraw =
                if (isBalanceSufficient) balanceAfter.toLong() else currentBalance),
        _viewModel.balanceView.withdrawBalanceState.safeValue,
        "Revert the state if the balance to withdraw is insufficient")
  }

  @Test
  fun `on withdraw dialog closed`() {
    assertEquals(
        CustomerBalanceWithdrawState(
            isDialogShown = false, formattedAmount = "", availableAmountToWithdraw = 0L),
        _viewModel.balanceView.withdrawBalanceState.safeValue,
        "Reset the withdraw balance state when dialog closes")
  }

  @Test
  fun `on withdraw balance submitted`() {
    _viewModel.onBalanceChanged(100L)
    _viewModel.balanceView.onWithdrawAmountTextChanged("$100")
    _viewModel.balanceView.onWithdrawBalanceSubmitted()
    assertEquals(
        _viewModel.uiState.safeValue.copy(balance = 0L),
        _viewModel.uiState.safeValue,
        "Reduce balance in UI state by submitted amount after submission")
  }
}
