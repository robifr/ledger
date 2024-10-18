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
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.di.IoDispatcher
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditQueueViewModel
@Inject
constructor(
    queueRepository: QueueRepository,
    customerRepository: CustomerRepository,
    productRepository: ProductRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val _savedStateHandle: SavedStateHandle,
) : CreateQueueViewModel(queueRepository, customerRepository, productRepository, dispatcher) {
  private lateinit var _initialQueueToEdit: QueueModel

  private val _editResultState: MutableLiveData<SafeEvent<EditQueueResultState>> = MutableLiveData()
  val editResultState: LiveData<SafeEvent<EditQueueResultState>>
    get() = _editResultState

  override val inputtedQueue: QueueModel
    get() =
        if (::_initialQueueToEdit.isInitialized) {
          super.inputtedQueue.copy(id = _initialQueueToEdit.id)
        } else {
          super.inputtedQueue
        }

  init {
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadQueueToEdit()
  }

  override fun onSave() {
    if (inputtedQueue.productOrders.isEmpty()) {
      _snackbarState.setValue(
          SafeEvent(
              SnackbarState(StringResource(R.string.createQueue_includeOneProductOrderError))))
      return
    }
    viewModelScope.launch(_dispatcher) { _updateQueue(inputtedQueue) }
  }

  override fun _onUpdateAllowedPaymentMethods() {
    if (!::_initialQueueToEdit.isInitialized) return super._onUpdateAllowedPaymentMethods()

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
    val allowedPaymentMethods: MutableSet<QueueModel.PaymentMethod> =
        _uiState.safeValue.allowedPaymentMethods.toMutableSet().apply {
          if (isBalanceSufficient && isTemporalBalancePositive) {
            add(QueueModel.PaymentMethod.ACCOUNT_BALANCE)
          } else {
            remove(QueueModel.PaymentMethod.ACCOUNT_BALANCE)
          }
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
                      balance = it.balanceOnUpdatedPayment(_initialQueueToEdit, inputtedQueue),
                      debt = it.debtOnUpdatedPayment(_initialQueueToEdit, inputtedQueue))
                }))
  }

  private suspend fun _selectQueueById(queueId: Long?): QueueModel? =
      _queueRepository.selectById(queueId).await().also { queue: QueueModel? ->
        if (queue == null) {
          _snackbarState.postValue(
              SafeEvent(SnackbarState(StringResource(R.string.createQueue_fetchQueueError))))
        }
      }

  private suspend fun _updateQueue(queue: QueueModel) {
    _queueRepository.update(queue).await().also { effected: Int? ->
      val updated: Int = effected ?: 0
      if (updated > 0) _editResultState.postValue(SafeEvent(EditQueueResultState(queue.id)))
      _snackbarState.postValue(
          SafeEvent(
              SnackbarState(
                  if (updated > 0) {
                    PluralResource(R.plurals.createQueue_updated_n_queue, updated, updated)
                  } else {
                    StringResource(R.string.createQueue_updateQueueError)
                  })))
    }
  }

  private fun _loadQueueToEdit() {
    viewModelScope.launch(_dispatcher) {
      // The initial queue ID shouldn't be null when editing data.
      _selectQueueById(
              _savedStateHandle.get<Long>(
                  EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key)!!)
          ?.let {
            withContext(Dispatchers.Main) {
              _initialQueueToEdit = it
              onCustomerChanged(it.customer)
              onDateChanged(it.date.atZone(ZoneId.systemDefault()))
              onStatusChanged(it.status)
              onPaymentMethodChanged(it.paymentMethod)
              onProductOrdersChanged(it.productOrders)
            }
          }
    }
  }
}
