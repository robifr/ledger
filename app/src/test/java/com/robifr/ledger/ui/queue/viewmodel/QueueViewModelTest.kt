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

package com.robifr.ledger.ui.queue.viewmodel

import android.os.Environment
import androidx.lifecycle.Observer
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.display.QueueSortMethod
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelChangedListener
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
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
class QueueViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _customerRepository: CustomerRepository
  private val _queueChangedListenerCaptor: CapturingSlot<ModelSyncListener<QueueModel>> = slot()
  private val _customerChangedListenerCaptor: CapturingSlot<ModelChangedListener<CustomerModel>> =
      slot()
  private lateinit var _viewModel: QueueViewModel
  private lateinit var _uiEventObserver: Observer<QueueEvent>

  private val _productOrder: ProductOrderModel =
      ProductOrderModel(
          productId = 111L, productName = "Apple", productPrice = 100L, quantity = 1.0)
  private val _firstQueue: QueueModel =
      QueueModel(
          id = 111L,
          customerId = 111L,
          customer = CustomerModel(id = 111L, name = "Amy"),
          status = QueueModel.Status.IN_QUEUE,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders = listOf(_productOrder.copy(id = 111L, queueId = 111L)))
  private val _secondQueue: QueueModel =
      QueueModel(
          id = 222L,
          customerId = 222L,
          customer = CustomerModel(id = 222L, name = "Ben"),
          status = QueueModel.Status.IN_QUEUE,
          date = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders = listOf(_productOrder.copy(id = 222L, queueId = 222L)))
  private val _thirdQueue: QueueModel =
      QueueModel(
          id = 333L,
          customerId = null,
          status = QueueModel.Status.IN_QUEUE,
          date = LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders = listOf(_productOrder.copy(id = 333L, queueId = 333L)))

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    mockkStatic(Environment::class)
    _queueRepository = mockk()
    _customerRepository = mockk()
    _uiEventObserver = mockk(relaxed = true)

    every { _queueRepository.addModelChangedListener(capture(_queueChangedListenerCaptor)) } just
        Runs
    every {
      _customerRepository.addModelChangedListener(capture(_customerChangedListenerCaptor))
    } just Runs
    every { Environment.isExternalStorageManager() } returns true
    coEvery { _queueRepository.selectAll() } returns listOf(_firstQueue, _secondQueue, _thirdQueue)
    coEvery { _queueRepository.isTableEmpty() } returns false
    _viewModel =
        QueueViewModel(
            _dispatcher = _dispatcher,
            _queueRepository = _queueRepository,
            _customerRepository = _customerRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on initialize with requested storage permission`(isPermissionGranted: Boolean) {
    every { Environment.isExternalStorageManager() } returns isPermissionGranted
    _viewModel =
        QueueViewModel(
            _dispatcher = _dispatcher,
            _queueRepository = _queueRepository,
            _customerRepository = _customerRepository)
    assertEquals(
        if (isPermissionGranted) listOf(_firstQueue, _secondQueue, _thirdQueue) else listOf(),
        _viewModel.uiState.safeValue.queues,
        "Prevent to load all queues when storage permission is denied")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on initialize with empty data`(isTableEmpty: Boolean) {
    coEvery { _queueRepository.selectAll() } returns
        if (isTableEmpty) listOf() else listOf(_firstQueue)
    coEvery { _queueRepository.isTableEmpty() } returns isTableEmpty
    _viewModel =
        QueueViewModel(
            _dispatcher = _dispatcher,
            _queueRepository = _queueRepository,
            _customerRepository = _customerRepository)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            queues = if (isTableEmpty) listOf() else listOf(_firstQueue),
            isNoQueuesCreatedIllustrationVisible = isTableEmpty),
        _viewModel.uiState.safeValue,
        "Show illustration for no queues created")
  }

  @Test
  fun `on initialize with unordered date`() {
    coEvery { _queueRepository.selectAll() } returns listOf(_thirdQueue, _firstQueue, _secondQueue)
    _viewModel =
        QueueViewModel(
            _dispatcher = _dispatcher,
            _queueRepository = _queueRepository,
            _customerRepository = _customerRepository)
    assertEquals(
        listOf(_firstQueue, _secondQueue, _thirdQueue),
        _viewModel.uiState.safeValue.queues,
        "Sort queues based from the default sort method")
  }

  @Test
  fun `on cleared`() {
    every { _queueRepository.removeModelChangedListener(any()) } just Runs
    every { _customerRepository.removeModelChangedListener(any()) } just Runs
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listeners from the repository") {
      verify {
        _queueRepository.removeModelChangedListener(any())
        _customerRepository.removeModelChangedListener(any())
      }
    }
  }

  @Test
  fun `on queues changed with unsorted list`() {
    _viewModel.onQueuesChanged(listOf(_thirdQueue, _firstQueue, _secondQueue))
    assertEquals(
        _viewModel.uiState.safeValue.copy(queues = listOf(_firstQueue, _secondQueue, _thirdQueue)),
        _viewModel.uiState.safeValue,
        "Update queues with the new sorted list")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on queue menu dialog shown`(isShown: Boolean) {
    _viewModel.onQueuesChanged(listOf(_firstQueue))
    _viewModel.onExpandedQueueIndexChanged(-1)
    _viewModel.onSortMethodChanged(QueueSortMethod(QueueSortMethod.SortBy.DATE, true))
    _viewModel.onSortMethodDialogClosed()

    if (isShown) _viewModel.onQueueMenuDialogShown(_firstQueue)
    else _viewModel.onQueueMenuDialogClosed()
    assertEquals(
        QueueState(
            queues = listOf(_firstQueue),
            expandedQueueIndex = -1,
            isQueueMenuDialogShown = isShown,
            selectedQueueMenu = if (isShown) _firstQueue else null,
            isNoQueuesCreatedIllustrationVisible = false,
            sortMethod = QueueSortMethod(QueueSortMethod.SortBy.DATE, true),
            isSortMethodDialogShown = false),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
  }

  @Test
  fun `on sort method changed with different sort method`() {
    val sortMethod: QueueSortMethod = QueueSortMethod(QueueSortMethod.SortBy.CUSTOMER_NAME, false)
    _viewModel.onSortMethodChanged(sortMethod)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            queues = listOf(_thirdQueue, _secondQueue, _firstQueue), sortMethod = sortMethod),
        _viewModel.uiState.safeValue,
        "Sort queues based from the sorting method")
  }

  @Test
  fun `on sort method changed with same sort`() {
    _viewModel.onSortMethodChanged(QueueSortMethod.SortBy.DATE)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            queues = listOf(_thirdQueue, _secondQueue, _firstQueue),
            sortMethod = QueueSortMethod(QueueSortMethod.SortBy.DATE, true)),
        _viewModel.uiState.safeValue,
        "Reverse sort order when selecting the same sort option")
  }

  @Test
  fun `on sort method changed with different sort`() {
    _viewModel.onSortMethodChanged(QueueSortMethod.SortBy.CUSTOMER_NAME)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            queues = listOf(_thirdQueue, _secondQueue, _firstQueue),
            sortMethod = QueueSortMethod(QueueSortMethod.SortBy.CUSTOMER_NAME, false)),
        _viewModel.uiState.safeValue,
        "Sort queues based from the sorting method")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on sort method dialog shown`(isShown: Boolean) {
    _viewModel.onQueuesChanged(listOf(_firstQueue))
    _viewModel.onExpandedQueueIndexChanged(0)
    _viewModel.onQueueMenuDialogClosed()
    _viewModel.onSortMethodChanged(QueueSortMethod(QueueSortMethod.SortBy.DATE, true))

    if (isShown) _viewModel.onSortMethodDialogShown() else _viewModel.onSortMethodDialogClosed()
    assertEquals(
        QueueState(
            queues = listOf(_firstQueue),
            expandedQueueIndex = 0,
            isQueueMenuDialogShown = false,
            selectedQueueMenu = null,
            isNoQueuesCreatedIllustrationVisible = false,
            sortMethod = QueueSortMethod(QueueSortMethod.SortBy.DATE, true),
            isSortMethodDialogShown = isShown),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
  }

  private fun `_on expanded queue index changed cases`(): Array<Array<Any>> =
      arrayOf(
          // The updated indexes have +1 offset due to header holder.
          arrayOf(0, 0, listOf(1), -1),
          arrayOf(-1, 0, listOf(1), 0),
          arrayOf(0, 1, listOf(1, 2), 1))

  @ParameterizedTest
  @MethodSource("_on expanded queue index changed cases")
  fun `on expanded queue index changed`(
      oldIndex: Int,
      newIndex: Int,
      updatedIndexes: List<Int>,
      expandedIndex: Int
  ) {
    _viewModel.onExpandedQueueIndexChanged(oldIndex)

    _viewModel.onExpandedQueueIndexChanged(newIndex)
    assertAll(
        {
          assertEquals(
              expandedIndex,
              _viewModel.uiState.safeValue.expandedQueueIndex,
              "Update expanded queue index and reset when selecting the same one")
        },
        {
          assertEquals(
              RecyclerAdapterState.ItemChanged(updatedIndexes),
              _viewModel.uiEvent.safeValue.recyclerAdapter?.data,
              "Notify recycler adapter of item changes")
        })
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `on delete product`(effectedRows: Int) {
    coEvery { _queueRepository.delete(any()) } returns effectedRows
    _viewModel.onDeleteQueue(_firstQueue)
    assertNotNull(
        _viewModel.uiEvent.safeValue.snackbar?.data, "Notify the delete result via snackbar")
  }

  @Test
  fun `on sync queue from database`() {
    val updatedQueues: List<QueueModel> =
        listOf(_firstQueue.copy(status = QueueModel.Status.COMPLETED), _secondQueue, _thirdQueue)
    _queueChangedListenerCaptor.captured.onModelUpdated(updatedQueues)
    assertEquals(
        updatedQueues,
        _viewModel.uiState.safeValue.queues,
        "Sync queues when any are updated in the database")
  }

  @Test
  fun `on sync queue from database result empty data`() {
    coEvery { _queueRepository.isTableEmpty() } returns true

    _queueChangedListenerCaptor.captured.onModelDeleted(
        listOf(_firstQueue, _secondQueue, _thirdQueue))
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            queues = listOf(), isNoQueuesCreatedIllustrationVisible = true),
        _viewModel.uiState.safeValue,
        "Show illustration for no queues created")
  }

  @Test
  fun `on sync customer from database`() {
    val updatedCustomer: CustomerModel? =
        _firstQueue.customer?.let { it.copy(balance = it.balance + 100L) }
    _customerChangedListenerCaptor.captured.onModelUpdated(listOfNotNull(updatedCustomer))
    assertEquals(
        listOf(_firstQueue.copy(customer = updatedCustomer), _secondQueue, _thirdQueue),
        _viewModel.uiState.safeValue.queues,
        "Sync queues when any customer is updated in the database")
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() {
    clearMocks(_uiEventObserver)
    _queueChangedListenerCaptor.captured.onModelAdded(listOf(_firstQueue))
    _viewModel.onQueuesChanged(_viewModel.uiState.safeValue.queues)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod.sortBy)
    assertDoesNotThrow("Notify recycler adapter of dataset changes") {
      verify(exactly = 4) {
        _uiEventObserver.onChanged(
            match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
      }
    }
  }
}
