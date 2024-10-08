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

package com.robifr.ledger.ui.createcustomer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.PluralResource
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

@HiltViewModel
open class CreateCustomerViewModel
@Inject
constructor(
    @IoDispatcher protected val _dispatcher: CoroutineDispatcher,
    protected val _customerRepository: CustomerRepository
) : ViewModel() {
  protected val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  protected val _uiState: SafeMutableLiveData<CreateCustomerState> =
      SafeMutableLiveData(
          CreateCustomerState(
              name = "", nameErrorMessageRes = null, balance = 0L, debt = 0.toBigDecimal()))
  val uiState: SafeLiveData<CreateCustomerState>
    get() = _uiState

  private val _resultState: SingleLiveEvent<CreateCustomerResultState> = SingleLiveEvent()
  val resultState: LiveData<CreateCustomerResultState>
    get() = _resultState

  val balanceView: CustomerBalanceViewModel by lazy { CustomerBalanceViewModel(this) }

  fun onNameTextChanged(name: String) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            name = name,
            // Disable error when name field filled.
            nameErrorMessageRes =
                if (name.isBlank()) _uiState.safeValue.nameErrorMessageRes else null))
  }

  fun onBalanceChanged(balance: Long) {
    _uiState.setValue(_uiState.safeValue.copy(balance = balance))
  }

  fun onDebtChanged(debt: BigDecimal) {
    _uiState.setValue(_uiState.safeValue.copy(debt = debt))
  }

  open fun onSave() {
    if (_uiState.safeValue.name.isBlank()) {
      _uiState.setValue(
          _uiState.safeValue.copy(
              nameErrorMessageRes = StringResource(R.string.createCustomer_name_emptyError)))
      return
    }
    viewModelScope.launch(_dispatcher) { _addCustomer(_parseInputtedCustomer()) }
  }

  protected open fun _parseInputtedCustomer(): CustomerModel =
      CustomerModel(
          name = _uiState.safeValue.name,
          balance = _uiState.safeValue.balance,
          debt = _uiState.safeValue.debt)

  private suspend fun _addCustomer(customer: CustomerModel) {
    _customerRepository.add(customer).await().let { id: Long? ->
      if (id != 0L) _resultState.postValue(CreateCustomerResultState(id))
      _snackbarState.postValue(
          SnackbarState(
              if (id != 0L) PluralResource(R.plurals.createCustomer_added_n_customer, 1, 1)
              else StringResource(R.string.createCustomer_addCustomerError)))
    }
  }
}
