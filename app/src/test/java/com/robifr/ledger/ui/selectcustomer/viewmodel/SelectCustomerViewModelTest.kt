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

package com.robifr.ledger.ui.selectcustomer.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.selectcustomer.SelectCustomerFragment
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
class SelectCustomerViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _customerRepository: CustomerRepository
  private val _customerChangedListenerCaptor: CapturingSlot<ModelSyncListener<CustomerModel>> =
      slot()
  private lateinit var _viewModel: SelectCustomerViewModel
  private lateinit var _recyclerAdapterStateObserver: Observer<RecyclerAdapterState>

  private val _firstCustomer: CustomerModel = CustomerModel(id = 111L, name = "Amy")
  private val _secondCustomer: CustomerModel = CustomerModel(id = 222L, name = "Ben")
  private val _thirdCustomer: CustomerModel = CustomerModel(id = 333L, name = "Cal")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()
    _recyclerAdapterStateObserver = mockk(relaxed = true)

    every {
      _customerRepository.addModelChangedListener(capture(_customerChangedListenerCaptor))
    } just Runs
    coEvery { _customerRepository.selectAll() } returns
        listOf(_firstCustomer, _secondCustomer, _thirdCustomer)
    coEvery { _customerRepository.selectById(any<Long>()) } returns _firstCustomer
    _viewModel = SelectCustomerViewModel(SavedStateHandle(), _dispatcher, _customerRepository)
    _viewModel.recyclerAdapterState.observe(_lifecycleOwner, _recyclerAdapterStateObserver)
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        SelectCustomerViewModel(
            SavedStateHandle().apply {
              set(
                  SelectCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_PARCELABLE.key(),
                  _firstCustomer)
            },
            _dispatcher,
            _customerRepository)
    assertEquals(
        SelectCustomerState(
            initialSelectedCustomer = _firstCustomer,
            selectedCustomerOnDatabase = _firstCustomer,
            customers = listOf(_firstCustomer, _secondCustomer, _thirdCustomer),
            expandedCustomerIndex = -1,
            isSelectedCustomerPreviewExpanded = false),
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
    _viewModel = SelectCustomerViewModel(SavedStateHandle(), _dispatcher, _customerRepository)
    assertEquals(
        listOf(secondCustomer, thirdCustomer, firstCustomer),
        _viewModel.uiState.safeValue.customers,
        "Sort customers based from its name")
  }

  @Test
  fun `on initialize result notify recycler adapter item changes`() = runTest {
    assertDoesNotThrow("Notify recycler adapter of header holder changes") {
      verify { _recyclerAdapterStateObserver.onChanged(eq(RecyclerAdapterState.ItemChanged(0))) }
    }
  }

  @Test
  fun `on cleared`() {
    every { _customerRepository.removeModelChangedListener(any()) } just Runs
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listener from the repository") {
      verify { _customerRepository.removeModelChangedListener(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on selected customer preview expanded`(isExpanded: Boolean) {
    _viewModel.onSelectedCustomerPreviewExpanded(isExpanded)
    assertAll(
        {
          assertEquals(
              isExpanded,
              _viewModel.uiState.safeValue.isSelectedCustomerPreviewExpanded,
              "Update whether selected customer preview is expanded")
        },
        {
          assertDoesNotThrow("Notify recycler adapter of header holder changes") {
            verify {
              _recyclerAdapterStateObserver.onChanged(eq(RecyclerAdapterState.ItemChanged(0)))
            }
          }
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
          assertDoesNotThrow("Notify recycler adapter of item changes") {
            verify {
              _recyclerAdapterStateObserver.onChanged(
                  eq(RecyclerAdapterState.ItemChanged(updatedIndexes)))
            }
          }
        })
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on customer selected`(isCustomerNull: Boolean) {
    val customer: CustomerModel? = if (!isCustomerNull) _secondCustomer else null
    _viewModel.onCustomerSelected(customer)
    assertEquals(
        SelectCustomerResultState(customer?.id),
        _viewModel.resultState.value,
        "Update result state based from the selected customer")
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
    assertDoesNotThrow("Notify recycler adapter of dataset changes") {
      verify(exactly = 1) {
        _recyclerAdapterStateObserver.onChanged(eq(RecyclerAdapterState.DataSetChanged))
      }
    }
  }
}
