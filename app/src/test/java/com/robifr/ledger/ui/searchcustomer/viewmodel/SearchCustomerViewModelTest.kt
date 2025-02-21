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

package com.robifr.ledger.ui.searchcustomer.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.local.access.FakeCustomerDao
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.search.viewmodel.SearchState
import com.robifr.ledger.ui.searchcustomer.SearchCustomerFragment
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coVerify
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
class SearchCustomerViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _customerDao: FakeCustomerDao
  private lateinit var _viewModel: SearchCustomerViewModel
  private lateinit var _uiEventObserver: Observer<SearchCustomerEvent>

  private val _customer: CustomerModel = CustomerModel(id = 111L, name = "Amy")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerDao =
        FakeCustomerDao(
            data = mutableListOf(_customer),
            queueData = mutableListOf(),
            productOrderData = mutableListOf())
    _customerRepository = spyk(CustomerRepository(_customerDao))
    _uiEventObserver = mockk(relaxed = true)

    _viewModel = SearchCustomerViewModel(SavedStateHandle(), _dispatcher, _customerRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on initialize with no arguments`() {
    assertEquals(
        SearchCustomerState(
            isSelectionEnabled = false,
            isToolbarVisible = true,
            initialQuery = "",
            query = "",
            initialSelectedCustomerIds = listOf(),
            customers = listOf(),
            expandedCustomerIndex = -1,
            isCustomerMenuDialogShown = false,
            selectedCustomerMenu = null),
        _viewModel.uiState.safeValue,
        "Apply the default state if no fragment arguments are provided")
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        SearchCustomerViewModel(
            SavedStateHandle().apply {
              set(SearchCustomerFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key(), true)
              set(SearchCustomerFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key(), false)
              set(SearchCustomerFragment.Arguments.INITIAL_QUERY_STRING.key(), "Amy")
              set(
                  SearchCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_IDS_LONG_ARRAY.key(),
                  longArrayOf(111L))
            },
            _dispatcher,
            _customerRepository)
    assertEquals(
        SearchCustomerState(
            isSelectionEnabled = true,
            isToolbarVisible = false,
            initialQuery = "Amy",
            query = "",
            initialSelectedCustomerIds = listOf(111L),
            customers = listOf(),
            expandedCustomerIndex = -1,
            isCustomerMenuDialogShown = false,
            selectedCustomerMenu = null),
        _viewModel.uiState.safeValue,
        "Match state with the retrieved data from the fragment arguments")
  }

  @Test
  fun `on cleared`() {
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listener from the repository") {
      verify { _customerRepository.removeModelChangedListener(any()) }
    }
  }

  @Test
  fun `on search with fast input`() = runTest {
    _viewModel.onSearch("A")
    _viewModel.onSearch("B")
    _viewModel.onSearch("C")
    advanceUntilIdle()
    assertDoesNotThrow("Prevent search from triggering multiple times when typing quickly") {
      coVerify(atMost = 1) { _customerRepository.search(any()) }
    }
  }

  @Test
  fun `on search with complete query`() = runTest {
    _viewModel.onSearch("A")
    advanceUntilIdle()
    assertEquals(
        _viewModel.uiState.safeValue.copy(query = "A", customers = listOf(_customer)),
        _viewModel.uiState.safeValue,
        "Update customers based from the queried search result")
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
  @ValueSource(booleans = [true, false])
  fun `on customer selected`(isCustomerNull: Boolean) {
    val customer: CustomerModel? = if (!isCustomerNull) _customer else null
    _viewModel.onCustomerSelected(customer)
    assertEquals(
        SearchCustomerResultState(customer?.id),
        _viewModel.uiEvent.safeValue.searchResult?.data,
        "Update result state based from the selected customer")
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 111L])
  fun `on delete customer`(idToDelete: Long) {
    _viewModel.onDeleteCustomer(idToDelete)
    assertNotNull(
        _viewModel.uiEvent.safeValue.snackbar?.data, "Notify the delete result via snackbar")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on customer menu dialog shown`(isShown: Boolean) = runTest {
    _viewModel =
        SearchCustomerViewModel(
            SavedStateHandle().apply {
              set(SearchCustomerFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key(), true)
              set(SearchCustomerFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key(), false)
              set(SearchCustomerFragment.Arguments.INITIAL_QUERY_STRING.key(), "Amy")
              set(
                  SearchCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_IDS_LONG_ARRAY.key(),
                  longArrayOf(111L))
            },
            _dispatcher,
            _customerRepository)
    _viewModel.onExpandedCustomerIndexChanged(0)
    advanceUntilIdle()

    if (isShown) _viewModel.onCustomerMenuDialogShown(_customer)
    else _viewModel.onCustomerMenuDialogClosed()
    assertEquals(
        SearchCustomerState(
            isSelectionEnabled = true,
            isToolbarVisible = false,
            initialQuery = "Amy",
            query = "",
            initialSelectedCustomerIds = listOf(111L),
            customers = listOf(),
            expandedCustomerIndex = 0,
            isCustomerMenuDialogShown = isShown,
            selectedCustomerMenu = if (isShown) _customer else null),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
  }

  @Test
  fun `on search ui state changed`() {
    val searchState: SearchState =
        SearchState(customers = listOf(_customer), products = listOf(), query = "A")
    _viewModel.onSearchUiStateChanged(searchState)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            customers = searchState.customers, query = searchState.query),
        _viewModel.uiState.safeValue,
        "Immediately update current state based from the search's UI state")
  }

  @Test
  fun `on sync customer from database`() = runTest {
    _viewModel.onSearch("Amy")
    advanceUntilIdle()

    val updatedCustomer: CustomerModel = _customer.copy(balance = _customer.balance + 100L)
    _customerRepository.update(updatedCustomer)
    assertEquals(
        listOf(updatedCustomer),
        _viewModel.uiState.safeValue.customers,
        "Sync customers when any are updated in the database")
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    clearMocks(_uiEventObserver)
    _customerRepository.add(_customer.copy(id = null))
    _viewModel.onSearch(_viewModel.uiState.safeValue.query)
    advanceUntilIdle()
    _viewModel.onSearchUiStateChanged(SearchState(listOf(), listOf(), ""))
    assertDoesNotThrow("Notify recycler adapter of dataset changes") {
      verify(exactly = 3) {
        _uiEventObserver.onChanged(
            match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
      }
    }
  }
}
