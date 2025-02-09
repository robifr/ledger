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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.common.PluralResource
import com.robifr.ledger.ui.common.StringResource
import com.robifr.ledger.ui.common.StringResourceType
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import com.robifr.ledger.ui.common.state.SnackbarState
import com.robifr.ledger.ui.common.state.updateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@HiltViewModel
open class CreateCustomerViewModel
@Inject
constructor(
    @IoDispatcher protected val _dispatcher: CoroutineDispatcher,
    protected val _customerRepository: CustomerRepository
) : ViewModel() {
  private val _initialCustomerToCreate: CustomerModel =
      CustomerModel(name = "", balance = 0L, debt = 0.toBigDecimal())

  protected val _uiEvent: SafeMutableLiveData<CreateCustomerEvent> =
      SafeMutableLiveData(CreateCustomerEvent())
  val uiEvent: SafeLiveData<CreateCustomerEvent>
    get() = _uiEvent

  protected val _uiState: SafeMutableLiveData<CreateCustomerState> =
      SafeMutableLiveData(
          CreateCustomerState(
              name = _initialCustomerToCreate.name,
              nameErrorMessageRes = null,
              balance = _initialCustomerToCreate.balance,
              debt = _initialCustomerToCreate.debt))
  val uiState: SafeLiveData<CreateCustomerState>
    get() = _uiState

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

  open fun onBackPressed() {
    if (_initialCustomerToCreate != _parseInputtedCustomer()) _onUnsavedChangesDialogShown()
    else _onFragmentFinished()
  }

  protected open fun _parseInputtedCustomer(): CustomerModel =
      CustomerModel(
          name = _uiState.safeValue.name,
          balance = _uiState.safeValue.balance,
          debt = _uiState.safeValue.debt)

  protected fun _onSnackbarShown(messageRes: StringResourceType) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SnackbarState(messageRes),
          onSet = { this?.copy(snackbar = it) },
          onReset = { this?.copy(snackbar = null) })
    }
  }

  protected fun _onFragmentFinished() {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = true,
          onSet = { this?.copy(isFragmentFinished = it) },
          onReset = { this?.copy(isFragmentFinished = null) })
    }
  }

  protected fun _onUnsavedChangesDialogShown() {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = true,
          onSet = { this?.copy(isUnsavedChangesDialogShown = it) },
          onReset = { this?.copy(isUnsavedChangesDialogShown = null) })
    }
  }

  private suspend fun _addCustomer(customer: CustomerModel) {
    _customerRepository.add(customer).let { id ->
      _onSnackbarShown(
          if (id != 0L) PluralResource(R.plurals.createCustomer_added_n_customer, 1, 1)
          else StringResource(R.string.createCustomer_addCustomerError))
      if (id != 0L) {
        _uiEvent.updateEvent(
            data = CreateCustomerResultState(id),
            onSet = { this?.copy(createResult = it) },
            onReset = { this?.copy(createResult = null) })
      }
    }
  }
}
