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

import com.robifr.ledger.InstantTaskExecutorRuleForJUnit5
import com.robifr.ledger.MainCoroutineRule
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.repository.QueueRepository
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorRuleForJUnit5::class, MainCoroutineRule::class)
open class CreateQueueViewModelTest {
  protected open lateinit var _viewModel: CreateQueueViewModel
  protected val _queueRepository: QueueRepository = mock()
  protected val _customerRepository: CustomerRepository = mock()
  protected val _productRepository: ProductRepository = mock()
  private val _uiState: CreateQueueState =
      CreateQueueState(
          customer = CustomerModel(id = 111L, name = "Amy", balance = 500L),
          temporalCustomer = CustomerModel(id = 111L, name = "Amy", balance = 500L),
          date = ZonedDateTime.now(),
          status = QueueModel.Status.IN_QUEUE,
          paymentMethod = QueueModel.PaymentMethod.CASH,
          allowedPaymentMethods = setOf(QueueModel.PaymentMethod.CASH),
          productOrders =
              listOf(
                  ProductOrderModel(
                      productId = 111L,
                      productName = "Apple",
                      productPrice = 100L,
                      quantity = 1.0,
                      totalPrice = 100.toBigDecimal())))

  @BeforeEach
  protected open fun beforeEach() {
    reset(_queueRepository, _customerRepository, _productRepository)
    _viewModel = CreateQueueViewModel(_queueRepository, _customerRepository, _productRepository)
  }

  @Test
  fun `on state changed`() {
    _viewModel.onCustomerChanged(_uiState.customer)
    _viewModel.onDateChanged(_uiState.date)
    _viewModel.onStatusChanged(_uiState.status)
    _viewModel.onPaymentMethodChanged(_uiState.paymentMethod)
    _viewModel.onProductOrdersChanged(_uiState.productOrders)
    assertEquals(
        _uiState, _viewModel.uiState.safeValue, "Preserve all values except for the changed field")
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 500L])
  fun `on update allowed payment methods`(customerBalance: Long) {
    _viewModel.onStatusChanged(QueueModel.Status.COMPLETED)
    _viewModel.onProductOrdersChanged(_uiState.productOrders)

    _viewModel.onPaymentMethodChanged(QueueModel.PaymentMethod.ACCOUNT_BALANCE)
    _viewModel.onCustomerChanged(_uiState.customer!!.copy(balance = customerBalance))
    assertEquals(
        if (customerBalance == 0L) setOf(QueueModel.PaymentMethod.CASH)
        else setOf(QueueModel.PaymentMethod.CASH, QueueModel.PaymentMethod.ACCOUNT_BALANCE),
        _viewModel.uiState.safeValue.allowedPaymentMethods,
        "Disable account balance payment option when the balance is insufficient")
    assertEquals(
        if (customerBalance == 0L) QueueModel.PaymentMethod.CASH
        else _viewModel.uiState.safeValue.paymentMethod,
        _viewModel.uiState.safeValue.paymentMethod,
        "Set the payment method to cash when the previous option is no longer allowed")
  }

  @ParameterizedTest
  @EnumSource(QueueModel.PaymentMethod::class)
  fun `on update temporal customer`(paymentMethod: QueueModel.PaymentMethod) {
    _viewModel.onStatusChanged(QueueModel.Status.COMPLETED)

    _viewModel.onCustomerChanged(_uiState.customer)
    _viewModel.onProductOrdersChanged(_uiState.productOrders)
    _viewModel.onPaymentMethodChanged(paymentMethod)
    assertEquals(
        _uiState.customer!!.copy(
            balance =
                if (paymentMethod == QueueModel.PaymentMethod.ACCOUNT_BALANCE) {
                  _uiState.customer!!.balance -
                      _uiState.productOrders.sumOf { it.totalPrice.toLong() }
                } else {
                  _uiState.customer!!.balance
                }),
        _viewModel.uiState.safeValue.temporalCustomer,
        "Reduce temporal customer balance when the selected payment method is account balance")
  }

  @Test
  fun `on save with empty product orders`() {
    _viewModel.onProductOrdersChanged(listOf())
    _viewModel.onSave()
    assertDoesNotThrow("Prevent save for an empty product orders") {
      verify(_queueRepository, never()).add(any())
    }
  }
}
