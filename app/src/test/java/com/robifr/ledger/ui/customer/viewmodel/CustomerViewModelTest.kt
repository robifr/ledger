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
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class CustomerViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _customerRepository: CustomerRepository
  private val _customerChangedListenerCaptor: CapturingSlot<ModelSyncListener<CustomerModel>> =
      slot()
  private lateinit var _viewModel: CustomerViewModel

  private val _firstCustomer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 200L)
  private val _secondCustomer: CustomerModel =
      CustomerModel(id = 222L, name = "Ben", balance = 300L)
  private val _thirdCustomer: CustomerModel = CustomerModel(id = 333L, name = "Cal", balance = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()

    every {
      _customerRepository.addModelChangedListener(capture(_customerChangedListenerCaptor))
    } just Runs
    coEvery { _customerRepository.selectAll() } returns
        listOf(_firstCustomer, _secondCustomer, _thirdCustomer)
    _viewModel = CustomerViewModel(_dispatcher, _customerRepository)
  }

  @Test
  fun `on initialize with unordered name`() {
    coEvery { _customerRepository.selectAll() } returns
        listOf(_thirdCustomer, _firstCustomer, _secondCustomer)
    _viewModel = CustomerViewModel(_dispatcher, _customerRepository)
    assertEquals(
        listOf(_firstCustomer, _secondCustomer, _thirdCustomer),
        _viewModel.uiState.safeValue.customers,
        "Sort customers based from the default sort method")
  }

  @Test
  fun `on cleared`() {
    every { _customerRepository.removeModelChangedListener(any()) } just Runs
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listener from the repository") {
      verify { _customerRepository.removeModelChangedListener(any()) }
    }
  }

  @Test
  fun `on customers changed with unsorted list`() {
    _viewModel.onCustomersChanged(listOf(_thirdCustomer, _firstCustomer, _secondCustomer))
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            customers = listOf(_firstCustomer, _secondCustomer, _thirdCustomer)),
        _viewModel.uiState.safeValue,
        "Update customers with the new sorted list")
  }

  @Test
  fun `on sort method changed with different sort method`() {
    val sortMethod: CustomerSortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, true)
    _viewModel.onSortMethodChanged(sortMethod)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            customers = listOf(_thirdCustomer, _firstCustomer, _secondCustomer),
            sortMethod = sortMethod),
        _viewModel.uiState.safeValue,
        "Sort customers based from the sorting method")
  }

  @Test
  fun `on sort method changed with same sort`() {
    _viewModel.onSortMethodChanged(CustomerSortMethod.SortBy.NAME)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            customers = listOf(_thirdCustomer, _secondCustomer, _firstCustomer),
            sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false)),
        _viewModel.uiState.safeValue,
        "Reverse sort order when selecting the same sort option")
  }

  @Test
  fun `on sort method changed with different sort`() {
    _viewModel.onSortMethodChanged(CustomerSortMethod.SortBy.BALANCE)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            customers = listOf(_thirdCustomer, _firstCustomer, _secondCustomer),
            sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, true)),
        _viewModel.uiState.safeValue,
        "Sort customers based from the sorting method")
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

  @Test
  fun `on sync customer from database`() {
    val updatedCustomers: List<CustomerModel> =
        listOf(
            _firstCustomer.copy(balance = _firstCustomer.balance + 100L),
            _secondCustomer,
            _thirdCustomer)
    _customerChangedListenerCaptor.captured.onModelUpdated(updatedCustomers)
    assertEquals(
        updatedCustomers,
        _viewModel.uiState.safeValue.customers,
        "Sync customers when any are updated in the database")
  }
}
