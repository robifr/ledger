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

package io.github.robifr.ledger.ui.selectcustomer.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.robifr.ledger.data.display.CustomerFilters
import io.github.robifr.ledger.data.display.CustomerSortMethod
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.CustomerPaginatedInfo
import io.github.robifr.ledger.di.IoDispatcher
import io.github.robifr.ledger.repository.CustomerRepository
import io.github.robifr.ledger.repository.ModelSyncListener
import io.github.robifr.ledger.ui.common.pagination.PaginationManager
import io.github.robifr.ledger.ui.common.pagination.PaginationState
import io.github.robifr.ledger.ui.common.state.RecyclerAdapterState
import io.github.robifr.ledger.ui.common.state.SafeLiveData
import io.github.robifr.ledger.ui.common.state.SafeMutableLiveData
import io.github.robifr.ledger.ui.common.state.updateEvent
import io.github.robifr.ledger.ui.selectcustomer.SelectCustomerFragment
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class SelectCustomerViewModel(
    maxPaginatedItemPerPage: Int = 20,
    maxPaginatedItemInMemory: Int = maxPaginatedItemPerPage * 3,
    savedStateHandle: SavedStateHandle,
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
            _customerRepository.countFilteredCustomers(CustomerFilters(null to null, null to null))
          },
          _selectItemsByPageOffset = { pageNumber, limit ->
            _customerRepository.selectPaginatedInfoByOffset(
                pageNumber = pageNumber,
                itemPerPage = _paginationManager.maxItemPerPage,
                limit = limit,
                sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, true),
                filters = CustomerFilters(null to null, null to null))
          })
  private val _customerChangedListener: ModelSyncListener<CustomerModel, Unit> =
      ModelSyncListener(
          onSync = { _, _ ->
            _onReloadPage(
                _uiState.safeValue.pagination.firstLoadedPageNumber,
                _uiState.safeValue.pagination.lastLoadedPageNumber)
          })

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
              isSelectedCustomerPreviewExpanded = false))
  val uiState: SafeLiveData<SelectCustomerState>
    get() = _uiState

  @Inject
  constructor(
      savedStateHandle: SavedStateHandle,
      @IoDispatcher dispatcher: CoroutineDispatcher,
      customerRepository: CustomerRepository
  ) : this(
      savedStateHandle = savedStateHandle,
      _dispatcher = dispatcher,
      _customerRepository = customerRepository)

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

  fun onSelectedCustomerPreviewExpanded(isExpanded: Boolean) {
    _uiState.setValue(_uiState.safeValue.copy(isSelectedCustomerPreviewExpanded = isExpanded))
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(0)) // Update header holder.
  }

  fun onExpandedCustomerIndexChanged(index: Int) {
    _expandedCustomerJob?.cancel()
    _expandedCustomerJob =
        viewModelScope.launch {
          delay(150)
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

  fun onCustomerSelected(customer: CustomerPaginatedInfo?) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SelectCustomerResultState(customer?.id),
          onSet = { this?.copy(selectResult = it) },
          onReset = { this?.copy(selectResult = null) })
    }
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

  private fun _onReloadPage(firstVisiblePageNumber: Int, lastVisiblePageNumber: Int) {
    viewModelScope.launch(_dispatcher) {
      val selectedCustomerOnDb: CustomerModel? =
          _customerRepository.selectById(_uiState.safeValue.initialSelectedCustomer?.id)
      _paginationManager.onReloadPage(firstVisiblePageNumber, lastVisiblePageNumber) {
        _onSelectedCustomerOnDatabaseChanged(selectedCustomerOnDb)
      }
    }
  }
}
