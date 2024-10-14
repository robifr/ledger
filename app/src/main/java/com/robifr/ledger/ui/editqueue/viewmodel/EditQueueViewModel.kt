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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.R
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.ui.PluralResource
import com.robifr.ledger.ui.SafeEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import com.robifr.ledger.ui.createqueue.viewmodel.CreateQueueViewModel
import com.robifr.ledger.ui.editqueue.EditQueueFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EditQueueViewModel
@Inject
constructor(
    queueRepository: QueueRepository,
    customerRepository: CustomerRepository,
    productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : CreateQueueViewModel(queueRepository, customerRepository, productRepository) {
  private lateinit var _initialQueueToEdit: QueueModel

  private val _editResultState: MutableLiveData<SafeEvent<EditQueueResultState>> = MutableLiveData()
  val editResultState: LiveData<SafeEvent<EditQueueResultState>>
    get() = _editResultState

  init {
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    // The initial queue ID also shouldn't be null when editing data.
    SafeEvent.observeOnce(
        _selectQueueById(
            savedStateHandle.get<Long>(
                EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key)!!),
    ) { queue ->
      queue?.let {
        _initialQueueToEdit = it
        onCustomerChanged(it.customer)
        onDateChanged(it.date.atZone(ZoneId.systemDefault()))
        onStatusChanged(it.status)
        onPaymentMethodChanged(it.paymentMethod)
        onProductOrdersChanged(it.productOrders)
      }
    }
  }

  override fun inputtedQueue(): QueueModel =
      if (::_initialQueueToEdit.isInitialized) {
        super.inputtedQueue().copy(id = _initialQueueToEdit.id)
      } else {
        super.inputtedQueue()
      }

  override fun onSave() {
    if (inputtedQueue().productOrders.isEmpty()) {
      _snackbarState.setValue(
          SafeEvent(
              SnackbarState(StringResource(R.string.createQueue_includeOneProductOrderError))))
      return
    }
    _updateQueue(inputtedQueue())
  }

  override fun _onUpdateAllowedPaymentMethods() {
    if (!::_initialQueueToEdit.isInitialized) return super._onUpdateAllowedPaymentMethods()
    val inputtedQueue: QueueModel = inputtedQueue()
    val allowedPaymentMethods: MutableSet<QueueModel.PaymentMethod> =
        _uiState.safeValue.allowedPaymentMethods.toMutableSet()

    val isBalanceSufficient: Boolean =
        inputtedQueue.status == QueueModel.Status.COMPLETED &&
            _uiState.safeValue.customer?.isBalanceSufficient(_initialQueueToEdit, inputtedQueue) ==
                true
    val isTemporalBalancePositive: Boolean =
        _uiState.safeValue.customer?.let {
          // Compare with the account balance payment option
          // as if the user does it before they actually do.
          it.balanceOnUpdatedPayment(
              _initialQueueToEdit,
              inputtedQueue.copy(paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE)) >= 0L
        } == true
    if (isBalanceSufficient && isTemporalBalancePositive) {
      allowedPaymentMethods.add(QueueModel.PaymentMethod.ACCOUNT_BALANCE)
    } else {
      allowedPaymentMethods.remove(QueueModel.PaymentMethod.ACCOUNT_BALANCE)
    }

    _uiState.setValue(_uiState.safeValue.copy(allowedPaymentMethods = allowedPaymentMethods))
    // Change payment method to cash when current selected one marked as not allowed.
    if (!allowedPaymentMethods.contains(inputtedQueue.paymentMethod)) {
      onPaymentMethodChanged(QueueModel.PaymentMethod.CASH)
    }
  }

  override fun _onUpdateTemporalCustomer() {
    if (!::_initialQueueToEdit.isInitialized) return _onUpdateTemporalCustomer()
    _uiState.setValue(
        _uiState.safeValue.copy(
            temporalCustomer =
                _uiState.safeValue.customer?.let {
                  it.copy(
                      balance = it.balanceOnUpdatedPayment(_initialQueueToEdit, inputtedQueue()),
                      debt = it.debtOnUpdatedPayment(_initialQueueToEdit, inputtedQueue()))
                }))
  }

  private fun _selectQueueById(queueId: Long?): LiveData<QueueModel?> {
    val result: MutableLiveData<QueueModel?> = MutableLiveData()
    _queueRepository.selectById(queueId).thenAcceptAsync { queue: QueueModel? ->
      if (queue == null) {
        _snackbarState.postValue(
            SafeEvent(SnackbarState(StringResource(R.string.createQueue_fetchQueueError))))
      }
      result.postValue(queue)
    }
    return result
  }

  private fun _updateQueue(queue: QueueModel) {
    _queueRepository.update(queue).thenAcceptAsync { effected: Int ->
      if (effected > 0) _editResultState.postValue(SafeEvent(EditQueueResultState(queue.id)))
      _snackbarState.postValue(
          SafeEvent(
              SnackbarState(
                  if (effected > 0) {
                    PluralResource(R.plurals.createQueue_updated_n_queue, effected, effected)
                  } else {
                    StringResource(R.string.createQueue_updateQueueError)
                  })))
    }
  }
}
