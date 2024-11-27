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

package com.robifr.ledger.ui.selectcustomer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.display.CustomerSorter
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import com.robifr.ledger.ui.selectcustomer.SelectCustomerFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SelectCustomerViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _customerRepository: CustomerRepository
) : ViewModel() {
  private val _sorter: CustomerSorter = CustomerSorter()
  private val _customerChangedListener: ModelSyncListener<CustomerModel> =
      ModelSyncListener(
          currentModel = { _uiState.safeValue.customers }, onSyncModels = ::_onCustomersChanged)

  private val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  private val _uiState: SafeMutableLiveData<SelectCustomerState> =
      SafeMutableLiveData(
          SelectCustomerState(
              initialSelectedCustomer =
                  savedStateHandle.get<CustomerModel>(
                      SelectCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_PARCELABLE.key),
              selectedCustomerOnDatabase = null,
              customers = listOf(),
              expandedCustomerIndex = -1,
              isSelectedCustomerPreviewExpanded = false))
  val uiState: SafeLiveData<SelectCustomerState>
    get() = _uiState

  private val _recyclerAdapterState: SingleLiveEvent<RecyclerAdapterState> = SingleLiveEvent()
  val recyclerAdapterState: LiveData<RecyclerAdapterState>
    get() = _recyclerAdapterState

  private val _resultState: SingleLiveEvent<SelectCustomerResultState> = SingleLiveEvent()
  val resultState: LiveData<SelectCustomerResultState>
    get() = _resultState

  init {
    _customerRepository.addModelChangedListener(_customerChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadAllCustomers()
  }

  override fun onCleared() {
    _customerRepository.removeModelChangedListener(_customerChangedListener)
  }

  fun onSelectedCustomerPreviewExpanded(isExpanded: Boolean) {
    _uiState.setValue(_uiState.safeValue.copy(isSelectedCustomerPreviewExpanded = isExpanded))
    _recyclerAdapterState.setValue(RecyclerAdapterState.ItemChanged(0)) // Update header holder.
  }

  fun onExpandedCustomerIndexChanged(index: Int) {
    // Update both previous and current expanded product. +1 offset because header holder.
    _recyclerAdapterState.setValue(
        RecyclerAdapterState.ItemChanged(_uiState.safeValue.expandedCustomerIndex + 1, index + 1))
    _uiState.setValue(
        _uiState.safeValue.copy(
            expandedCustomerIndex =
                if (_uiState.safeValue.expandedCustomerIndex != index) index else -1))
  }

  fun onCustomerSelected(customer: CustomerModel?) {
    _resultState.setValue(SelectCustomerResultState(customer?.id))
  }

  private fun _onCustomersChanged(customers: List<CustomerModel>) {
    _uiState.setValue(_uiState.safeValue.copy(customers = _sorter.sort(customers)))
    _recyclerAdapterState.setValue(RecyclerAdapterState.DataSetChanged)
  }

  private fun _onSelectedCustomerOnDatabaseChanged(customer: CustomerModel?) {
    _uiState.setValue(_uiState.safeValue.copy(selectedCustomerOnDatabase = customer))
    // Update `selectedItemDescription` in header holder.
    _recyclerAdapterState.setValue(RecyclerAdapterState.ItemChanged(0))
  }

  private suspend fun _selectAllCustomers(): List<CustomerModel> =
      _customerRepository.selectAll().await().also { customers: List<CustomerModel> ->
        if (customers.isEmpty()) {
          _snackbarState.postValue(
              SnackbarState(StringResource(R.string.selectCustomer_fetchAllCustomerError)))
        }
      }

  private fun _loadAllCustomers() {
    viewModelScope.launch(_dispatcher) {
      val customers: List<CustomerModel> = _selectAllCustomers()
      val selectedCustomerOnDb: CustomerModel? =
          _customerRepository.selectById(_uiState.safeValue.initialSelectedCustomer?.id).await()
      withContext(Dispatchers.Main) {
        _onCustomersChanged(customers)
        _onSelectedCustomerOnDatabaseChanged(selectedCustomerOnDb)
      }
    }
  }
}
