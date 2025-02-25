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
import com.robifr.ledger.data.model.CustomerPaginatedInfo
import com.robifr.ledger.local.access.FakeCustomerDao
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.selectcustomer.SelectCustomerFragment
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
class SelectCustomerViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _customerDao: FakeCustomerDao
  private lateinit var _viewModel: SelectCustomerViewModel
  private lateinit var _uiEventObserver: Observer<SelectCustomerEvent>

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
        SelectCustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            savedStateHandle = SavedStateHandle(),
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        SelectCustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            savedStateHandle =
                SavedStateHandle().apply {
                  set(
                      SelectCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_PARCELABLE.key(),
                      _firstCustomer)
                },
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Match state with the retrieved data from the fragment argument")
        .isEqualTo(
            SelectCustomerState(
                initialSelectedCustomer = _firstCustomer,
                selectedCustomerOnDatabase = _firstCustomer,
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_firstCustomer, _secondCustomer).map {
                              CustomerPaginatedInfo(it)
                            }),
                expandedCustomerIndex = -1,
                isSelectedCustomerPreviewExpanded = false))
  }

  @Test
  fun `on initialize with unordered name`() {
    _customerDao.data.clear()
    _customerDao.data.addAll(mutableListOf(_thirdCustomer, _firstCustomer, _secondCustomer))

    _viewModel =
        SelectCustomerViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            savedStateHandle = SavedStateHandle(),
            _dispatcher = _dispatcher,
            _customerRepository = _customerRepository)
    assertThat(_viewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Sort customers based from its name")
        .isEqualTo(listOf(_firstCustomer, _secondCustomer).map { CustomerPaginatedInfo(it) })
  }

  @Test
  fun `on initialize result notify recycler adapter item changes`() {
    assertThat(_viewModel.uiEvent.safeValue.recyclerAdapter?.data)
        .describedAs("Notify recycler adapter of header holder changes")
        .isEqualTo(RecyclerAdapterState.ItemChanged(0))
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
  fun `on selected customer preview expanded`(isExpanded: Boolean) {
    _viewModel.onSelectedCustomerPreviewExpanded(isExpanded)
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.isSelectedCustomerPreviewExpanded)
          .describedAs("Update whether selected customer preview is expanded")
          .isEqualTo(isExpanded)
      it.assertThat(_viewModel.uiEvent.safeValue.recyclerAdapter?.data)
          .describedAs("Notify recycler adapter of header holder changes")
          .isEqualTo(RecyclerAdapterState.ItemChanged(0))
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
  fun `on customer selected`(isCustomerNull: Boolean) {
    val customer: CustomerPaginatedInfo? =
        if (!isCustomerNull) CustomerPaginatedInfo(_secondCustomer) else null
    _viewModel.onCustomerSelected(customer)
    assertThat(_viewModel.uiEvent.safeValue.selectResult?.data)
        .describedAs("Update result state based from the selected customer")
        .isEqualTo(SelectCustomerResultState(customer?.id))
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
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    clearMocks(_uiEventObserver)
    _customerRepository.add(_firstCustomer.copy(id = null))
    assertThatCode {
          verify(exactly = 1) {
            _uiEventObserver.onChanged(
                match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
          }
        }
        .describedAs("Notify recycler adapter of dataset changes")
        .doesNotThrowAnyException()
  }
}
