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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Show illustration for no customers added")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            if (isTableEmpty) listOf()
                            else listOf(CustomerPaginatedInfo(_firstCustomer))),
                isNoCustomersAddedIllustrationVisible = isTableEmpty))
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
    assertThat(_viewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Sort customers based from the default sort method")
        .isEqualTo(listOf(_firstCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) })
  }

  @Test
  fun `on cleared`() {
    _viewModel.onLifecycleOwnerDestroyed()
    assertThatCode { verify { _customerRepository.removeModelChangedListener(any()) } }
        .describedAs("Remove attached listener from the repository")
        .doesNotThrowAnyException()
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
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            CustomerState(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdCustomer, _secondCustomer).map {
                              CustomerPaginatedInfo(it)
                            }),
                expandedCustomerIndex = 0,
                isCustomerMenuDialogShown = isShown,
                selectedCustomerMenu = if (isShown) CustomerPaginatedInfo(_thirdCustomer) else null,
                isNoCustomersAddedIllustrationVisible = false,
                sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false),
                isSortMethodDialogShown = false))
  }

  @Test
  fun `on sort method changed with different sort method`() {
    val sortMethod: CustomerSortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, true)
    _viewModel.onSortMethodChanged(sortMethod)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Sort customers based from the sorting method")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdCustomer, _firstCustomer).map {
                              CustomerPaginatedInfo(it)
                            }),
                sortMethod = sortMethod))
  }

  @Test
  fun `on sort method changed with same sort`() {
    _viewModel.onSortMethodChanged(CustomerSortMethod.SortBy.NAME)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Reverse sort order when selecting the same sort option")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdCustomer, _secondCustomer).map {
                              CustomerPaginatedInfo(it)
                            }),
                sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false)))
  }

  @Test
  fun `on sort method changed with different sort`() {
    _viewModel.onSortMethodChanged(CustomerSortMethod.SortBy.BALANCE)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Sort customers based from the sorting method")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdCustomer, _firstCustomer).map {
                              CustomerPaginatedInfo(it)
                            }),
                sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, true)))
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on sort method dialog shown`(isShown: Boolean) = runTest {
    _viewModel.onExpandedCustomerIndexChanged(0)
    advanceUntilIdle()
    _viewModel.onCustomerMenuDialogClosed()
    _viewModel.onSortMethodChanged(CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false))

    if (isShown) _viewModel.onSortMethodDialogShown() else _viewModel.onSortMethodDialogClosed()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            CustomerState(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdCustomer, _secondCustomer).map {
                              CustomerPaginatedInfo(it)
                            }),
                expandedCustomerIndex = 0,
                isCustomerMenuDialogShown = false,
                selectedCustomerMenu = null,
                isNoCustomersAddedIllustrationVisible = false,
                sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, false),
                isSortMethodDialogShown = isShown))
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
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.expandedCustomerIndex)
          .describedAs("Update expanded customer index and reset when selecting the same one")
          .isEqualTo(expandedIndex)
      it.assertThat(_viewModel.uiEvent.safeValue.recyclerAdapter?.data)
          .describedAs("Notify recycler adapter of item changes")
          .isEqualTo(RecyclerAdapterState.ItemChanged(updatedIndexes))
    }
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 111L])
  fun `on delete customer`(idToDelete: Long) {
    _viewModel.onDeleteCustomer(idToDelete)
    assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
        .describedAs("Notify the delete result via snackbar")
        .isNotNull()
  }

  @Test
  fun `on sync customer from database`() = runTest {
    val updatedCustomer: CustomerModel =
        _firstCustomer.copy(balance = _firstCustomer.balance + 100L)
    _customerRepository.update(updatedCustomer)
    assertThat(_viewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Sync customers when any are updated in the database")
        .isEqualTo(listOf(updatedCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) })
  }

  @Test
  fun `on sync customer from database result empty data`() = runTest {
    _customerRepository.delete(_firstCustomer.id)
    _customerRepository.delete(_secondCustomer.id)
    _customerRepository.delete(_thirdCustomer.id)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Show illustration for no customers created")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(paginatedItems = listOf()),
                isNoCustomersAddedIllustrationVisible = true))
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    clearMocks(_uiEventObserver)
    _customerRepository.add(_firstCustomer.copy(id = null))
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod.sortBy)
    assertThatCode {
          verify(exactly = 3) {
            _uiEventObserver.onChanged(
                match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
          }
        }
        .describedAs("Notify recycler adapter of dataset changes")
        .doesNotThrowAnyException()
  }
}
