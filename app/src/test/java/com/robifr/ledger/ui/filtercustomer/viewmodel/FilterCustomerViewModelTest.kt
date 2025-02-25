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

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.CustomerPaginatedInfo
import com.robifr.ledger.local.access.FakeCustomerDao
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.filtercustomer.FilterCustomerFragment
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
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
class FilterCustomerViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _customerDao: FakeCustomerDao
  private lateinit var _viewModel: FilterCustomerViewModel
  private lateinit var _uiEventObserver: Observer<FilterCustomerEvent>

  private val _firstCustomer: CustomerModel = CustomerModel(id = 111L, name = "Amy")
  private val _secondCustomer: CustomerModel = CustomerModel(id = 222L, name = "Ben")
  private val _thirdCustomer: CustomerModel = CustomerModel(id = 333L, name = "Cal")

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
        FilterCustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository,
            _savedStateHandle = SavedStateHandle())
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        FilterCustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository,
            _savedStateHandle =
                SavedStateHandle().apply {
                  set(
                      FilterCustomerFragment.Arguments.INITIAL_FILTERED_CUSTOMER_IDS_LONG_ARRAY
                          .key(),
                      listOfNotNull(_firstCustomer.id).toLongArray())
                })
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Match state with the retrieved data from the fragment argument")
        .isEqualTo(
            FilterCustomerState(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_firstCustomer, _secondCustomer).map {
                              CustomerPaginatedInfo(it)
                            }),
                expandedCustomerIndex = -1,
                filteredCustomers = listOf(CustomerPaginatedInfo(_firstCustomer))))
  }

  @Test
  fun `on initialize with unordered name`() {
    _customerDao.data.clear()
    _customerDao.data.addAll(mutableListOf(_thirdCustomer, _firstCustomer, _secondCustomer))

    _viewModel =
        FilterCustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository,
            _savedStateHandle = SavedStateHandle())
    assertThat(_viewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Sort customers based on their name")
        .isEqualTo(listOf(_firstCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) })
  }

  private fun `_on customer checked changed cases`(): Array<Array<Any>> =
      arrayOf(
          // spotless:off
          // The zero index (header holder) should always be updated. The customer index is also
          // has a +1 offset due to header holder.
          arrayOf(_firstCustomer, _firstCustomer, listOf(0, 1), listOf<CustomerModel>()),
          arrayOf(_firstCustomer, _secondCustomer, listOf(0, 2), listOf(_firstCustomer, _secondCustomer)))
          // spotless:on

  @ParameterizedTest
  @MethodSource("_on customer checked changed cases")
  fun `on customer checked changed`(
      oldCheckedCustomer: CustomerModel,
      newCheckedCustomer: CustomerModel,
      recyclerItemIndexToUpdate: List<Int>,
      checkedCustomers: List<CustomerModel>
  ) {
    _viewModel.onCustomerCheckedChanged(CustomerPaginatedInfo(oldCheckedCustomer))

    _viewModel.onCustomerCheckedChanged(CustomerPaginatedInfo(newCheckedCustomer))
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.filteredCustomers)
          .describedAs(
              "Add checked customer to the filtered customers and remove it when double checked")
          .isEqualTo(checkedCustomers.map { CustomerPaginatedInfo(it) })
      it.assertThat(_viewModel.uiEvent.safeValue.recyclerAdapter?.data)
          .describedAs("Notify recycler adapter of item changes")
          .isEqualTo(RecyclerAdapterState.ItemChanged(recyclerItemIndexToUpdate))
    }
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
  @ValueSource(booleans = [true, false])
  fun `on save`(isAnyCustomerFiltered: Boolean) {
    if (isAnyCustomerFiltered) {
      _viewModel.onCustomerCheckedChanged(CustomerPaginatedInfo(_firstCustomer))
    }

    _viewModel.onSave()
    assertThat(_viewModel.uiEvent.safeValue.filterResult?.data)
        .describedAs("Update result state based on the selected customer")
        .isEqualTo(
            FilterCustomerResultState(
                if (isAnyCustomerFiltered) listOfNotNull(_firstCustomer.id) else listOf()))
  }
}
