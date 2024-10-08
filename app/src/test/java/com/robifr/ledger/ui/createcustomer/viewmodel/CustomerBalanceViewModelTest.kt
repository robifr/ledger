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

import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import io.mockk.clearAllMocks
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
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
    _createCustomerViewModel = CreateCustomerViewModel(_dispatcher, mockk())
    _viewModel = _createCustomerViewModel.balanceView
  }

  @ParameterizedTest
  @ValueSource(longs = [500L, Long.MAX_VALUE])
  fun `on balance amount text changed`(currentBalance: Long) {
    val currentFormattedAmount: String = "$0"
    _viewModel.onBalanceAmountTextChanged(currentFormattedAmount)
    _createCustomerViewModel.onBalanceChanged(currentBalance)

    val amountToAdd: Long = 100L
    val formattedAmountToAdd: String = "$${amountToAdd}"
    _viewModel.onBalanceAmountTextChanged(formattedAmountToAdd)
    assertEquals(
        if ((currentBalance.toBigDecimal() + amountToAdd.toBigDecimal()).compareTo(
            Long.MAX_VALUE.toBigDecimal()) > 0) {
          currentFormattedAmount
        } else {
          formattedAmountToAdd
        },
        _viewModel.addBalanceState.safeValue.formattedAmount,
        "Revert the state if the added balance exceeds the maximum allowed")
  }

  @Test
  fun `on add dialog closed`() {
    _viewModel.onAddBalanceDialogClosed()
    assertEquals(
        CustomerBalanceAddState(formattedAmount = ""),
        _viewModel.addBalanceState.safeValue,
        "Reset the add balance state when dialog closes")
  }

  @Test
  fun `on add balance submitted`() {
    _viewModel.onBalanceAmountTextChanged("$100")
    _viewModel.onAddBalanceSubmitted()
    assertEquals(
        _createCustomerViewModel.uiState.safeValue.copy(balance = 100L),
        _createCustomerViewModel.uiState.safeValue,
        "Add balance to state by submitted amount after submission")
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 100L])
  fun `on withdraw amount text changed`(currentBalance: Long) {
    val currentFormattedAmount: String = "$0"
    _createCustomerViewModel.onBalanceChanged(currentBalance)
    _viewModel.onWithdrawAmountTextChanged(currentFormattedAmount)

    val withdrawAmount: Long = 100L
    val formattedAmountToReduce: String = "$${withdrawAmount}"
    val balanceAfter: BigDecimal = (currentBalance - withdrawAmount).toBigDecimal()
    val isBalanceSufficient: Boolean = balanceAfter.compareTo(0.toBigDecimal()) >= 0
    _viewModel.onWithdrawAmountTextChanged(formattedAmountToReduce)
    assertEquals(
        _viewModel.withdrawBalanceState.safeValue.copy(
            formattedAmount =
                if (isBalanceSufficient) formattedAmountToReduce else currentFormattedAmount,
            availableAmountToWithdraw =
                if (isBalanceSufficient) balanceAfter.toLong() else currentBalance),
        _viewModel.withdrawBalanceState.safeValue,
        "Revert the state if the balance to withdraw is insufficient")
  }

  @ParameterizedTest
  @ValueSource(longs = [0, 100L])
  fun `on withdraw dialog closed`(currentBalance: Long) {
    _createCustomerViewModel.onBalanceChanged(currentBalance)

    _viewModel.onWithdrawBalanceDialogClosed()
    assertEquals(
        CustomerBalanceWithdrawState(
            formattedAmount = "", availableAmountToWithdraw = currentBalance),
        _viewModel.withdrawBalanceState.safeValue,
        "Reset the withdraw balance state when dialog closes")
  }

  @Test
  fun `on withdraw balance submitted`() {
    _createCustomerViewModel.onBalanceChanged(100L)
    _viewModel.onWithdrawAmountTextChanged("$100")

    _viewModel.onWithdrawBalanceSubmitted()
    assertEquals(
        _createCustomerViewModel.uiState.safeValue.copy(balance = 0L),
        _createCustomerViewModel.uiState.safeValue,
        "Reduce balance in state by submitted amount after submission")
  }
}
