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
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.ui.common.state.UiEvent
import com.robifr.ledger.ui.createqueue.viewmodel.CreateQueueEvent
import com.robifr.ledger.ui.createqueue.viewmodel.CreateQueueState
import com.robifr.ledger.ui.editqueue.EditQueueFragment
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verifyOrder
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _viewModel: EditQueueViewModel
  private lateinit var _uiEventObserver: Observer<CreateQueueEvent>
  private lateinit var _editResultEventObserver: Observer<UiEvent<EditQueueResultState>>

  private val _customer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 500L)
  private val _productOrder: ProductOrderModel =
      ProductOrderModel(
          id = 111L,
          queueId = 111L,
          productId = 111L,
          productName = "Apple",
          productPrice = 100L,
          quantity = 1.0)
  private val _queueToEdit: QueueModel =
      QueueModel(
          id = 111L,
          customerId = _customer.id,
          customer = _customer,
          status = QueueModel.Status.COMPLETED,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE,
          productOrders = listOf(_productOrder),
          note = "Example")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _queueRepository = mockk()
    _uiEventObserver = mockk(relaxed = true)
    _editResultEventObserver = mockk(relaxed = true)

    coEvery { _queueRepository.add(any()) } returns 0L
    coEvery { _queueRepository.selectById(_queueToEdit.id) } returns _queueToEdit
    _viewModel =
        EditQueueViewModel(
            dispatcher = _dispatcher,
            queueRepository = _queueRepository,
            customerRepository = mockk(),
            productRepository = mockk(),
            _savedStateHandle =
                SavedStateHandle().apply {
                  set(
                      EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key(),
                      _queueToEdit.id)
                })
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
    _viewModel.editResultEvent.observe(_lifecycleOwner, _editResultEventObserver)
  }

  @Test
  fun `on initialize with arguments`() {
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Match state with the retrieved data from the fragment arguments")
        .isEqualTo(
            CreateQueueState(
                customer = _queueToEdit.customer,
                temporalCustomer = _queueToEdit.customer,
                date = _queueToEdit.date.atZone(ZoneId.systemDefault()),
                status = _queueToEdit.status,
                isStatusDialogShown = false,
                paymentMethod = _queueToEdit.paymentMethod,
                allowedPaymentMethods =
                    setOf(QueueModel.PaymentMethod.CASH, _queueToEdit.paymentMethod),
                productOrders = _queueToEdit.productOrders,
                note = _queueToEdit.note))
  }

  @Test
  fun `on initialize with empty initial queue`() {
    coEvery { _queueRepository.selectById(null) } returns null
    assertThatThrownBy {
          runTest {
            _viewModel =
                EditQueueViewModel(
                    dispatcher = _dispatcher,
                    queueRepository = _queueRepository,
                    customerRepository = mockk(),
                    productRepository = mockk(),
                    _savedStateHandle =
                        SavedStateHandle().apply {
                          set(EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key(), null)
                        })
          }
        }
        .describedAs("Can't edit queue if there's no queue ID is provided")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on update allowed payment methods`(isCustomerChanged: Boolean) {
    // Happens when the user edits a queue where all the customer balance has already been used
    // via account balance, making it appear to be less than the queue's grand total price.
    val initialCustomer: CustomerModel =
        _customer.copy(id = if (isCustomerChanged) 222L else _customer.id, balance = 0L)
    val initialQueue: QueueModel = _queueToEdit.copy(customer = initialCustomer)
    coEvery { _queueRepository.selectById(_queueToEdit.id) } returns initialQueue
    _viewModel =
        EditQueueViewModel(
            dispatcher = _dispatcher,
            queueRepository = _queueRepository,
            customerRepository = mockk(),
            productRepository = mockk(),
            _savedStateHandle =
                SavedStateHandle().apply {
                  set(
                      EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key(),
                      _queueToEdit.id)
                })

    // It's negative when the customer is changed, otherwise positive.
    val futureTemporalCustomerBalance: Long =
        initialCustomer.balanceOnUpdatedPayment(
            initialQueue,
            initialQueue.copy(paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE))
    assertThat(_viewModel.uiState.safeValue.allowedPaymentMethods)
        .describedAs(
            "Remove account balance payment option when the temporal balance will be negative")
        .isEqualTo(
            if (futureTemporalCustomerBalance >= 0L) {
              setOf(QueueModel.PaymentMethod.CASH, QueueModel.PaymentMethod.ACCOUNT_BALANCE)
            } else {
              setOf(QueueModel.PaymentMethod.CASH)
            })
  }

  @Test
  fun `on update temporal customer result updated balance`() {
    // Simplest way to ensure `CustomerModel.balanceOnUpdatedPayment()` gets called is by changing
    // the product orders total price, without altering any other fields. This will make the
    // temporal customer's balance deducted based on difference between two total prices.
    val editedQueue: QueueModel =
        _queueToEdit.copy(
            productOrders =
                _queueToEdit.productOrders.map {
                  it.copy(totalPrice = it.totalPrice + 100.toBigDecimal())
                })
    _viewModel.onProductOrdersChanged(editedQueue.productOrders)
    assertThat(_viewModel.uiState.safeValue.temporalCustomer?.balance)
        .describedAs(
            "Update temporal customer's balance via `CustomerModel.balanceOnUpdatedPayment()`")
        .isEqualTo(_customer.balanceOnUpdatedPayment(_queueToEdit, editedQueue))
  }

  @Test
  fun `on update temporal customer result updated debt`() {
    // Same as balance, simplest way to ensure `CustomerModel.debtOnUpdatedPayment()` gets called
    // is by changing status to unpaid and product orders total price. This will make the temporal
    // customer's debt deducted based on difference between two total prices.
    val editedQueue: QueueModel =
        _queueToEdit.copy(
            status = QueueModel.Status.UNPAID,
            productOrders =
                _queueToEdit.productOrders.map {
                  it.copy(totalPrice = it.totalPrice + 100.toBigDecimal())
                })
    _viewModel.onStatusChanged(editedQueue.status)
    _viewModel.onProductOrdersChanged(editedQueue.productOrders)
    assertThat(_viewModel.uiState.safeValue.temporalCustomer?.debt)
        .describedAs("Update temporal customer's debt via `CustomerModel.debtOnUpdatedPayment()`")
        .isEqualTo(_customer.debtOnUpdatedPayment(_queueToEdit, editedQueue))
  }

  @Test
  fun `on save with empty product orders`() {
    _viewModel.onProductOrdersChanged(listOf())

    coEvery { _queueRepository.update(any()) } returns 0
    _viewModel.onSave()
    assertThatCode { coVerify(exactly = 0) { _queueRepository.update(any()) } }
        .describedAs("Prevent save for an empty product orders")
        .doesNotThrowAnyException()
  }

  @Test
  fun `on save with edited queue result update operation`() {
    // Prevent save with add operation (parent class behavior) instead of update operation.
    coEvery { _queueRepository.update(any()) } returns 0
    _viewModel.onSave()
    assertThatCode {
          coVerify(exactly = 0) { _queueRepository.add(any()) }
          coVerify(exactly = 1) { _queueRepository.update(any()) }
        }
        .describedAs("Editing a queue shouldn't result in adding new data")
        .doesNotThrowAnyException()
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `on save with edited queue`(effectedRows: Int) {
    coEvery { _queueRepository.update(any()) } returns effectedRows
    _viewModel.onSave()
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
          .describedAs("Notify the result via snackbar")
          .isNotNull()
      it.assertThat(_viewModel.editResultEvent.value?.data)
          .describedAs("Return result with the correct ID after success update")
          .isEqualTo(if (effectedRows != 0) EditQueueResultState(_queueToEdit.id) else null)
      it.assertThatCode {
            verifyOrder {
              _uiEventObserver.onChanged(match { it.snackbar != null })
              if (effectedRows != 0) _editResultEventObserver.onChanged(any())
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
}
