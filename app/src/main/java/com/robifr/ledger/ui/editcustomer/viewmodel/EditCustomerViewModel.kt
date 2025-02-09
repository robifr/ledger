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

package com.robifr.ledger.ui.editcustomer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.common.PluralResource
import com.robifr.ledger.ui.common.StringResource
import com.robifr.ledger.ui.common.state.UiEvent
import com.robifr.ledger.ui.common.state.updateEvent
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerViewModel
import com.robifr.ledger.ui.editcustomer.EditCustomerFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditCustomerViewModel
@Inject
constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    customerRepository: CustomerRepository,
    private val _savedStateHandle: SavedStateHandle
) : CreateCustomerViewModel(dispatcher, customerRepository) {
  private lateinit var _initialCustomerToEdit: CustomerModel

  private val _editResultEvent: MutableLiveData<UiEvent<EditCustomerResultState>> =
      MutableLiveData()
  val editResultEvent: LiveData<UiEvent<EditCustomerResultState>>
    get() = _editResultEvent

  init {
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadCustomerToEdit()
  }

  override fun onSave() {
    if (_uiState.safeValue.name.isBlank()) {
      _uiState.setValue(
          _uiState.safeValue.copy(
              nameErrorMessageRes = StringResource(R.string.createCustomer_name_emptyError)))
      return
    }
    viewModelScope.launch(_dispatcher) { _updateCustomer(_parseInputtedCustomer()) }
  }

  override fun onBackPressed() {
    if (_initialCustomerToEdit != _parseInputtedCustomer()) _onUnsavedChangesDialogShown()
    else _onFragmentFinished()
  }

  override fun _parseInputtedCustomer(): CustomerModel =
      if (::_initialCustomerToEdit.isInitialized) {
        super._parseInputtedCustomer().copy(id = _initialCustomerToEdit.id)
      } else {
        super._parseInputtedCustomer()
      }

  private suspend fun _selectCustomerById(customerId: Long?): CustomerModel? =
      _customerRepository.selectById(customerId).also {
        if (it == null) _onSnackbarShown(StringResource(R.string.createCustomer_fetchCustomerError))
      }

  private suspend fun _updateCustomer(customer: CustomerModel) {
    _customerRepository.update(customer).let { effected ->
      _onSnackbarShown(
          if (effected > 0) {
            PluralResource(R.plurals.createCustomer_updated_n_customer, effected, effected)
          } else {
            StringResource(R.string.createCustomer_updateCustomerError)
          })
      if (effected > 0) {
        _editResultEvent.updateEvent(
            data = EditCustomerResultState(customer.id), onSet = { it }, onReset = { null })
      }
    }
  }

  private fun _loadCustomerToEdit() {
    viewModelScope.launch(_dispatcher) {
      // The initial customer ID shouldn't be null when editing data.
      _selectCustomerById(
              _savedStateHandle.get<Long>(
                  EditCustomerFragment.Arguments.INITIAL_CUSTOMER_ID_TO_EDIT_LONG.key())!!)
          ?.let {
            withContext(Dispatchers.Main) {
              _initialCustomerToEdit = it
              onNameTextChanged(it.name)
              onBalanceChanged(it.balance)
              onDebtChanged(it.debt)
            }
          }
    }
  }
}
