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
import com.robifr.ledger.data.model.CustomerPaginatedInfo
import com.robifr.ledger.local.access.FakeCustomerDao
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

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
  private lateinit var _customerDao: FakeCustomerDao
  private lateinit var _viewModel: CustomerViewModel
  private lateinit var _uiEventObserver: Observer<CustomerEvent>

  private val _firstCustomer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 200L)
  private val _secondCustomer: CustomerModel =
      CustomerModel(id = 222L, name = "Ben", balance = 300L)
  private val _thirdCustomer: CustomerModel = CustomerModel(id = 333L, name = "Cal", balance = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerDao =
        FakeCustomerDao(
            data = mutableListOf(_firstCustomer, _secondCustomer, _thirdCustomer),
            queueData = mutableListOf(),
            productOrderData = mutableListOf())
    _customerRepository = spyk(CustomerRepository(_customerDao))
    _uiEventObserver = mockk(relaxed = true)
    _viewModel =
        CustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on initialize with empty data`(isTableEmpty: Boolean) {
    _customerDao.data.clear()
    if (!isTableEmpty) _customerDao.data.add(_firstCustomer)

    _viewModel =
        CustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        if (isTableEmpty) listOf()
                        else listOf(CustomerPaginatedInfo(_firstCustomer))),
            isNoCustomersAddedIllustrationVisible = isTableEmpty),
        _viewModel.uiState.safeValue,
        "Show illustration for no customers added")
  }

  @Test
  fun `on initialize with unordered name`() {
    _customerDao.data.clear()
    _customerDao.data.addAll(mutableListOf(_thirdCustomer, _firstCustomer, _secondCustomer))

    _viewModel =
        CustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository)
    assertEquals(
        listOf(_firstCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) },
        _viewModel.uiState.safeValue.pagination.paginatedItems,
        "Sort customers based from the default sort method")
  }

  @Test
  fun `on cleared`() {
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listener from the repository") {
      verify { _customerRepository.removeModelChangedListener(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on customer menu dialog shown`(isShown: Boolean) = runTest {
    _viewModel.onExpandedCustomerIndexChanged(0)
    advanceUntilIdle()
    _viewModel.onSortMethodChanged(CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false))
    _viewModel.onSortMethodDialogClosed()

    if (isShown) _viewModel.onCustomerMenuDialogShown(CustomerPaginatedInfo(_thirdCustomer))
    else _viewModel.onCustomerMenuDialogClosed()
    assertEquals(
        CustomerState(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) }),
            expandedCustomerIndex = 0,
            isCustomerMenuDialogShown = isShown,
            selectedCustomerMenu = if (isShown) CustomerPaginatedInfo(_thirdCustomer) else null,
            isNoCustomersAddedIllustrationVisible = false,
            sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false),
            isSortMethodDialogShown = false),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
  }

  @Test
  fun `on sort method changed with different sort method`() {
    val sortMethod: CustomerSortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, true)
    _viewModel.onSortMethodChanged(sortMethod)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdCustomer, _firstCustomer).map { CustomerPaginatedInfo(it) }),
            sortMethod = sortMethod),
        _viewModel.uiState.safeValue,
        "Sort customers based from the sorting method")
  }

  @Test
  fun `on sort method changed with same sort`() {
    _viewModel.onSortMethodChanged(CustomerSortMethod.SortBy.NAME)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) }),
            sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false)),
        _viewModel.uiState.safeValue,
        "Reverse sort order when selecting the same sort option")
  }

  @Test
  fun `on sort method changed with different sort`() {
    _viewModel.onSortMethodChanged(CustomerSortMethod.SortBy.BALANCE)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdCustomer, _firstCustomer).map { CustomerPaginatedInfo(it) }),
            sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, true)),
        _viewModel.uiState.safeValue,
        "Sort customers based from the sorting method")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on sort method dialog shown`(isShown: Boolean) = runTest {
    _viewModel.onExpandedCustomerIndexChanged(0)
    advanceUntilIdle()
    _viewModel.onCustomerMenuDialogClosed()
    _viewModel.onSortMethodChanged(CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false))

    if (isShown) _viewModel.onSortMethodDialogShown() else _viewModel.onSortMethodDialogClosed()
    assertEquals(
        CustomerState(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) }),
            expandedCustomerIndex = 0,
            isCustomerMenuDialogShown = false,
            selectedCustomerMenu = null,
            isNoCustomersAddedIllustrationVisible = false,
            sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false),
            isSortMethodDialogShown = isShown),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
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
  ) = runTest {
    _viewModel.onExpandedCustomerIndexChanged(oldIndex)
    advanceUntilIdle()

    _viewModel.onExpandedCustomerIndexChanged(newIndex)
    advanceUntilIdle()
    assertAll(
        {
          assertEquals(
              expandedIndex,
              _viewModel.uiState.safeValue.expandedCustomerIndex,
              "Update expanded customer index and reset when selecting the same one")
        },
        {
          assertEquals(
              RecyclerAdapterState.ItemChanged(updatedIndexes),
              _viewModel.uiEvent.safeValue.recyclerAdapter?.data,
              "Notify recycler adapter of item changes")
        })
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 111L])
  fun `on delete customer`(idToDelete: Long) {
    _viewModel.onDeleteCustomer(idToDelete)
    assertNotNull(
        _viewModel.uiEvent.safeValue.snackbar?.data, "Notify the delete result via snackbar")
  }

  @Test
  fun `on sync customer from database`() = runTest {
    val updatedCustomer: CustomerModel =
        _firstCustomer.copy(balance = _firstCustomer.balance + 100L)
    _customerRepository.update(updatedCustomer)
    assertEquals(
        listOf(updatedCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) },
        _viewModel.uiState.safeValue.pagination.paginatedItems,
        "Sync customers when any are updated in the database")
  }

  @Test
  fun `on sync customer from database result empty data`() = runTest {
    _customerRepository.delete(_firstCustomer.id)
    _customerRepository.delete(_secondCustomer.id)
    _customerRepository.delete(_thirdCustomer.id)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            pagination = _viewModel.uiState.safeValue.pagination.copy(paginatedItems = listOf()),
            isNoCustomersAddedIllustrationVisible = true),
        _viewModel.uiState.safeValue,
        "Show illustration for no customers created")
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    clearMocks(_uiEventObserver)
    _customerRepository.add(_firstCustomer.copy(id = null))
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod.sortBy)
    assertDoesNotThrow("Notify recycler adapter of dataset changes") {
      verify(exactly = 3) {
        _uiEventObserver.onChanged(
            match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
      }
    }
  }
}
