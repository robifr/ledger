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
import com.robifr.ledger.data.model.QueuePaginatedInfo
import com.robifr.ledger.local.TransactionProvider
import com.robifr.ledger.local.access.FakeCustomerDao
import com.robifr.ledger.local.access.FakeProductOrderDao
import com.robifr.ledger.local.access.FakeQueueDao
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductOrderRepository
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
class QueueViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _transactionProvider: TransactionProvider
  private val _withTransactionCaptor: CapturingSlot<suspend () -> Any> = slot()
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _queueDao: FakeQueueDao
  private lateinit var _productOrderRepository: ProductOrderRepository
  private lateinit var _productOrderDao: FakeProductOrderDao
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _customerDao: FakeCustomerDao
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
    _transactionProvider = mockk()
    _queueDao = FakeQueueDao(data = mutableListOf(_firstQueue, _secondQueue, _thirdQueue))
    _productOrderDao =
        FakeProductOrderDao(
            data = _queueDao.data.flatMap { it.productOrders }.toMutableList(),
            queueData = _queueDao.data)
    _customerDao =
        FakeCustomerDao(
            data = _queueDao.data.mapNotNull { it.customer }.toMutableList(),
            queueData = _queueDao.data,
            productOrderData = _productOrderDao.data)
    _customerRepository = spyk(CustomerRepository(_customerDao))
    _productOrderRepository = spyk(ProductOrderRepository(_productOrderDao))
    coEvery { _transactionProvider.withTransaction(capture(_withTransactionCaptor)) } coAnswers
        {
          _withTransactionCaptor.captured.invoke()
        }
    _queueRepository =
        spyk(
            QueueRepository(
                _queueDao, _transactionProvider, _customerRepository, _productOrderRepository))
    _uiEventObserver = mockk(relaxed = true)

    every { Environment.isExternalStorageManager() } returns true
    _viewModel =
        QueueViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
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
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _queueRepository = _queueRepository,
            _customerRepository = _customerRepository)
    assertEquals(
        if (isPermissionGranted) listOf(_firstQueue, _secondQueue).map { QueuePaginatedInfo(it) }
        else listOf(),
        _viewModel.uiState.safeValue.pagination.paginatedItems,
        "Prevent to load all queues when storage permission is denied")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on initialize with empty data`(isTableEmpty: Boolean) {
    _queueDao.data.clear()
    if (!isTableEmpty) _queueDao.data.add(_firstQueue)

    _viewModel =
        QueueViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _queueRepository = _queueRepository,
            _customerRepository = _customerRepository)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        if (isTableEmpty) listOf() else listOf(QueuePaginatedInfo(_firstQueue))),
            isNoQueuesCreatedIllustrationVisible = isTableEmpty),
        _viewModel.uiState.safeValue,
        "Show illustration for no queues created")
  }

  @Test
  fun `on initialize with unordered date`() {
    _queueDao.data.clear()
    _queueDao.data.addAll(mutableListOf(_thirdQueue, _firstQueue, _secondQueue))

    _viewModel =
        QueueViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _queueRepository = _queueRepository,
            _customerRepository = _customerRepository)
    assertEquals(
        listOf(_firstQueue, _secondQueue).map { QueuePaginatedInfo(it) },
        _viewModel.uiState.safeValue.pagination.paginatedItems,
        "Sort queues based from the default sort method")
  }

  @Test
  fun `on cleared`() {
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listeners from the repository") {
      verify {
        _queueRepository.removeModelChangedListener(any())
        _customerRepository.removeModelChangedListener(any())
      }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on queue menu dialog shown`(isShown: Boolean) = runTest {
    _viewModel.onExpandedQueueIndexChanged(0)
    advanceUntilIdle()
    _viewModel.onSortMethodChanged(QueueSortMethod(QueueSortMethod.SortBy.DATE, true))
    _viewModel.onSortMethodDialogClosed()

    if (isShown) _viewModel.onQueueMenuDialogShown(QueuePaginatedInfo(_firstQueue))
    else _viewModel.onQueueMenuDialogClosed()
    assertEquals(
        QueueState(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdQueue, _secondQueue).map { QueuePaginatedInfo(it) }),
            expandedQueueIndex = 0,
            expandedQueue = _firstQueue,
            isQueueMenuDialogShown = isShown,
            selectedQueueMenu = if (isShown) QueuePaginatedInfo(_firstQueue) else null,
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
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdQueue, _secondQueue).map { QueuePaginatedInfo(it) }),
            sortMethod = sortMethod),
        _viewModel.uiState.safeValue,
        "Sort queues based from the sorting method")
  }

  @Test
  fun `on sort method changed with same sort`() {
    _viewModel.onSortMethodChanged(QueueSortMethod.SortBy.DATE)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdQueue, _secondQueue).map { QueuePaginatedInfo(it) }),
            sortMethod = QueueSortMethod(QueueSortMethod.SortBy.DATE, true)),
        _viewModel.uiState.safeValue,
        "Reverse sort order when selecting the same sort option")
  }

  @Test
  fun `on sort method changed with different sort`() {
    _viewModel.onSortMethodChanged(QueueSortMethod.SortBy.CUSTOMER_NAME)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdQueue, _secondQueue).map { QueuePaginatedInfo(it) }),
            sortMethod = QueueSortMethod(QueueSortMethod.SortBy.CUSTOMER_NAME, false)),
        _viewModel.uiState.safeValue,
        "Sort queues based from the sorting method")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on sort method dialog shown`(isShown: Boolean) = runTest {
    _viewModel.onExpandedQueueIndexChanged(0)
    advanceUntilIdle()
    _viewModel.onQueueMenuDialogClosed()
    _viewModel.onSortMethodChanged(QueueSortMethod(QueueSortMethod.SortBy.DATE, true))

    if (isShown) _viewModel.onSortMethodDialogShown() else _viewModel.onSortMethodDialogClosed()
    assertEquals(
        QueueState(
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_thirdQueue, _secondQueue).map { QueuePaginatedInfo(it) }),
            expandedQueueIndex = 0,
            expandedQueue = _firstQueue,
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
  ) = runTest {
    _viewModel.onExpandedQueueIndexChanged(oldIndex)
    advanceUntilIdle()

    _viewModel.onExpandedQueueIndexChanged(newIndex)
    advanceUntilIdle()
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
  @ValueSource(longs = [0L, 111L])
  fun `on delete queue`(idToDelete: Long) {
    _viewModel.onDeleteQueue(idToDelete)
    assertNotNull(
        _viewModel.uiEvent.safeValue.snackbar?.data, "Notify the delete result via snackbar")
  }

  @Test
  fun `on sync queue from database`() = runTest {
    val updatedQueue: QueueModel = _firstQueue.copy(status = QueueModel.Status.COMPLETED)
    _queueRepository.update(updatedQueue)
    assertEquals(
        listOf(updatedQueue, _secondQueue).map { QueuePaginatedInfo(it) },
        _viewModel.uiState.safeValue.pagination.paginatedItems,
        "Sync queues when any are updated in the database")
  }

  @Test
  fun `on sync queue from database result empty data`() = runTest {
    _queueRepository.delete(_firstQueue.id)
    _queueRepository.delete(_secondQueue.id)
    _queueRepository.delete(_thirdQueue.id)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            pagination = _viewModel.uiState.safeValue.pagination.copy(paginatedItems = listOf()),
            isNoQueuesCreatedIllustrationVisible = true),
        _viewModel.uiState.safeValue,
        "Show illustration for no queues created")
  }

  @Test
  fun `on sync customer from database`() = runTest {
    val updatedCustomer: CustomerModel? =
        _firstQueue.customer?.let { it.copy(balance = it.balance + 100L) }
    updatedCustomer?.let { _customerRepository.update(it) }
    assertEquals(
        listOf(_firstQueue.copy(customer = updatedCustomer), _secondQueue).map {
          QueuePaginatedInfo(it)
        },
        _viewModel.uiState.safeValue.pagination.paginatedItems,
        "Sync queues when any customer is updated in the database")
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    clearMocks(_uiEventObserver)
    _queueRepository.add(_firstQueue.copy(id = null))
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod.sortBy)
    assertDoesNotThrow("Notify recycler adapter of dataset changes") {
      // The extra one is from notified customer during `_queueRepository.add()`.
      verify(exactly = 4) {
        _uiEventObserver.onChanged(
            match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
      }
    }
  }
}
