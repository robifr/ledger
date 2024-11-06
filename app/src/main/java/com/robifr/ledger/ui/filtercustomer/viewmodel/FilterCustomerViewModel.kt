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
import com.robifr.ledger.R
import com.robifr.ledger.data.display.CustomerSorter
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import com.robifr.ledger.ui.filtercustomer.FilterCustomerFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
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

  private val _resultState: SingleLiveEvent<FilterCustomerResultState> = SingleLiveEvent()
  val resultState: LiveData<FilterCustomerResultState>
    get() = _resultState

  init {
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadAllCustomers()
  }

  fun onCustomerCheckedChanged(vararg customer: CustomerModel) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            filteredCustomers =
                _uiState.safeValue.filteredCustomers.toMutableList().apply {
                  customer.forEach { if (!contains(it)) add(it) else remove(it) }
                }))
  }

  fun onExpandedCustomerIndexChanged(index: Int) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            expandedCustomerIndex =
                if (_uiState.safeValue.expandedCustomerIndex != index) index else -1))
  }

  fun onSave() {
    _resultState.setValue(
        FilterCustomerResultState(
            _uiState.safeValue.filteredCustomers.mapNotNull(CustomerModel::id)))
  }

  private fun _onCustomersChanged(customers: List<CustomerModel>) {
    _uiState.setValue(_uiState.safeValue.copy(customers = _sorter.sort(customers)))
  }

  private suspend fun _selectAllCustomers(): List<CustomerModel> =
      _customerRepository.selectAll().await().also { customers: List<CustomerModel> ->
        if (customers.isEmpty()) {
          _snackbarState.postValue(
              SnackbarState(StringResource(R.string.filterCustomer_fetchAllCustomerError)))
        }
      }

  private fun _loadAllCustomers() {
    viewModelScope.launch(_dispatcher) {
      _selectAllCustomers().let { customers: List<CustomerModel> ->
        withContext(Dispatchers.Main) {
          val filteredCustomerIds: LongArray =
              _savedStateHandle.get<LongArray>(
                  FilterCustomerFragment.Arguments.INITIAL_FILTERED_CUSTOMER_IDS_LONG_ARRAY.key)
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
