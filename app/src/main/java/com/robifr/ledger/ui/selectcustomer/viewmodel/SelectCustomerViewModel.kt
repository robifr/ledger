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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.data.display.CustomerSorter
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.selectcustomer.SelectCustomerFragment
import com.robifr.ledger.util.updateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

  private val _uiEvent: SafeMutableLiveData<SelectCustomerEvent> =
      SafeMutableLiveData(SelectCustomerEvent())
  val uiEvent: SafeLiveData<SelectCustomerEvent>
    get() = _uiEvent

  private val _uiState: SafeMutableLiveData<SelectCustomerState> =
      SafeMutableLiveData(
          SelectCustomerState(
              initialSelectedCustomer =
                  savedStateHandle.get<CustomerModel>(
                      SelectCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_PARCELABLE.key()),
              selectedCustomerOnDatabase = null,
              customers = listOf(),
              expandedCustomerIndex = -1,
              isSelectedCustomerPreviewExpanded = false))
  val uiState: SafeLiveData<SelectCustomerState>
    get() = _uiState

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
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(0)) // Update header holder.
  }

  fun onExpandedCustomerIndexChanged(index: Int) {
    // Update both previous and current expanded product. +1 offset because header holder.
    _onRecyclerAdapterRefreshed(
        RecyclerAdapterState.ItemChanged(
            listOfNotNull(
                _uiState.safeValue.expandedCustomerIndex.takeIf { it != -1 && it != index }?.inc(),
                index + 1)))
    _uiState.setValue(
        _uiState.safeValue.copy(
            expandedCustomerIndex =
                if (_uiState.safeValue.expandedCustomerIndex != index) index else -1))
  }

  fun onCustomerSelected(customer: CustomerModel?) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SelectCustomerResultState(customer?.id),
          onSet = { this?.copy(selectResult = it) },
          onReset = { this?.copy(selectResult = null) })
    }
  }

  private fun _onCustomersChanged(customers: List<CustomerModel>) {
    _uiState.setValue(_uiState.safeValue.copy(customers = _sorter.sort(customers)))
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.DataSetChanged)
  }

  private fun _onSelectedCustomerOnDatabaseChanged(customer: CustomerModel?) {
    _uiState.setValue(_uiState.safeValue.copy(selectedCustomerOnDatabase = customer))
    // Update `selectedItemDescription` in header holder.
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(0))
  }

  private fun _onRecyclerAdapterRefreshed(state: RecyclerAdapterState) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = state,
          onSet = { this?.copy(recyclerAdapter = it) },
          onReset = { this?.copy(recyclerAdapter = null) })
    }
  }

  private suspend fun _selectAllCustomers(): List<CustomerModel> = _customerRepository.selectAll()

  private fun _loadAllCustomers() {
    viewModelScope.launch(_dispatcher) {
      val customers: List<CustomerModel> = _selectAllCustomers()
      val selectedCustomerOnDb: CustomerModel? =
          _customerRepository.selectById(_uiState.safeValue.initialSelectedCustomer?.id)
      withContext(Dispatchers.Main) {
        _onCustomersChanged(customers)
        _onSelectedCustomerOnDatabaseChanged(selectedCustomerOnDb)
      }
    }
  }
}
