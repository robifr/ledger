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

import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.display.CustomerSortMethod
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.repository.CustomerRepository
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class CustomerFilterViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _customerViewModel: CustomerViewModel
  private lateinit var _viewModel: CustomerFilterViewModel

  private val _firstCustomer: CustomerModel =
      CustomerModel(id = 111L, name = "Amy", balance = 0L, debt = (-200).toBigDecimal())
  private val _secondCustomer: CustomerModel =
      CustomerModel(id = 222L, name = "Ben", balance = 100L, debt = (-100).toBigDecimal())
  private val _thirdCustomer: CustomerModel = CustomerModel(id = 333L, name = "Cal", balance = 200L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()

    every { _customerRepository.addModelChangedListener(any()) } just Runs
    every { _customerRepository.selectAll() } returns
        CompletableFuture.completedFuture(listOf(_firstCustomer, _secondCustomer, _thirdCustomer))
    _customerViewModel = CustomerViewModel(_dispatcher, _customerRepository)
    _viewModel = _customerViewModel.filterView
  }

  @Test
  fun `on state changed`() {
    _viewModel.onMinBalanceTextChanged("$0")
    _viewModel.onMaxBalanceTextChanged("$100")
    _viewModel.onMinDebtTextChanged("$0")
    _viewModel.onMaxDebtTextChanged("-$100")
    assertEquals(
        CustomerFilterState(
            formattedMinBalance = "$0",
            formattedMaxBalance = "$100",
            formattedMinDebt = "$0",
            formattedMaxDebt = "-$100"),
        _viewModel.uiState.safeValue,
        "Preserve all values except for the changed field")
  }

  @Test
  fun `on dialog closed with sorted customers`() {
    _customerViewModel.onSortMethodChanged(
        CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, false))
    _viewModel.onMinBalanceTextChanged("$100")
    _viewModel.onMaxBalanceTextChanged("$200")

    _viewModel.onDialogClosed()
    assertEquals(
        listOf(_thirdCustomer, _secondCustomer),
        _customerViewModel.uiState.safeValue.customers,
        "Apply filter to the customers while retaining the sorted list")
  }

  private fun `_on dialog closed with unbounded balance range cases`(): Array<Any> =
      arrayOf(
          arrayOf("", "", listOf(_firstCustomer, _secondCustomer, _thirdCustomer)),
          arrayOf("$100", "", listOf(_secondCustomer, _thirdCustomer)),
          arrayOf("", "$100", listOf(_firstCustomer, _secondCustomer)))

  @ParameterizedTest
  @MethodSource("_on dialog closed with unbounded balance range cases")
  fun `on dialog closed with unbounded balance range`(
      formattedMinBalance: String,
      formattedMaxBalance: String,
      filteredCustomers: List<CustomerModel>
  ) {
    // Simulate when the previous filter has any range applied.
    _viewModel.onMinBalanceTextChanged("$0")
    _viewModel.onMaxBalanceTextChanged("$0")
    _viewModel.onDialogClosed()

    _viewModel.onMinBalanceTextChanged(formattedMinBalance)
    _viewModel.onMaxBalanceTextChanged(formattedMaxBalance)

    _viewModel.onDialogClosed()
    assertEquals(
        filteredCustomers,
        _customerViewModel.uiState.safeValue.customers,
        "Include any customer whose balance falls within the unbounded range")
  }

  private fun `_on dialog closed with unbounded debt range cases`(): Array<Any> =
      arrayOf(
          arrayOf("", "", listOf(_firstCustomer, _secondCustomer, _thirdCustomer)),
          arrayOf("-$100", "", listOf(_firstCustomer, _secondCustomer)),
          arrayOf("$100", "", listOf(_firstCustomer, _secondCustomer)),
          arrayOf("", "-$100", listOf(_secondCustomer, _thirdCustomer)),
          arrayOf("", "$100", listOf(_secondCustomer, _thirdCustomer)))

  @ParameterizedTest
  @MethodSource("_on dialog closed with unbounded debt range cases")
  fun `on dialog closed with unbounded debt range`(
      formattedMinDebt: String,
      formattedMaxDebt: String,
      filteredCustomers: List<CustomerModel>
  ) {
    // Simulate when the previous filter has any range applied.
    _viewModel.onMinDebtTextChanged("$0")
    _viewModel.onMaxDebtTextChanged("$0")
    _viewModel.onDialogClosed()

    _viewModel.onMinDebtTextChanged(formattedMinDebt)
    _viewModel.onMaxDebtTextChanged(formattedMaxDebt)

    _viewModel.onDialogClosed()
    assertEquals(
        filteredCustomers,
        _customerViewModel.uiState.safeValue.customers,
        "Include any customer whose debt falls within the unbounded range")
  }

  private fun `_on dialog closed with customer excluded from previous filter cases`(): Array<Any> =
      arrayOf(
          // `_firstCustomer` was previously excluded.
          arrayOf("$100", "", "", "", listOf(_firstCustomer, _secondCustomer, _thirdCustomer)),
          // `_firstCustomer` was previously excluded, but then exclude `_thirdCustomer`.
          arrayOf("$100", "", "", "$100", listOf(_firstCustomer, _secondCustomer)))

  @ParameterizedTest
  @MethodSource("_on dialog closed with customer excluded from previous filter cases")
  fun `on dialog closed with customer excluded from previous filter`(
      oldFormattedMinBalance: String,
      oldFormattedMaxBalance: String,
      newFormattedMinBalance: String,
      newFormattedMaxBalance: String,
      filteredCustomers: List<CustomerModel>
  ) {
    _viewModel.onMinBalanceTextChanged(oldFormattedMinBalance)
    _viewModel.onMaxBalanceTextChanged(oldFormattedMaxBalance)
    _viewModel.onDialogClosed()

    _viewModel.onMinBalanceTextChanged(newFormattedMinBalance)
    _viewModel.onMaxBalanceTextChanged(newFormattedMaxBalance)

    _viewModel.onDialogClosed()
    assertEquals(
        filteredCustomers,
        _customerViewModel.uiState.safeValue.customers,
        "Include customer from the database that match the filter")
  }
}
