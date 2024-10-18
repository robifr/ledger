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

package com.robifr.ledger.ui.editqueue.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.awaitValue
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.ui.SafeEvent
import com.robifr.ledger.ui.createqueue.viewmodel.CreateQueueState
import com.robifr.ledger.ui.editqueue.EditQueueFragment
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(
    InstantTaskExecutorExtension::class,
    MainCoroutineExtension::class,
    LifecycleOwnerExtension::class)
class EditQueueViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _resultStateObserver: Observer<SafeEvent<EditQueueResultState>>
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _viewModel: EditQueueViewModel

  private val _customer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 500L)
  private val _product: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)
  private val _productOrder: ProductOrderModel =
      ProductOrderModel(
          id = 111L,
          queueId = 111L,
          productId = _product.id,
          productName = _product.name,
          productPrice = _product.price,
          quantity = 1.0)
  private val _queueToEdit: QueueModel =
      QueueModel(
          id = 111L,
          customerId = _customer.id,
          customer = _customer,
          status = QueueModel.Status.COMPLETED,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE,
          productOrders = listOf(_productOrder))

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _resultStateObserver = mockk(relaxed = true)
    _queueRepository = mockk()

    every { _queueRepository.add(any()) } returns CompletableFuture.completedFuture(0L)
    every { _queueRepository.selectById(_queueToEdit.id) } returns
        CompletableFuture.completedFuture(_queueToEdit)
    _viewModel =
        EditQueueViewModel(
            _queueRepository,
            mockk(),
            mockk(),
            _dispatcher,
            SavedStateHandle().apply {
              set(EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key, _queueToEdit.id)
            })
    _viewModel.editResultState.observe(_lifecycleOwner, _resultStateObserver)
  }

  @Test
  fun `on initialize with arguments`() {
    assertEquals(
        CreateQueueState(
            customer = _queueToEdit.customer,
            temporalCustomer = _queueToEdit.customer,
            date = _queueToEdit.date.atZone(ZoneId.systemDefault()),
            status = _queueToEdit.status,
            paymentMethod = _queueToEdit.paymentMethod,
            allowedPaymentMethods =
                setOf(QueueModel.PaymentMethod.CASH, _queueToEdit.paymentMethod),
            productOrders = _queueToEdit.productOrders),
        _viewModel.uiState.safeValue,
        "The state should match the retrieved data based from the provided queue ID")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on update allowed payment methods`(isCustomerChanged: Boolean) {
    // Happens when the user edits a queue where all the customer balance has already been used
    // via account balance, making it appear to be less than the queue's grand total price.
    val initialCustomer: CustomerModel =
        _customer.copy(id = if (isCustomerChanged) 222L else _customer.id, balance = 0L)
    val initialQueue: QueueModel = _queueToEdit.copy(customer = initialCustomer)
    every { _queueRepository.selectById(_queueToEdit.id) } returns
        CompletableFuture.completedFuture(initialQueue)
    val viewModel: EditQueueViewModel =
        EditQueueViewModel(
            _queueRepository,
            mockk(),
            mockk(),
            _dispatcher,
            SavedStateHandle().apply {
              set(EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key, _queueToEdit.id)
            })

    // It's negative when the customer is changed, otherwise positive.
    val futureTemporalCustomerBalance: Long =
        initialCustomer.balanceOnUpdatedPayment(
            initialQueue,
            initialQueue.copy(paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE))
    assertEquals(
        if (futureTemporalCustomerBalance >= 0L) {
          setOf(QueueModel.PaymentMethod.CASH, QueueModel.PaymentMethod.ACCOUNT_BALANCE)
        } else {
          setOf(QueueModel.PaymentMethod.CASH)
        },
        viewModel.uiState.safeValue.allowedPaymentMethods,
        "Remove account balance payment option when the temporal balance will be negative")
  }

  @Test
  fun `on update temporal customer result updated balance`() {
    // Simplest way to ensure `CustomerModel#balanceOnUpdatedPayment()` gets called is by changing
    // the product orders total price, without altering any other fields. This will make the
    // temporal customer's balance deducted based on difference between two total prices.
    val editedQueue: QueueModel =
        _queueToEdit.copy(
            productOrders =
                _queueToEdit.productOrders.map {
                  it.copy(totalPrice = it.totalPrice.add(100.toBigDecimal()))
                })
    _viewModel.onProductOrdersChanged(editedQueue.productOrders)
    assertEquals(
        _customer.balanceOnUpdatedPayment(_queueToEdit, editedQueue),
        _viewModel.uiState.safeValue.temporalCustomer?.balance,
        "Update temporal customer's balance via `CustomerModel#balanceOnUpdatedPayment()`")
  }

  @Test
  fun `on update temporal customer result updated debt`() {
    // Same as balance, simplest way to ensure `CustomerModel#debtOnUpdatedPayment()` gets called
    // is by changing status to unpaid and product orders total price. This will make the temporal
    // customer's debt deducted based on difference between two total prices.
    val editedQueue: QueueModel =
        _queueToEdit.copy(
            status = QueueModel.Status.UNPAID,
            productOrders =
                _queueToEdit.productOrders.map {
                  it.copy(totalPrice = it.totalPrice.add(100.toBigDecimal()))
                })
    _viewModel.onStatusChanged(editedQueue.status)
    _viewModel.onProductOrdersChanged(editedQueue.productOrders)
    assertEquals(
        _customer.debtOnUpdatedPayment(_queueToEdit, editedQueue),
        _viewModel.uiState.safeValue.temporalCustomer?.debt,
        "Update temporal customer's debt via `CustomerModel#debtOnUpdatedPayment()`")
  }

  @Test
  fun `on save with empty product orders`() {
    _viewModel.onProductOrdersChanged(listOf())

    every { _queueRepository.update(any()) } returns CompletableFuture.completedFuture(0)
    _viewModel.onSave()
    assertDoesNotThrow("Prevent save for an empty product orders") {
      verify(exactly = 0) { _queueRepository.update(any()) }
    }
  }

  @Test
  fun `on save with edited queue result update operation`() {
    // Prevent save with add operation (parent class behavior) instead of update operation.
    every { _queueRepository.update(any()) } returns CompletableFuture.completedFuture(0)
    _viewModel.onSave()
    assertDoesNotThrow("Editing a queue shouldn't result in adding new data") {
      verify(exactly = 0) { _queueRepository.add(any()) }
      verify(exactly = 1) { _queueRepository.update(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `on save with edited queue`(effectedRows: Int) {
    every { _queueRepository.update(any()) } returns CompletableFuture.completedFuture(effectedRows)
    _viewModel.onSave()
    if (effectedRows == 0) {
      assertDoesNotThrow("Don't return result for a failed save") {
        verify(exactly = 0) { _resultStateObserver.onChanged(any()) }
      }
    } else {
      assertEquals(
          _queueToEdit.id,
          _viewModel.editResultState.awaitValue().valueIfNotHandled?.editedQueueId,
          "Return result with the correct ID after success save")
    }
  }
}
