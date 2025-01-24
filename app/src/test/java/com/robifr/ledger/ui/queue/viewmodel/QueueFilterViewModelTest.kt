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
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.display.QueueSortMethod
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.QueueRepository
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class QueueFilterViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _queueViewModel: QueueViewModel
  private lateinit var _viewModel: QueueFilterViewModel

  private val _firstQueue: QueueModel =
      QueueModel(
          id = 111L,
          customerId = 111L,
          customer = CustomerModel(id = 111L, name = "Amy"),
          status = QueueModel.Status.IN_QUEUE,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders =
              listOf(
                  ProductOrderModel(
                      id = 111L,
                      queueId = 111L,
                      productId = 111L,
                      productName = "Apple",
                      productPrice = 100L,
                      quantity = 1.0)))
  private val _secondQueue: QueueModel =
      QueueModel(
          id = 222L,
          customerId = 222L,
          customer = CustomerModel(id = 222L, name = "Ben"),
          status = QueueModel.Status.IN_QUEUE,
          date = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders =
              listOf(
                  ProductOrderModel(
                      id = 222L,
                      queueId = 222L,
                      productId = 111L,
                      productName = "Apple",
                      productPrice = 100L,
                      quantity = 2.0)))
  private val _thirdQueue: QueueModel =
      QueueModel(
          id = 333L,
          customerId = null,
          status = QueueModel.Status.IN_QUEUE,
          date = LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders =
              listOf(
                  ProductOrderModel(
                      id = 333L,
                      queueId = 333L,
                      productId = 111L,
                      productName = "Apple",
                      productPrice = 100L,
                      quantity = 3.0)))

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    mockkStatic(Environment::class)
    _queueRepository = mockk()
    _customerRepository = mockk()

    every { _queueRepository.addModelChangedListener(any()) } just Runs
    every { _customerRepository.addModelChangedListener(any()) } just Runs
    every { Environment.isExternalStorageManager() } returns true
    coEvery { _queueRepository.selectAll() } returns listOf(_firstQueue, _secondQueue, _thirdQueue)
    coEvery { _queueRepository.isTableEmpty() } returns false
    _queueViewModel =
        QueueViewModel(
            _dispatcher = _dispatcher,
            _queueRepository = _queueRepository,
            _customerRepository = _customerRepository)
    _viewModel = _queueViewModel.filterView
  }

  @Test
  fun `on state changed`() {
    _viewModel.onDialogShown()
    _viewModel.onNullCustomerShown(false)
    _viewModel.onCustomerIdsChanged(listOf(111L))
    _viewModel.onDateChanged(QueueDate(QueueDate.Range.TODAY))
    _viewModel.onStatusChanged(setOf(QueueModel.Status.UNPAID))
    _viewModel.onMinTotalPriceTextChanged("$0")
    _viewModel.onMaxTotalPriceTextChanged("$1.00")
    assertEquals(
        QueueFilterState(
            isDialogShown = true,
            isNullCustomerShown = false,
            customerIds = listOf(111L),
            date = QueueDate(QueueDate.Range.TODAY),
            status = setOf(QueueModel.Status.UNPAID),
            formattedMinTotalPrice = "$0",
            formattedMaxTotalPrice = "$1.00"),
        _viewModel.uiState.safeValue,
        "Preserve all values except for the changed field")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on dialog shown`(isShown: Boolean) {
    _viewModel.onNullCustomerShown(false)
    _viewModel.onCustomerIdsChanged(listOf(111L))
    _viewModel.onDateChanged(QueueDate(QueueDate.Range.TODAY))
    _viewModel.onStatusChanged(setOf(QueueModel.Status.UNPAID))
    _viewModel.onMinTotalPriceTextChanged("$0")
    _viewModel.onMaxTotalPriceTextChanged("$1")

    if (isShown) _viewModel.onDialogShown() else _viewModel.onDialogClosed()
    assertEquals(
        QueueFilterState(
            isDialogShown = isShown,
            isNullCustomerShown = false,
            customerIds = listOf(111L),
            date = QueueDate(QueueDate.Range.TODAY),
            status = setOf(QueueModel.Status.UNPAID),
            formattedMinTotalPrice = "$0",
            formattedMaxTotalPrice = "$1"),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
  }

  @Test
  fun `on dialog closed with sorted queues`() {
    _queueViewModel.onSortMethodChanged(
        QueueSortMethod(QueueSortMethod.SortBy.CUSTOMER_NAME, false))
    _viewModel.onNullCustomerShown(false)

    _viewModel.onDialogClosed()
    assertEquals(
        listOf(_secondQueue, _firstQueue),
        _queueViewModel.uiState.safeValue.queues,
        "Apply filter to the queues while retaining the sorted list")
  }

  private fun `_on dialog closed with unbounded grand total price range cases`():
      Array<Array<Any>> =
      arrayOf(
          arrayOf("$0", "$0", "", "", listOf(_firstQueue, _secondQueue, _thirdQueue)),
          arrayOf("$0", "$0", "$2.00", "", listOf(_secondQueue, _thirdQueue)),
          arrayOf("$0", "$0", "", "$2.00", listOf(_firstQueue, _secondQueue)))

  @ParameterizedTest
  @MethodSource("_on dialog closed with unbounded grand total price range cases")
  fun `on dialog closed with unbounded grand total price range`(
      oldFormattedMinTotalPrice: String,
      oldFormattedMaxTotalPrice: String,
      newFormattedMinTotalPrice: String,
      newFormattedMaxTotalPrice: String,
      filteredQueues: List<QueueModel>
  ) {
    _viewModel.onMinTotalPriceTextChanged(oldFormattedMinTotalPrice)
    _viewModel.onMaxTotalPriceTextChanged(oldFormattedMaxTotalPrice)
    _viewModel.onDialogClosed()

    _viewModel.onMinTotalPriceTextChanged(newFormattedMinTotalPrice)
    _viewModel.onMaxTotalPriceTextChanged(newFormattedMaxTotalPrice)

    _viewModel.onDialogClosed()
    assertEquals(
        filteredQueues,
        _queueViewModel.uiState.safeValue.queues,
        "Include any queue whose grand total price falls within the unbounded range")
  }

  private fun `_on dialog closed with queue excluded from previous filter cases`():
      Array<Array<Any>> =
      arrayOf(
          // `_firstQueue` was previously excluded.
          arrayOf("$2.00", "", "", "", listOf(_firstQueue, _secondQueue, _thirdQueue)),
          // `_firstQueue` was previously excluded, but then exclude `_thirdQueue`.
          arrayOf("$2.00", "", "", "$2.00", listOf(_firstQueue, _secondQueue)))

  @ParameterizedTest
  @MethodSource("_on dialog closed with queue excluded from previous filter cases")
  fun `on dialog closed with queue excluded from previous filter`(
      oldFormattedMinTotalPrice: String,
      oldFormattedMaxTotalPrice: String,
      newFormattedMinTotalPrice: String,
      newFormattedMaxTotalPrice: String,
      filteredQueue: List<QueueModel>
  ) {
    _viewModel.onMinTotalPriceTextChanged(oldFormattedMinTotalPrice)
    _viewModel.onMaxTotalPriceTextChanged(oldFormattedMaxTotalPrice)
    _viewModel.onDialogClosed()

    _viewModel.onMinTotalPriceTextChanged(newFormattedMinTotalPrice)
    _viewModel.onMaxTotalPriceTextChanged(newFormattedMaxTotalPrice)

    _viewModel.onDialogClosed()
    assertEquals(
        filteredQueue,
        _queueViewModel.uiState.safeValue.queues,
        "Include queue from the database that match the filter")
  }
}
