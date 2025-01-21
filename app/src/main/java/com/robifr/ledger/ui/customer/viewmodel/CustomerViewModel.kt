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

package com.robifr.ledger.ui.customer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.display.CustomerSortMethod
import com.robifr.ledger.data.display.CustomerSorter
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.PluralResource
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class CustomerViewModel
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _customerRepository: CustomerRepository
) : ViewModel() {
  private val _sorter: CustomerSorter = CustomerSorter()
  private val _customerChangedListener: ModelSyncListener<CustomerModel> =
      ModelSyncListener(
          currentModel = { _uiState.safeValue.customers },
          onSyncModels = { filterView._onFiltersChanged(customers = it) })

  private val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  private val _uiState: SafeMutableLiveData<CustomerState> =
      SafeMutableLiveData(
          CustomerState(
              customers = listOf(),
              expandedCustomerIndex = -1,
              isCustomerMenuDialogShown = false,
              selectedCustomerMenu = null,
              sortMethod = _sorter.sortMethod,
              isSortMethodDialogShown = false))
  val uiState: SafeLiveData<CustomerState>
    get() = _uiState

  private val _recyclerAdapterState: SingleLiveEvent<RecyclerAdapterState> = SingleLiveEvent()
  val recyclerAdapterState: LiveData<RecyclerAdapterState>
    get() = _recyclerAdapterState

  val filterView: CustomerFilterViewModel =
      CustomerFilterViewModel(
          _viewModel = this,
          _dispatcher = _dispatcher,
          _selectAllCustomers = { _selectAllCustomers() })

  init {
    _customerRepository.addModelChangedListener(_customerChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadAllCustomers()
  }

  override fun onCleared() {
    _customerRepository.removeModelChangedListener(_customerChangedListener)
  }

  fun onCustomersChanged(customers: List<CustomerModel>) {
    _uiState.setValue(_uiState.safeValue.copy(customers = _sorter.sort(customers)))
    _recyclerAdapterState.setValue(RecyclerAdapterState.DataSetChanged)
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

  fun onCustomerMenuDialogShown(selectedCustomer: CustomerModel) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            isCustomerMenuDialogShown = true, selectedCustomerMenu = selectedCustomer))
  }

  fun onCustomerMenuDialogClosed() {
    _uiState.setValue(
        _uiState.safeValue.copy(isCustomerMenuDialogShown = false, selectedCustomerMenu = null))
  }

  fun onSortMethodChanged(
      sortMethod: CustomerSortMethod,
      customers: List<CustomerModel> = _uiState.safeValue.customers
  ) {
    _sorter.sortMethod = sortMethod
    _uiState.setValue(_uiState.safeValue.copy(sortMethod = sortMethod))
    onCustomersChanged(customers)
  }

  /**
   * Sort [CustomerState.customers] based on specified [CustomerSortMethod.SortBy] type. Doing so
   * will reverse the order — Ascending becomes descending and vice versa. Use [onSortMethodChanged]
   * that takes a [CustomerSortMethod] if you want to apply the order by yourself.
   */
  fun onSortMethodChanged(
      sortBy: CustomerSortMethod.SortBy,
      customers: List<CustomerModel> = _uiState.safeValue.customers
  ) {
    onSortMethodChanged(
        CustomerSortMethod(
            sortBy,
            // Reverse sort order when selecting same sort option.
            if (_uiState.safeValue.sortMethod.sortBy == sortBy) {
              !_uiState.safeValue.sortMethod.isAscending
            } else {
              _uiState.safeValue.sortMethod.isAscending
            }),
        customers)
  }

  fun onSortMethodDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = true))
  }

  fun onSortMethodDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = false))
  }

  fun onDeleteCustomer(customer: CustomerModel) {
    viewModelScope.launch(_dispatcher) {
      _customerRepository.delete(customer).let { effected ->
        _snackbarState.postValue(
            SnackbarState(
                if (effected > 0) {
                  PluralResource(R.plurals.customer_deleted_n_customer, effected, effected)
                } else {
                  StringResource(R.string.customer_deleteCustomerError)
                }))
      }
    }
  }

  private suspend fun _selectAllCustomers(): List<CustomerModel> = _customerRepository.selectAll()

  private fun _loadAllCustomers() {
    viewModelScope.launch(_dispatcher) {
      _selectAllCustomers().let {
        withContext(Dispatchers.Main) { filterView._onFiltersChanged(customers = it) }
      }
    }
  }
}
