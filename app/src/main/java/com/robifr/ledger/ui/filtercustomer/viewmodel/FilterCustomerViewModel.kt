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

package com.robifr.ledger.ui.filtercustomer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.data.display.CustomerSorter
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.filtercustomer.FilterCustomerFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class FilterCustomerViewModel
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _customerRepository: CustomerRepository,
    private val _savedStateHandle: SavedStateHandle
) : ViewModel() {
  private val _sorter: CustomerSorter = CustomerSorter()

  private val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  private val _uiState: SafeMutableLiveData<FilterCustomerState> =
      SafeMutableLiveData(
          FilterCustomerState(
              customers = listOf(), expandedCustomerIndex = -1, filteredCustomers = listOf()))
  val uiState: SafeLiveData<FilterCustomerState>
    get() = _uiState

  private val _recyclerAdapterState: SingleLiveEvent<RecyclerAdapterState> = SingleLiveEvent()
  val recyclerAdapterState: LiveData<RecyclerAdapterState>
    get() = _recyclerAdapterState

  private val _resultState: SingleLiveEvent<FilterCustomerResultState> = SingleLiveEvent()
  val resultState: LiveData<FilterCustomerResultState>
    get() = _resultState

  init {
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadAllCustomers()
  }

  fun onCustomerCheckedChanged(vararg customers: CustomerModel) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            filteredCustomers =
                _uiState.safeValue.filteredCustomers.toMutableList().apply {
                  customers.forEach { if (!contains(it)) add(it) else remove(it) }
                }))
    _recyclerAdapterState.setValue(
        RecyclerAdapterState.ItemChanged(
            0, // Index 0 to update header holder.
            *customers
                .mapNotNull { checkedCustomer ->
                  _uiState.safeValue.customers
                      .indexOfFirst { it.id == checkedCustomer.id }
                      .takeIf { it != -1 }
                      ?.let { it + 1 } // +1 offset because header holder.
                }
                .toIntArray()))
  }

  fun onExpandedCustomerIndexChanged(index: Int) {
    // Update both previous and current expanded product. +1 offset because header holder.
    _recyclerAdapterState.setValue(
        RecyclerAdapterState.ItemChanged(
            listOfNotNull(
                _uiState.safeValue.expandedCustomerIndex.takeIf { it != -1 }?.let { it + 1 },
                index + 1)))
    _uiState.setValue(
        _uiState.safeValue.copy(
            expandedCustomerIndex =
                if (_uiState.safeValue.expandedCustomerIndex != index) index else -1))
  }

  fun onSave() {
    _resultState.setValue(
        FilterCustomerResultState(_uiState.safeValue.filteredCustomers.mapNotNull { it.id }))
  }

  private fun _onCustomersChanged(customers: List<CustomerModel>) {
    _uiState.setValue(_uiState.safeValue.copy(customers = _sorter.sort(customers)))
    _recyclerAdapterState.setValue(RecyclerAdapterState.DataSetChanged)
  }

  private suspend fun _selectAllCustomers(): List<CustomerModel> = _customerRepository.selectAll()

  private fun _loadAllCustomers() {
    viewModelScope.launch(_dispatcher) {
      _selectAllCustomers().let { customers: List<CustomerModel> ->
        withContext(Dispatchers.Main) {
          val filteredCustomerIds: LongArray =
              _savedStateHandle.get<LongArray>(
                  FilterCustomerFragment.Arguments.INITIAL_FILTERED_CUSTOMER_IDS_LONG_ARRAY.key())
                  ?: longArrayOf()
          val filteredCustomer: List<CustomerModel> =
              customers.filter { it.id != null && filteredCustomerIds.contains(it.id) }
          _onCustomersChanged(customers)
          onCustomerCheckedChanged(*filteredCustomer.toTypedArray())
        }
      }
    }
  }
}
