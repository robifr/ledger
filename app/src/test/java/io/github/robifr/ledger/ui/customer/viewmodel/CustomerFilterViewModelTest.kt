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

package io.github.robifr.ledger.ui.customer.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.robifr.ledger.InstantTaskExecutorExtension
import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.display.CustomerSortMethod
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.CustomerPaginatedInfo
import io.github.robifr.ledger.local.access.FakeCustomerDao
import io.github.robifr.ledger.repository.CustomerRepository
import io.mockk.clearAllMocks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class CustomerFilterViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _customerDao: FakeCustomerDao
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
    _customerDao =
        FakeCustomerDao(
            data = mutableListOf(_firstCustomer, _secondCustomer, _thirdCustomer),
            queueData = mutableListOf(),
            productOrderData = mutableListOf())
    _customerRepository = CustomerRepository(_customerDao)
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en-US"))

    _customerViewModel =
        CustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository)
    _viewModel = _customerViewModel.filterView
  }

  @Test
  fun `on state changed`() {
    _viewModel.onDialogShown()
    _viewModel.onMinBalanceTextChanged("$0")
    _viewModel.onMaxBalanceTextChanged("$1.00")
    _viewModel.onMinDebtTextChanged("$0")
    _viewModel.onMaxDebtTextChanged("-$1.00")
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve all values except for the changed field")
        .isEqualTo(
            CustomerFilterState(
                isDialogShown = true,
                formattedMinBalance = "$0",
                formattedMaxBalance = "$1.00",
                formattedMinDebt = "$0",
                formattedMaxDebt = "-$1.00"))
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on dialog shown`(isShown: Boolean) {
    _viewModel.onMinBalanceTextChanged("$0")
    _viewModel.onMaxBalanceTextChanged("$1")
    _viewModel.onMinDebtTextChanged("$0")
    _viewModel.onMaxDebtTextChanged("-$1")

    if (isShown) _viewModel.onDialogShown() else _viewModel.onDialogClosed()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            CustomerFilterState(
                isDialogShown = isShown,
                formattedMinBalance = "$0",
                formattedMaxBalance = "$1",
                formattedMinDebt = "$0",
                formattedMaxDebt = "-$1"))
  }

  @Test
  fun `on dialog closed with sorted customers`() {
    _customerViewModel.onSortMethodChanged(
        CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, false))
    _viewModel.onMinBalanceTextChanged("$1.00")
    _viewModel.onMaxBalanceTextChanged("$2.00")

    _viewModel.onDialogClosed()
    assertThat(_customerViewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Apply filter to the customers while retaining the sorted list")
        .isEqualTo(listOf(_thirdCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) })
  }

  private fun `_on dialog closed with unbounded balance range cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf("$0", "$0", "", "", listOf(_firstCustomer, _secondCustomer)),
          arrayOf("$0", "$0", "$1.00", "", listOf(_secondCustomer, _thirdCustomer)),
          arrayOf("$0", "$0", "", "$1.00", listOf(_firstCustomer, _secondCustomer)))

  @ParameterizedTest
  @MethodSource("_on dialog closed with unbounded balance range cases")
  fun `on dialog closed with unbounded balance range`(
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
    assertThat(_customerViewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Include any customer whose balance falls within the unbounded range")
        .isEqualTo(filteredCustomers.map { CustomerPaginatedInfo(it) })
  }

  private fun `_on dialog closed with unbounded debt range cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf("$0", "$0", "", "", listOf(_firstCustomer, _secondCustomer)),
          arrayOf("$0", "$0", "-$1.00", "", listOf(_firstCustomer, _secondCustomer)),
          arrayOf("$0", "$0", "$1.00", "", listOf(_firstCustomer, _secondCustomer)),
          arrayOf("$0", "$0", "", "-$1.00", listOf(_secondCustomer, _thirdCustomer)),
          arrayOf("$0", "$0", "", "$1.00", listOf(_secondCustomer, _thirdCustomer)))

  @ParameterizedTest
  @MethodSource("_on dialog closed with unbounded debt range cases")
  fun `on dialog closed with unbounded debt range`(
      oldFormattedMinDebt: String,
      oldFormattedMaxDebt: String,
      newFormattedMinDebt: String,
      newFormattedMaxDebt: String,
      filteredCustomers: List<CustomerModel>
  ) {
    _viewModel.onMinDebtTextChanged(oldFormattedMinDebt)
    _viewModel.onMaxDebtTextChanged(oldFormattedMaxDebt)
    _viewModel.onDialogClosed()

    _viewModel.onMinDebtTextChanged(newFormattedMinDebt)
    _viewModel.onMaxDebtTextChanged(newFormattedMaxDebt)

    _viewModel.onDialogClosed()
    assertThat(_customerViewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Include any customer whose debt falls within the unbounded range")
        .isEqualTo(filteredCustomers.map { CustomerPaginatedInfo(it) })
  }

  private fun `_on dialog closed with customer excluded from previous filter cases`():
      Array<Array<Any>> =
      arrayOf(
          // `_firstCustomer` was previously excluded.
          arrayOf("$1.00", "", "", "", listOf(_firstCustomer, _secondCustomer)),
          // `_firstCustomer` was previously excluded, but then exclude `_secondCustomer`.
          arrayOf("$1.00", "", "", "$0.50", listOf(_firstCustomer)))

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
    assertThat(_customerViewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Include customer from the database that match the filter")
        .isEqualTo(filteredCustomers.map { CustomerPaginatedInfo(it) })
  }
}
