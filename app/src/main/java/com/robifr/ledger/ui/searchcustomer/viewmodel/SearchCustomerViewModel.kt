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

package com.robifr.ledger.ui.searchcustomer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
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
import com.robifr.ledger.ui.search.viewmodel.SearchState
import com.robifr.ledger.ui.searchcustomer.SearchCustomerFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class SearchCustomerViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _customerRepository: CustomerRepository
) : ViewModel() {
  private val _customerChangedListener: ModelSyncListener<CustomerModel> =
      ModelSyncListener(
          currentModel = { _uiState.safeValue.customers }, onSyncModels = ::_onCustomersChanged)
  private var _searchJob: Job? = null

  private val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  private val _uiState: SafeMutableLiveData<SearchCustomerState> =
      SafeMutableLiveData(
          SearchCustomerState(
              isSelectionEnabled =
                  savedStateHandle.get<Boolean>(
                      SearchCustomerFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key()) ?: false,
              isToolbarVisible =
                  savedStateHandle.get<Boolean>(
                      SearchCustomerFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key()) ?: true,
              initialQuery =
                  savedStateHandle.get<String>(
                      SearchCustomerFragment.Arguments.INITIAL_QUERY_STRING.key()) ?: "",
              query = "",
              initialSelectedCustomerIds =
                  savedStateHandle
                      .get<LongArray>(
                          SearchCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_IDS_LONG_ARRAY
                              .key())
                      ?.toList() ?: listOf(),
              customers = listOf(),
              expandedCustomerIndex = -1,
              isCustomerMenuDialogShown = false,
              selectedCustomerMenu = null))
  val uiState: SafeLiveData<SearchCustomerState>
    get() = _uiState

  private val _recyclerAdapterState: SingleLiveEvent<RecyclerAdapterState> = SingleLiveEvent()
  val recyclerAdapterState: LiveData<RecyclerAdapterState>
    get() = _recyclerAdapterState

  private val _resultState: SingleLiveEvent<SearchCustomerResultState> = SingleLiveEvent()
  val resultState: LiveData<SearchCustomerResultState>
    get() = _resultState

  init {
    _customerRepository.addModelChangedListener(_customerChangedListener)
  }

  override fun onCleared() {
    _customerRepository.removeModelChangedListener(_customerChangedListener)
  }

  fun onSearch(query: String) {
    // Remove old job to ensure old query results don't appear in the future.
    _searchJob?.cancel()
    _searchJob =
        viewModelScope.launch(_dispatcher) {
          delay(300L)
          _customerRepository.search(query).let {
            _uiState.postValue(_uiState.safeValue.copy(query = query, customers = it))
            _recyclerAdapterState.postValue(RecyclerAdapterState.DataSetChanged)
          }
        }
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

  fun onCustomerSelected(customer: CustomerModel?) {
    _resultState.setValue(SearchCustomerResultState(customer?.id))
  }

  fun onDeleteCustomer(customer: CustomerModel) {
    viewModelScope.launch(_dispatcher) {
      _customerRepository.delete(customer).also { effected ->
        _snackbarState.postValue(
            SnackbarState(
                if (effected > 0) {
                  PluralResource(R.plurals.searchCustomer_deleted_n_customer, effected, effected)
                } else {
                  StringResource(R.string.searchCustomer_deleteCustomerError)
                }))
      }
    }
  }

  fun onSearchUiStateChanged(state: SearchState) {
    _uiState.setValue(_uiState.safeValue.copy(query = state.query, customers = state.customers))
    _recyclerAdapterState.setValue(RecyclerAdapterState.DataSetChanged)
  }

  private fun _onCustomersChanged(customers: List<CustomerModel>) {
    _uiState.setValue(_uiState.safeValue.copy(customers = customers))
    _recyclerAdapterState.setValue(RecyclerAdapterState.DataSetChanged)
  }
}
