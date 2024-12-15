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

package com.robifr.ledger.ui.filtercustomer.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.filtercustomer.FilterCustomerFragment
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
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
class FilterCustomerViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _viewModel: FilterCustomerViewModel

  private val _firstCustomer: CustomerModel = CustomerModel(id = 111L, name = "Amy")
  private val _secondCustomer: CustomerModel = CustomerModel(id = 222L, name = "Ben")
  private val _thirdCustomer: CustomerModel = CustomerModel(id = 333L, name = "Cal")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()

    coEvery { _customerRepository.selectAll() } returns
        listOf(_firstCustomer, _secondCustomer, _thirdCustomer)
    _viewModel = FilterCustomerViewModel(_dispatcher, _customerRepository, SavedStateHandle())
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        FilterCustomerViewModel(
            _dispatcher,
            _customerRepository,
            SavedStateHandle().apply {
              set(
                  FilterCustomerFragment.Arguments.INITIAL_FILTERED_CUSTOMER_IDS_LONG_ARRAY.key(),
                  listOfNotNull(_firstCustomer.id).toLongArray())
            })
    assertEquals(
        FilterCustomerState(
            customers = listOf(_firstCustomer, _secondCustomer, _thirdCustomer),
            expandedCustomerIndex = -1,
            filteredCustomers = listOf(_firstCustomer)),
        _viewModel.uiState.safeValue,
        "Match state with the retrieved data from the fragment argument")
  }

  @Test
  fun `on initialize with unordered name`() {
    val firstCustomer: CustomerModel = _firstCustomer.copy(name = "Cal")
    val secondCustomer: CustomerModel = _secondCustomer.copy(name = "Amy")
    val thirdCustomer: CustomerModel = _thirdCustomer.copy(name = "Ben")
    coEvery { _customerRepository.selectAll() } returns
        listOf(firstCustomer, secondCustomer, thirdCustomer)
    _viewModel = FilterCustomerViewModel(_dispatcher, _customerRepository, SavedStateHandle())
    assertEquals(
        listOf(secondCustomer, thirdCustomer, firstCustomer),
        _viewModel.uiState.safeValue.customers,
        "Sort customers based on their name")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on customer checked changed`(isSameCustomerChecked: Boolean) {
    _viewModel.onCustomerCheckedChanged(_firstCustomer)

    _viewModel.onCustomerCheckedChanged(
        if (isSameCustomerChecked) _firstCustomer else _secondCustomer)
    assertEquals(
        if (isSameCustomerChecked) listOf() else listOf(_firstCustomer, _secondCustomer),
        _viewModel.uiState.safeValue.filteredCustomers,
        "Add checked customer to the filtered customers and remove it when double checked")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on expanded customer index changed`(isSameIndexSelected: Boolean) {
    _viewModel.onExpandedCustomerIndexChanged(0)

    _viewModel.onExpandedCustomerIndexChanged(if (isSameIndexSelected) 0 else 1)
    assertEquals(
        if (isSameIndexSelected) -1 else 1,
        _viewModel.uiState.safeValue.expandedCustomerIndex,
        "Update expanded customer index and reset when selecting the same one")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on save`(isAnyCustomerFiltered: Boolean) {
    if (isAnyCustomerFiltered) _viewModel.onCustomerCheckedChanged(_firstCustomer)

    _viewModel.onSave()
    assertEquals(
        FilterCustomerResultState(
            if (isAnyCustomerFiltered) listOfNotNull(_firstCustomer.id) else listOf()),
        _viewModel.resultState.value,
        "Update result state based on the selected customer")
  }
}
