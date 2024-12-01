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

package com.robifr.ledger.ui.createqueue.viewmodel

import androidx.lifecycle.Observer
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.repository.QueueRepository
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
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(
    InstantTaskExecutorExtension::class,
    MainCoroutineExtension::class,
    LifecycleOwnerExtension::class)
class CreateQueueViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _productRepository: ProductRepository
  private lateinit var _viewModel: CreateQueueViewModel
  private lateinit var _resultStateObserver: Observer<CreateQueueResultState>

  private val _customer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 500L)
  private val _product: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)
  private val _queue: QueueModel =
      QueueModel(
          customerId = _customer.id,
          customer = _customer,
          status = QueueModel.Status.IN_QUEUE,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders =
              listOf(
                  ProductOrderModel(
                      productId = _product.id,
                      productName = _product.name,
                      productPrice = _product.price,
                      quantity = 1.0)))

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _queueRepository = mockk()
    _customerRepository = mockk()
    _productRepository = mockk()
    _resultStateObserver = mockk(relaxed = true)
    _viewModel =
        CreateQueueViewModel(_dispatcher, _queueRepository, _customerRepository, _productRepository)
    _viewModel.resultState.observe(_lifecycleOwner, _resultStateObserver)
  }

  @Test
  fun `on state changed`() {
    _viewModel.onCustomerChanged(_customer)
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onStatusChanged(_queue.status)
    _viewModel.onPaymentMethodChanged(_queue.paymentMethod)
    _viewModel.onProductOrdersChanged(_queue.productOrders)
    assertEquals(
        CreateQueueState(
            customer = _customer,
            temporalCustomer = _customer,
            date = _queue.date.atZone(ZoneId.systemDefault()),
            status = _queue.status,
            paymentMethod = _queue.paymentMethod,
            allowedPaymentMethods = setOf(_queue.paymentMethod),
            productOrders = _queue.productOrders),
        _viewModel.uiState.safeValue,
        "Preserve all values except for the changed field")
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 500L])
  fun `on customer changed result updates other fields`(balance: Long) {
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onProductOrdersChanged(_queue.productOrders)
    _viewModel.onStatusChanged(QueueModel.Status.COMPLETED)
    _viewModel.onPaymentMethodChanged(QueueModel.PaymentMethod.ACCOUNT_BALANCE)

    _viewModel.onCustomerChanged(_customer.copy(balance = balance))
    assertAll(
        {
          assertEquals(
              _customer.copy(balance = if (balance == 500L) 400L else balance),
              _viewModel.uiState.safeValue.temporalCustomer,
              "Temporal customer must be updated when the customer change")
        },
        {
          assertEquals(
              if (balance == 0L) setOf(QueueModel.PaymentMethod.CASH)
              else setOf(QueueModel.PaymentMethod.CASH, QueueModel.PaymentMethod.ACCOUNT_BALANCE),
              _viewModel.uiState.safeValue.allowedPaymentMethods,
              "Allowed payment methods must be updated when the customer change")
        },
        {
          assertEquals(
              if (balance == 0L) QueueModel.PaymentMethod.CASH
              else QueueModel.PaymentMethod.ACCOUNT_BALANCE,
              _viewModel.uiState.safeValue.paymentMethod,
              "Fallback payment method to cash when the customer balance is insufficient")
        })
  }

  @ParameterizedTest
  @EnumSource(QueueModel.Status::class)
  fun `on status changed result updates other fields`(status: QueueModel.Status) {
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onCustomerChanged(_customer)
    _viewModel.onProductOrdersChanged(_queue.productOrders)
    _viewModel.onPaymentMethodChanged(QueueModel.PaymentMethod.ACCOUNT_BALANCE)

    _viewModel.onStatusChanged(status)
    assertAll(
        {
          assertEquals(
              _customer.copy(
                  balance = if (status == QueueModel.Status.COMPLETED) 400L else 500L,
                  debt =
                      if (status == QueueModel.Status.UNPAID) (-100).toBigDecimal()
                      else 0.toBigDecimal()),
              _viewModel.uiState.safeValue.temporalCustomer,
              "Temporal customer must be updated when the status change")
        },
        {
          assertEquals(
              if (status == QueueModel.Status.COMPLETED) {
                setOf(QueueModel.PaymentMethod.CASH, QueueModel.PaymentMethod.ACCOUNT_BALANCE)
              } else {
                setOf(QueueModel.PaymentMethod.CASH)
              },
              _viewModel.uiState.safeValue.allowedPaymentMethods,
              "Allowed payment methods must be updated when the status change")
        },
        {
          assertEquals(
              if (status == QueueModel.Status.COMPLETED) QueueModel.PaymentMethod.ACCOUNT_BALANCE
              else QueueModel.PaymentMethod.CASH,
              _viewModel.uiState.safeValue.paymentMethod,
              "Fallback payment method to cash when the status is other than completed")
        })
  }

  @ParameterizedTest
  @ValueSource(longs = [100L, 1000L])
  fun `on product orders changed result updates other fields`(totalPrice: Long) {
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onCustomerChanged(_customer)
    _viewModel.onStatusChanged(QueueModel.Status.COMPLETED)
    _viewModel.onPaymentMethodChanged(QueueModel.PaymentMethod.ACCOUNT_BALANCE)

    _viewModel.onProductOrdersChanged(
        _queue.productOrders.map { it.copy(totalPrice = totalPrice.toBigDecimal()) })
    assertAll(
        {
          assertEquals(
              _customer.copy(
                  balance =
                      if (totalPrice == 100L) _customer.balance - totalPrice
                      else _customer.balance),
              _viewModel.uiState.safeValue.temporalCustomer,
              "Temporal customer must be updated when the product orders change")
        },
        {
          assertEquals(
              if (totalPrice == 100L) {
                setOf(QueueModel.PaymentMethod.CASH, QueueModel.PaymentMethod.ACCOUNT_BALANCE)
              } else {
                setOf(QueueModel.PaymentMethod.CASH)
              },
              _viewModel.uiState.safeValue.allowedPaymentMethods,
              "Allowed payment method must be updated when the product orders change")
        },
        {
          assertEquals(
              if (totalPrice == 100L) QueueModel.PaymentMethod.ACCOUNT_BALANCE
              else QueueModel.PaymentMethod.CASH,
              _viewModel.uiState.safeValue.paymentMethod,
              "Fallback payment method to cash when the status is other than completed")
        })
  }

  @ParameterizedTest
  @EnumSource(QueueModel.PaymentMethod::class)
  fun `on payment method changed result updates other fields`(
      paymentMethod: QueueModel.PaymentMethod
  ) {
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onCustomerChanged(_customer)
    _viewModel.onProductOrdersChanged(_queue.productOrders)
    _viewModel.onStatusChanged(QueueModel.Status.COMPLETED)

    _viewModel.onPaymentMethodChanged(paymentMethod)
    assertEquals(
        _customer.copy(
            balance =
                if (paymentMethod == QueueModel.PaymentMethod.ACCOUNT_BALANCE) 400L else 500L),
        _viewModel.uiState.safeValue.temporalCustomer,
        "Temporal customer must be updated when the payment method change")
  }

  @Test
  fun `on save with empty product orders`() {
    _viewModel.onProductOrdersChanged(listOf())

    every { _queueRepository.add(any()) } returns CompletableFuture.completedFuture(0L)
    _viewModel.onSave()
    assertDoesNotThrow("Prevent save for an empty product orders") {
      verify(exactly = 0) { _queueRepository.add(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 111L])
  fun `on save with created queue`(createdQueueId: Long) {
    _viewModel.onCustomerChanged(_customer)
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onStatusChanged(_queue.status)
    _viewModel.onPaymentMethodChanged(_queue.paymentMethod)
    _viewModel.onProductOrdersChanged(_queue.productOrders)

    every { _queueRepository.add(any()) } returns CompletableFuture.completedFuture(createdQueueId)
    _viewModel.onSave()
    if (createdQueueId == 0L) {
      assertDoesNotThrow("Don't return result for a failed save") {
        verify(exactly = 0) { _resultStateObserver.onChanged(any()) }
      }
    } else {
      assertEquals(
          createdQueueId,
          _viewModel.resultState.value?.createdQueueId,
          "Return result with the correct ID after success save")
    }
  }

  @Test
  fun `select customer by id`() {
    every { _customerRepository.selectById(any<Long>()) } returns
        CompletableFuture.completedFuture(_customer)
    assertEquals(
        _customer,
        _viewModel.selectCustomerById(_customer.id).value,
        "Return the correct customer for the given ID")
  }

  @Test
  fun `select product by id`() {
    every { _productRepository.selectById(any<Long>()) } returns
        CompletableFuture.completedFuture(_product)
    assertEquals(
        _product,
        _viewModel.selectProductById(_product.id).value,
        "Return the correct product for the given ID")
  }
}
