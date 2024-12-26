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

import androidx.lifecycle.Observer
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.display.CustomerSortMethod
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.SnackbarState
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(
    InstantTaskExecutorExtension::class,
    MainCoroutineExtension::class,
    LifecycleOwnerExtension::class)
class CustomerViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _customerRepository: CustomerRepository
  private val _customerChangedListenerCaptor: CapturingSlot<ModelSyncListener<CustomerModel>> =
      slot()
  private lateinit var _viewModel: CustomerViewModel
  private lateinit var _snackbarStateObserver: Observer<SnackbarState>
  private lateinit var _recyclerAdapterStateObserver: Observer<RecyclerAdapterState>

  private val _firstCustomer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 200L)
  private val _secondCustomer: CustomerModel =
      CustomerModel(id = 222L, name = "Ben", balance = 300L)
  private val _thirdCustomer: CustomerModel = CustomerModel(id = 333L, name = "Cal", balance = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()
    _snackbarStateObserver = mockk(relaxed = true)
    _recyclerAdapterStateObserver = mockk(relaxed = true)

    every {
      _customerRepository.addModelChangedListener(capture(_customerChangedListenerCaptor))
    } just Runs
    coEvery { _customerRepository.selectAll() } returns
        listOf(_firstCustomer, _secondCustomer, _thirdCustomer)
    _viewModel = CustomerViewModel(_dispatcher, _customerRepository)
    _viewModel.snackbarState.observe(_lifecycleOwner, _snackbarStateObserver)
    _viewModel.recyclerAdapterState.observe(_lifecycleOwner, _recyclerAdapterStateObserver)
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

  private fun `_on expanded customer index changed cases`(): Array<Array<Any>> =
      arrayOf(
          // The updated indexes have +1 offset due to header holder.
          arrayOf(0, 0, listOf(1), -1),
          arrayOf(-1, 0, listOf(1), 0),
          arrayOf(0, 1, listOf(1, 2), 1))

  @ParameterizedTest
  @MethodSource("_on expanded customer index changed cases")
  fun `on expanded customer index changed`(
      oldIndex: Int,
      newIndex: Int,
      updatedIndexes: List<Int>,
      expandedIndex: Int
  ) {
    _viewModel.onExpandedCustomerIndexChanged(oldIndex)

    _viewModel.onExpandedCustomerIndexChanged(newIndex)
    assertAll(
        {
          assertEquals(
              expandedIndex,
              _viewModel.uiState.safeValue.expandedCustomerIndex,
              "Update expanded customer index and reset when selecting the same one")
        },
        {
          assertDoesNotThrow("Notify recycler adapter of item changes") {
            verify {
              _recyclerAdapterStateObserver.onChanged(
                  eq(RecyclerAdapterState.ItemChanged(updatedIndexes)))
            }
          }
        })
  }

  @Test
  fun `on delete customer`() {
    coEvery { _customerRepository.delete(any()) } returns 1
    _viewModel.onDeleteCustomer(_firstCustomer)
    assertDoesNotThrow("Notify the delete result via snackbar") {
      verify { _snackbarStateObserver.onChanged(any()) }
    }
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

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() {
    clearMocks(_recyclerAdapterStateObserver)
    _customerChangedListenerCaptor.captured.onModelAdded(listOf(_firstCustomer))
    _viewModel.onCustomersChanged(_viewModel.uiState.safeValue.customers)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod.sortBy)
    assertDoesNotThrow("Notify recycler adapter of dataset changes") {
      verify(exactly = 4) {
        _recyclerAdapterStateObserver.onChanged(eq(RecyclerAdapterState.DataSetChanged))
      }
    }
  }
}
