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
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.filtercustomer.FilterCustomerFragment
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
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
  private lateinit var _viewModel: FilterCustomerViewModel
  private lateinit var _uiEventObserver: Observer<FilterCustomerEvent>

  private val _firstCustomer: CustomerModel = CustomerModel(id = 111L, name = "Amy")
  private val _secondCustomer: CustomerModel = CustomerModel(id = 222L, name = "Ben")
  private val _thirdCustomer: CustomerModel = CustomerModel(id = 333L, name = "Cal")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()
    _uiEventObserver = mockk(relaxed = true)

    coEvery { _customerRepository.selectAll() } returns
        listOf(_firstCustomer, _secondCustomer, _thirdCustomer)
    _viewModel = FilterCustomerViewModel(_dispatcher, _customerRepository, SavedStateHandle())
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
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
    _viewModel.onCustomerCheckedChanged(oldCheckedCustomer)

    _viewModel.onCustomerCheckedChanged(newCheckedCustomer)
    assertAll(
        {
          assertEquals(
              checkedCustomers,
              _viewModel.uiState.safeValue.filteredCustomers,
              "Add checked customer to the filtered customers and remove it when double checked")
        },
        {
          assertEquals(
              RecyclerAdapterState.ItemChanged(recyclerItemIndexToUpdate),
              _viewModel.uiEvent.safeValue.recyclerAdapter?.data,
              "Notify recycler adapter of item changes")
        })
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
          assertEquals(
              RecyclerAdapterState.ItemChanged(updatedIndexes),
              _viewModel.uiEvent.safeValue.recyclerAdapter?.data,
              "Notify recycler adapter of item changes")
        })
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on save`(isAnyCustomerFiltered: Boolean) {
    if (isAnyCustomerFiltered) _viewModel.onCustomerCheckedChanged(_firstCustomer)

    _viewModel.onSave()
    assertEquals(
        FilterCustomerResultState(
            if (isAnyCustomerFiltered) listOfNotNull(_firstCustomer.id) else listOf()),
        _viewModel.uiEvent.safeValue.filterResult?.data,
        "Update result state based on the selected customer")
  }
}
