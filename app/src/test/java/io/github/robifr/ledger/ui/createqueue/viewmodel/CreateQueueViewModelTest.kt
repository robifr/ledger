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

package io.github.robifr.ledger.ui.createqueue.viewmodel

import androidx.lifecycle.Observer
import io.github.robifr.ledger.InstantTaskExecutorExtension
import io.github.robifr.ledger.LifecycleOwnerExtension
import io.github.robifr.ledger.LifecycleTestOwner
import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.repository.CustomerRepository
import io.github.robifr.ledger.repository.ProductRepository
import io.github.robifr.ledger.repository.QueueRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verifyOrder
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
  private lateinit var _uiEventObserver: Observer<CreateQueueEvent>

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
                      quantity = 1.0)),
          note = "Example")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _queueRepository = mockk()
    _customerRepository = mockk()
    _productRepository = mockk()
    _uiEventObserver = mockk(relaxed = true)
    _viewModel =
        CreateQueueViewModel(_dispatcher, _queueRepository, _customerRepository, _productRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on state changed`() {
    _viewModel.onCustomerChanged(_customer)
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onStatusChanged(_queue.status)
    _viewModel.onStatusDialogShown()
    _viewModel.onPaymentMethodChanged(_queue.paymentMethod)
    _viewModel.onProductOrdersChanged(_queue.productOrders)
    _viewModel.onNoteTextChanged(_queue.note)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve all values except for the changed field")
        .isEqualTo(
            CreateQueueState(
                customer = _customer,
                temporalCustomer = _customer,
                date = _queue.date.atZone(ZoneId.systemDefault()),
                status = _queue.status,
                isStatusDialogShown = true,
                paymentMethod = _queue.paymentMethod,
                allowedPaymentMethods = setOf(_queue.paymentMethod),
                productOrders = _queue.productOrders,
                note = _queue.note))
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 500L])
  fun `on customer changed result updates other fields`(balance: Long) {
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onProductOrdersChanged(_queue.productOrders)
    _viewModel.onStatusChanged(QueueModel.Status.COMPLETED)
    _viewModel.onPaymentMethodChanged(QueueModel.PaymentMethod.ACCOUNT_BALANCE)

    _viewModel.onCustomerChanged(_customer.copy(balance = balance))
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.temporalCustomer)
          .describedAs("Temporal customer must be updated when the customer change")
          .isEqualTo(_customer.copy(balance = if (balance == 500L) 400L else balance))
      it.assertThat(_viewModel.uiState.safeValue.allowedPaymentMethods)
          .describedAs("Allowed payment methods must be updated when the customer change")
          .isEqualTo(
              if (balance == 0L) setOf(QueueModel.PaymentMethod.CASH)
              else setOf(QueueModel.PaymentMethod.CASH, QueueModel.PaymentMethod.ACCOUNT_BALANCE))
      it.assertThat(_viewModel.uiState.safeValue.paymentMethod)
          .describedAs("Fallback payment method to cash when the customer balance is insufficient")
          .isEqualTo(
              if (balance == 0L) QueueModel.PaymentMethod.CASH
              else QueueModel.PaymentMethod.ACCOUNT_BALANCE)
    }
  }

  @ParameterizedTest
  @EnumSource(QueueModel.Status::class)
  fun `on status changed result updates other fields`(status: QueueModel.Status) {
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onCustomerChanged(_customer)
    _viewModel.onProductOrdersChanged(_queue.productOrders)
    _viewModel.onPaymentMethodChanged(QueueModel.PaymentMethod.ACCOUNT_BALANCE)

    _viewModel.onStatusChanged(status)
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.temporalCustomer)
          .describedAs("Temporal customer must be updated when the status change")
          .isEqualTo(
              _customer.copy(
                  balance = if (status == QueueModel.Status.COMPLETED) 400L else 500L,
                  debt =
                      if (status == QueueModel.Status.UNPAID) (-100).toBigDecimal()
                      else 0.toBigDecimal()))
      it.assertThat(_viewModel.uiState.safeValue.allowedPaymentMethods)
          .describedAs("Allowed payment methods must be updated when the status change")
          .isEqualTo(
              if (status == QueueModel.Status.COMPLETED) {
                setOf(QueueModel.PaymentMethod.CASH, QueueModel.PaymentMethod.ACCOUNT_BALANCE)
              } else {
                setOf(QueueModel.PaymentMethod.CASH)
              },
          )
      it.assertThat(_viewModel.uiState.safeValue.paymentMethod)
          .describedAs("Fallback payment method to cash when the status is other than completed")
          .isEqualTo(
              if (status == QueueModel.Status.COMPLETED) QueueModel.PaymentMethod.ACCOUNT_BALANCE
              else QueueModel.PaymentMethod.CASH)
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on status dialog shown`(isShown: Boolean) {
    _viewModel.onCustomerChanged(_customer)
    _viewModel.onDateChanged(_queue.date.atZone(ZoneId.systemDefault()))
    _viewModel.onStatusChanged(_queue.status)
    _viewModel.onPaymentMethodChanged(_queue.paymentMethod)
    _viewModel.onProductOrdersChanged(_queue.productOrders)
    _viewModel.onNoteTextChanged(_queue.note)

    if (isShown) _viewModel.onStatusDialogShown() else _viewModel.onStatusDialogClosed()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            CreateQueueState(
                customer = _customer,
                temporalCustomer = _customer,
                date = _queue.date.atZone(ZoneId.systemDefault()),
                status = _queue.status,
                isStatusDialogShown = isShown,
                paymentMethod = _queue.paymentMethod,
                allowedPaymentMethods = setOf(_queue.paymentMethod),
                productOrders = _queue.productOrders,
                note = _queue.note))
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
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.temporalCustomer)
          .describedAs("Temporal customer must be updated when the product orders change")
          .isEqualTo(
              _customer.copy(
                  balance =
                      if (totalPrice == 100L) _customer.balance - totalPrice
                      else _customer.balance))
      it.assertThat(_viewModel.uiState.safeValue.allowedPaymentMethods)
          .describedAs("Allowed payment method must be updated when the product orders change")
          .isEqualTo(
              if (totalPrice == 100L) {
                setOf(QueueModel.PaymentMethod.CASH, QueueModel.PaymentMethod.ACCOUNT_BALANCE)
              } else {
                setOf(QueueModel.PaymentMethod.CASH)
              })
      it.assertThat(_viewModel.uiState.safeValue.paymentMethod)
          .describedAs("Fallback payment method to cash when the status is other than completed")
          .isEqualTo(
              if (totalPrice == 100L) QueueModel.PaymentMethod.ACCOUNT_BALANCE
              else QueueModel.PaymentMethod.CASH)
    }
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
    assertThat(_viewModel.uiState.safeValue.temporalCustomer)
        .describedAs("Temporal customer must be updated when the payment method change")
        .isEqualTo(
            _customer.copy(
                balance =
                    if (paymentMethod == QueueModel.PaymentMethod.ACCOUNT_BALANCE) 400L else 500L))
  }

  @Test
  fun `on save with empty product orders`() {
    _viewModel.onProductOrdersChanged(listOf())

    coEvery { _queueRepository.add(any()) } returns 0L
    _viewModel.onSave()
    assertSoftly {
      it.assertThatCode { coVerify(exactly = 0) { _queueRepository.add(any()) } }
          .describedAs("Prevent save for an empty product orders")
          .doesNotThrowAnyException()
      it.assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
          .describedAs("Notify the error via snackbar")
          .isNotNull()
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

    coEvery { _queueRepository.add(any()) } returns createdQueueId
    _viewModel.onSave()
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
          .describedAs("Notify the result via snackbar")
          .isNotNull()
      it.assertThat(_viewModel.uiEvent.safeValue.createResult?.data)
          .describedAs("Return result with the correct ID after success save")
          .isEqualTo(if (createdQueueId != 0L) CreateQueueResultState(createdQueueId) else null)
      it.assertThatCode {
            verifyOrder {
              _uiEventObserver.onChanged(match { it.snackbar != null && it.createResult == null })
              if (createdQueueId != 0L) {
                _uiEventObserver.onChanged(match { it.createResult != null })
              }
            }
          }
          .describedAs("Update result event last to finish the fragment")
          .doesNotThrowAnyException()
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on back pressed`(isQueueChanged: Boolean) {
    if (isQueueChanged) _viewModel.onStatusChanged(QueueModel.Status.UNPAID)

    _viewModel.onBackPressed()
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.isUnsavedChangesDialogShown?.data)
          .describedAs("Show unsaved changes dialog when there's a change")
          .isEqualTo(if (isQueueChanged) true else null)
      it.assertThat(_viewModel.uiEvent.safeValue.isFragmentFinished?.data)
          .describedAs("Finish fragment when there's no change")
          .isEqualTo(if (!isQueueChanged) true else null)
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `select customer by id`(isCustomerNull: Boolean) {
    coEvery { _customerRepository.selectById(any<Long>()) } returns
        if (!isCustomerNull) _customer else null
    assertSoftly {
      it.assertThat(_viewModel.selectCustomerById(_customer.id).value)
          .describedAs("Return the correct customer for the given ID")
          .isEqualTo(if (!isCustomerNull) _customer else null)
      it.assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
          .describedAs("Notify any error via snackbar")
          .apply { if (isCustomerNull) isNotNull() else isNull() }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `select product by id`(isProductNull: Boolean) {
    coEvery { _productRepository.selectById(any<Long>()) } returns
        if (!isProductNull) _product else null
    assertSoftly {
      it.assertThat(_viewModel.selectProductById(_product.id).value)
          .describedAs("Return the correct product for the given ID")
          .isEqualTo(if (!isProductNull) _product else null)
      it.assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
          .describedAs("Notify any error via snackbar")
          .apply { if (isProductNull) isNotNull() else isNull() }
    }
  }
}
