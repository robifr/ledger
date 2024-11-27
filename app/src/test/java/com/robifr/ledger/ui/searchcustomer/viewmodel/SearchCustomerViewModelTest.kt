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

import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.searchcustomer.SearchCustomerFragment
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class SearchCustomerViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _customerRepository: CustomerRepository
  private val _customerChangedListenerCaptor: CapturingSlot<ModelSyncListener<CustomerModel>> =
      slot()
  private lateinit var _viewModel: SearchCustomerViewModel

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()

    every {
      _customerRepository.addModelChangedListener(capture(_customerChangedListenerCaptor))
    } just Runs
    _viewModel = SearchCustomerViewModel(SavedStateHandle(), _dispatcher, _customerRepository)
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        SearchCustomerViewModel(
            SavedStateHandle().apply {
              set(SearchCustomerFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key, true)
              set(SearchCustomerFragment.Arguments.INITIAL_QUERY_STRING.key, "Amy")
              set(
                  SearchCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_IDS_LONG_ARRAY.key,
                  longArrayOf(111L))
            },
            _dispatcher,
            _customerRepository)
    assertEquals(
        SearchCustomerState(
            isSelectionEnabled = true,
            initialQuery = "Amy",
            query = "",
            initialSelectedCustomerIds = listOf(111L),
            customers = listOf(),
            expandedCustomerIndex = -1),
        _viewModel.uiState.safeValue,
        "Match state with the retrieved data from the fragment argument")
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
  fun `on search with fast input`() = runTest {
    every { _customerRepository.search(any()) } returns CompletableFuture.completedFuture(listOf())
    _viewModel.onSearch("A")
    _viewModel.onSearch("B")
    _viewModel.onSearch("C")
    advanceUntilIdle()
    assertDoesNotThrow("Prevent search from triggering multiple times when typing quickly") {
      verify(atMost = 1) { _customerRepository.search(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["A", "Amy", "Cal", "  "])
  fun `on search with complete query`(query: String) = runTest {
    val customers: List<CustomerModel> =
        if (query.contains("A", ignoreCase = true)) {
          listOf(CustomerModel(id = 111L, name = "Amy"), CustomerModel(id = 222L, name = "Cal"))
        } else {
          listOf()
        }
    every { _customerRepository.search(query) } returns CompletableFuture.completedFuture(customers)
    _viewModel.onSearch(query)
    advanceUntilIdle()
    assertEquals(
        _viewModel.uiState.safeValue.copy(query = query, customers = customers),
        _viewModel.uiState.safeValue,
        "Update customers based from the queried search result")
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
  fun `on customer selected`(isCustomerNull: Boolean) {
    val customer: CustomerModel? =
        if (!isCustomerNull) CustomerModel(id = 111L, name = "Amy") else null
    _viewModel.onCustomerSelected(customer)
    assertEquals(
        SearchCustomerResultState(customer?.id),
        _viewModel.resultState.value,
        "Update result state based from the selected customer")
  }

  @Test
  fun `on sync customer from database`() = runTest {
    val customer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 100L)
    every { _customerRepository.search(any()) } returns
        CompletableFuture.completedFuture(listOf(customer))
    _viewModel.onSearch("Amy")
    advanceUntilIdle()

    val updatedCustomer: CustomerModel = customer.copy(balance = customer.balance + 100L)
    every { _customerRepository.notifyModelUpdated(any()) } answers
        {
          _customerChangedListenerCaptor.captured.onModelUpdated(listOf(updatedCustomer))
        }
    _customerRepository.notifyModelUpdated(listOf(updatedCustomer))
    assertEquals(
        listOf(updatedCustomer),
        _viewModel.uiState.safeValue.customers,
        "Sync customers when any are updated in the database")
  }
}
