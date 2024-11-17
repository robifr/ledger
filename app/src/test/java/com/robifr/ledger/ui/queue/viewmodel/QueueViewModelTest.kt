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
import com.robifr.ledger.InstantTaskExecutorExtension
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
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class QueueViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _customerRepository: CustomerRepository
  private val _queueChangedListenerCaptor: CapturingSlot<ModelSyncListener<QueueModel>> = slot()
  private val _customerChangedListenerCaptor: CapturingSlot<ModelChangedListener<CustomerModel>> =
      slot()
  private lateinit var _viewModel: QueueViewModel

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

    every { _queueRepository.addModelChangedListener(capture(_queueChangedListenerCaptor)) } just
        Runs
    every {
      _customerRepository.addModelChangedListener(capture(_customerChangedListenerCaptor))
    } just Runs
    every { Environment.isExternalStorageManager() } returns true
    every { _queueRepository.selectAll() } returns
        CompletableFuture.completedFuture(listOf(_firstQueue, _secondQueue, _thirdQueue))
    _viewModel =
        QueueViewModel(
            _dispatcher = _dispatcher,
            _queueRepository = _queueRepository,
            _customerRepository = _customerRepository)
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

  @Test
  fun `on initialize with unordered date`() {
    every { _queueRepository.selectAll() } returns
        CompletableFuture.completedFuture(listOf(_thirdQueue, _firstQueue, _secondQueue))
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
  fun `on expanded queue index changed`(isSameIndexSelected: Boolean) {
    _viewModel.onExpandedQueueIndexChanged(0)

    _viewModel.onExpandedQueueIndexChanged(if (isSameIndexSelected) 0 else 1)
    assertEquals(
        if (isSameIndexSelected) -1 else 1,
        _viewModel.uiState.safeValue.expandedQueueIndex,
        "Update expanded queue index and reset when selecting the same one")
  }

  @Test
  fun `on sync queue from database`() {
    val updatedQueues: List<QueueModel> =
        listOf(_firstQueue.copy(status = QueueModel.Status.COMPLETED), _secondQueue, _thirdQueue)
    every { _queueRepository.notifyModelUpdated(any()) } answers
        {
          _queueChangedListenerCaptor.captured.onModelUpdated(updatedQueues)
        }
    _queueRepository.notifyModelUpdated(updatedQueues)
    assertEquals(
        updatedQueues,
        _viewModel.uiState.safeValue.queues,
        "Sync queues when any are updated in the database")
  }

  @Test
  fun `on sync customer from database`() {
    val updatedCustomer: CustomerModel? =
        _firstQueue.customer?.let { it.copy(balance = it.balance + 100L) }
    every { _customerRepository.notifyModelUpdated(any()) } answers
        {
          _customerChangedListenerCaptor.captured.onModelUpdated(listOfNotNull(updatedCustomer))
        }
    _customerRepository.notifyModelUpdated(listOfNotNull(updatedCustomer))
    assertEquals(
        listOf(_firstQueue.copy(customer = updatedCustomer), _secondQueue, _thirdQueue),
        _viewModel.uiState.safeValue.queues,
        "Sync queues when any customer is updated in the database")
  }
}
