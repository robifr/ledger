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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.display.CustomerSortMethod
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.CustomerPaginatedInfo
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.common.PluralResource
import com.robifr.ledger.ui.common.StringResource
import com.robifr.ledger.ui.common.StringResourceType
import com.robifr.ledger.ui.common.pagination.PaginationManager
import com.robifr.ledger.ui.common.pagination.PaginationState
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import com.robifr.ledger.ui.common.state.SnackbarState
import com.robifr.ledger.ui.common.state.updateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class CustomerViewModel(
    maxPaginatedItemPerPage: Int = 20,
    maxPaginatedItemInMemory: Int = maxPaginatedItemPerPage * 3,
    private val _dispatcher: CoroutineDispatcher,
    private val _customerRepository: CustomerRepository
) : ViewModel() {
  private var _expandedCustomerJob: Job? = null
  private val _paginationManager: PaginationManager<CustomerPaginatedInfo> =
      PaginationManager(
          state = { _uiState.safeValue.pagination },
          onStateChanged = { _uiState.setValue(_uiState.safeValue.copy(pagination = it)) },
          maxItemPerPage = maxPaginatedItemPerPage,
          maxItemInMemory = maxPaginatedItemInMemory,
          _coroutineScope = viewModelScope,
          _dispatcher = _dispatcher,
          _onNotifyRecyclerState = {
            _onRecyclerAdapterRefreshed(
                when (it) {
                  // +1 offset because header holder.
                  is RecyclerAdapterState.ItemRangeChanged ->
                      RecyclerAdapterState.ItemRangeChanged(
                          it.positionStart + 1, it.itemCount, it.payload)
                  is RecyclerAdapterState.ItemRangeInserted ->
                      RecyclerAdapterState.ItemRangeInserted(it.positionStart + 1, it.itemCount)
                  is RecyclerAdapterState.ItemRangeRemoved ->
                      RecyclerAdapterState.ItemRangeRemoved(it.positionStart + 1, it.itemCount)
                  else -> it
                })
          },
          _countTotalItem = {
            _customerRepository.countFilteredCustomers(filterView._parseInputtedFilters())
          },
          _selectItemsByPageOffset = { pageNumber, limit ->
            _customerRepository.selectPaginatedInfoByOffset(
                pageNumber,
                limit,
                _uiState.safeValue.sortMethod,
                filterView._parseInputtedFilters())
          })
  private val _customerChangedListener: ModelSyncListener<CustomerModel, CustomerModel> =
      ModelSyncListener(
          onSync = { _, _ ->
            _onReloadPage(
                _uiState.safeValue.pagination.firstLoadedPageNumber,
                _uiState.safeValue.pagination.lastLoadedPageNumber)
          })

  private val _uiEvent: SafeMutableLiveData<CustomerEvent> = SafeMutableLiveData(CustomerEvent())
  val uiEvent: SafeLiveData<CustomerEvent>
    get() = _uiEvent

  private val _uiState: SafeMutableLiveData<CustomerState> =
      SafeMutableLiveData(
          CustomerState(
              pagination =
                  PaginationState(
                      isLoading = false,
                      firstLoadedPageNumber = 1,
                      lastLoadedPageNumber = 1,
                      isRecyclerStateIdle = false,
                      paginatedItems = listOf(),
                      totalItem = 0L,
                  ),
              expandedCustomerIndex = -1,
              isCustomerMenuDialogShown = false,
              selectedCustomerMenu = null,
              isNoCustomersAddedIllustrationVisible = false,
              sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, true),
              isSortMethodDialogShown = false))
  val uiState: SafeLiveData<CustomerState>
    get() = _uiState

  val filterView: CustomerFilterViewModel = CustomerFilterViewModel { _onReloadPage(1, 1) }

  @Inject
  constructor(
      @IoDispatcher dispatcher: CoroutineDispatcher,
      customerRepository: CustomerRepository
  ) : this(_dispatcher = dispatcher, _customerRepository = customerRepository)

  init {
    _customerRepository.addModelChangedListener(_customerChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _onReloadPage(1, 1)
  }

  override fun onCleared() {
    _customerRepository.removeModelChangedListener(_customerChangedListener)
  }

  fun onLoadPreviousPage() {
    _paginationManager.onLoadPreviousPage {
      if (_uiState.safeValue.expandedCustomer == null) return@onLoadPreviousPage
      // Re-expand current expanded customer.
      val expandedCustomerIndex: Int =
          _uiState.safeValue.pagination.paginatedItems.indexOfFirst {
            it.id == _uiState.safeValue.expandedCustomer?.id
          }
      if (expandedCustomerIndex != -1) {
        // +1 offset because header holder.
        _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(expandedCustomerIndex + 1))
      }
    }
  }

  fun onLoadNextPage() {
    _paginationManager.onLoadNextPage {
      if (_uiState.safeValue.expandedCustomer == null) return@onLoadNextPage
      // Re-expand current expanded customer.
      val expandedCustomerIndex: Int =
          _uiState.safeValue.pagination.paginatedItems.indexOfFirst {
            it.id == _uiState.safeValue.expandedCustomer?.id
          }
      if (expandedCustomerIndex != -1) {
        // +1 offset because header holder.
        _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(expandedCustomerIndex + 1))
      }
    }
  }

  fun onRecyclerStateIdle(isIdle: Boolean) {
    _paginationManager.onRecyclerStateIdleNotifyItemRangeChanged(isIdle)
  }

  fun onExpandedCustomerIndexChanged(index: Int) {
    _expandedCustomerJob?.cancel()
    _expandedCustomerJob =
        viewModelScope.launch {
          delay(200)
          val previousExpandedIndex: Int = _uiState.safeValue.expandedCustomerIndex
          val shouldExpand: Boolean = previousExpandedIndex != index
          // Unlike queue, there's no need to load for the customer's
          // `CustomerPaginatedInfo.fullModel`. They're loaded by default.
          _uiState.setValue(
              _uiState.safeValue.copy(expandedCustomerIndex = if (shouldExpand) index else -1))
          // Update both previous and current expanded customer. +1 offset because header holder.
          _onRecyclerAdapterRefreshed(
              RecyclerAdapterState.ItemChanged(
                  listOfNotNull(
                      previousExpandedIndex.takeIf { it != -1 && it != index }?.inc(), index + 1)))
        }
  }

  fun onCustomerMenuDialogShown(selectedCustomer: CustomerPaginatedInfo) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            isCustomerMenuDialogShown = true, selectedCustomerMenu = selectedCustomer))
  }

  fun onCustomerMenuDialogClosed() {
    _uiState.setValue(
        _uiState.safeValue.copy(isCustomerMenuDialogShown = false, selectedCustomerMenu = null))
  }

  fun onSortMethodChanged(sortMethod: CustomerSortMethod) {
    _uiState.setValue(_uiState.safeValue.copy(sortMethod = sortMethod))
    _onReloadPage(1, 1)
  }

  /**
   * Sort [PaginationState.paginatedItems] based on specified [CustomerSortMethod.SortBy] type.
   * Doing so will reverse the order — Ascending becomes descending and vice versa. Use
   * [onSortMethodChanged] that takes a [CustomerSortMethod] if you want to apply the order by
   * yourself.
   */
  fun onSortMethodChanged(sortBy: CustomerSortMethod.SortBy) {
    onSortMethodChanged(
        CustomerSortMethod(
            sortBy,
            // Reverse sort order when selecting same sort option.
            if (_uiState.safeValue.sortMethod.sortBy == sortBy) {
              !_uiState.safeValue.sortMethod.isAscending
            } else {
              _uiState.safeValue.sortMethod.isAscending
            }))
  }

  fun onSortMethodDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = true))
  }

  fun onSortMethodDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = false))
  }

  fun onDeleteCustomer(customerId: Long?) {
    viewModelScope.launch(_dispatcher) {
      _customerRepository.delete(customerId).let { effected ->
        _onSnackbarShown(
            if (effected > 0) {
              PluralResource(R.plurals.customer_deleted_n_customer, effected, effected)
            } else {
              StringResource(R.string.customer_deleteCustomerError)
            })
      }
    }
  }

  private fun _onNoCustomersAddedIllustrationVisible(isVisible: Boolean) {
    _uiState.setValue(_uiState.safeValue.copy(isNoCustomersAddedIllustrationVisible = isVisible))
  }

  private fun _onSnackbarShown(messageRes: StringResourceType) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SnackbarState(messageRes),
          onSet = { this?.copy(snackbar = it) },
          onReset = { this?.copy(snackbar = null) })
    }
  }

  private fun _onRecyclerAdapterRefreshed(state: RecyclerAdapterState) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = state,
          onSet = { this?.copy(recyclerAdapter = it) },
          onReset = { this?.copy(recyclerAdapter = null) })
    }
  }

  private fun _onReloadPage(firstVisiblePageNumber: Int, lastVisiblePageNumber: Int) {
    viewModelScope.launch(_dispatcher) {
      val isTableEmpty: Boolean = _customerRepository.isTableEmpty()
      _paginationManager.onReloadPage(firstVisiblePageNumber, lastVisiblePageNumber) {
        _onNoCustomersAddedIllustrationVisible(isTableEmpty)
      }
    }
  }
}
