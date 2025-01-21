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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.ui.PluralResource
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@HiltViewModel
open class CreateQueueViewModel
@Inject
constructor(
    @IoDispatcher protected val _dispatcher: CoroutineDispatcher,
    protected val _queueRepository: QueueRepository,
    private val _customerRepository: CustomerRepository,
    private val _productRepository: ProductRepository
) : ViewModel() {
  protected val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  protected val _uiState: SafeMutableLiveData<CreateQueueState> =
      SafeMutableLiveData(
          CreateQueueState(
              customer = null,
              temporalCustomer = null,
              date = ZonedDateTime.now(ZoneId.systemDefault()),
              status = QueueModel.Status.IN_QUEUE,
              isStatusDialogShown = false,
              paymentMethod = QueueModel.PaymentMethod.CASH,
              allowedPaymentMethods = setOf(QueueModel.PaymentMethod.CASH),
              productOrders = listOf()))
  val uiState: SafeLiveData<CreateQueueState>
    get() = _uiState

  private val _resultState: SingleLiveEvent<CreateQueueResultState> = SingleLiveEvent()
  val resultState: LiveData<CreateQueueResultState>
    get() = _resultState

  val makeProductOrderView: MakeProductOrderViewModel by lazy { MakeProductOrderViewModel(this) }
  val selectProductOrderView: SelectProductOrderViewModel by lazy {
    SelectProductOrderViewModel(this)
  }

  open fun parseInputtedQueue(): QueueModel =
      QueueModel(
          status = _uiState.safeValue.status,
          date = _uiState.safeValue.date.toInstant(),
          paymentMethod = _uiState.safeValue.paymentMethod,
          customerId = _uiState.safeValue.customer?.id,
          customer = _uiState.safeValue.customer,
          productOrders = _uiState.safeValue.productOrders)

  fun onCustomerChanged(customer: CustomerModel?) {
    _uiState.setValue(_uiState.safeValue.copy(customer = customer))
    _onUpdateAllowedPaymentMethods()
    // Update after allowed payment methods updated, in case payment method changed.
    _onUpdateTemporalCustomer()
  }

  fun onDateChanged(date: ZonedDateTime) {
    _uiState.setValue(_uiState.safeValue.copy(date = date))
  }

  fun onStatusChanged(status: QueueModel.Status) {
    _uiState.setValue(_uiState.safeValue.copy(status = status))
    _onUpdateAllowedPaymentMethods()
    // Update after allowed payment methods updated, in case payment method changed.
    _onUpdateTemporalCustomer()
  }

  fun onStatusDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isStatusDialogShown = true))
  }

  fun onStatusDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isStatusDialogShown = false))
  }

  fun onProductOrdersChanged(productOrders: List<ProductOrderModel>) {
    _uiState.setValue(_uiState.safeValue.copy(productOrders = productOrders))
    _onUpdateAllowedPaymentMethods()
    // Update after allowed payment methods updated, in case payment method changed.
    _onUpdateTemporalCustomer()
  }

  fun onPaymentMethodChanged(paymentMethod: QueueModel.PaymentMethod) {
    _uiState.setValue(_uiState.safeValue.copy(paymentMethod = paymentMethod))
    _onUpdateTemporalCustomer()
  }

  open fun onSave() {
    if (_uiState.safeValue.productOrders.isEmpty()) {
      _snackbarState.setValue(
          SnackbarState(StringResource(R.string.createQueue_includeOneProductOrderError)))
      return
    }
    viewModelScope.launch(_dispatcher) { _addQueue(parseInputtedQueue()) }
  }

  fun selectCustomerById(customerId: Long?): LiveData<CustomerModel?> =
      MutableLiveData<CustomerModel?>().apply {
        viewModelScope.launch(_dispatcher) {
          postValue(
              _customerRepository.selectById(customerId).also {
                if (it == null) {
                  _snackbarState.postValue(
                      SnackbarState(StringResource(R.string.createQueue_fetchCustomerError)))
                }
              })
        }
      }

  fun selectProductById(productId: Long?): LiveData<ProductModel?> =
      MutableLiveData<ProductModel?>().apply {
        viewModelScope.launch(_dispatcher) {
          postValue(
              _productRepository.selectById(productId).also {
                if (it == null) {
                  _snackbarState.postValue(
                      SnackbarState(StringResource(R.string.createQueue_fetchProductError)))
                }
              })
        }
      }

  protected open fun _onUpdateAllowedPaymentMethods() {
    val allowedPaymentMethods: MutableSet<QueueModel.PaymentMethod> =
        _uiState.safeValue.allowedPaymentMethods.toMutableSet().apply {
          if (_uiState.safeValue.status == QueueModel.Status.COMPLETED &&
              _uiState.safeValue.customer?.isBalanceSufficient(null, parseInputtedQueue()) ==
                  true) {
            add(QueueModel.PaymentMethod.ACCOUNT_BALANCE)
          } else {
            remove(QueueModel.PaymentMethod.ACCOUNT_BALANCE)
          }
        }
    _uiState.setValue(_uiState.safeValue.copy(allowedPaymentMethods = allowedPaymentMethods))
    // Change payment method to cash when current selected one marked as not allowed.
    if (!allowedPaymentMethods.contains(_uiState.safeValue.paymentMethod)) {
      onPaymentMethodChanged(QueueModel.PaymentMethod.CASH)
    }
  }

  protected open fun _onUpdateTemporalCustomer() {
    val inputtedQueue: QueueModel = parseInputtedQueue()
    _uiState.setValue(
        _uiState.safeValue.copy(
            temporalCustomer =
                _uiState.safeValue.customer?.let {
                  it.copy(
                      balance = it.balanceOnMadePayment(inputtedQueue),
                      debt = it.debtOnMadePayment(inputtedQueue))
                }))
  }

  private suspend fun _addQueue(queue: QueueModel) {
    _queueRepository.add(queue).let { id ->
      if (id != 0L) _resultState.postValue(CreateQueueResultState(id))
      _snackbarState.postValue(
          SnackbarState(
              if (id != 0L) PluralResource(R.plurals.createQueue_added_n_queue, 1, 1)
              else StringResource(R.string.createQueue_addQueueError)))
    }
  }
}
