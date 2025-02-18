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

package com.robifr.ledger.ui.selectproduct.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.data.display.ProductFilters
import com.robifr.ledger.data.display.ProductSortMethod
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductPaginatedInfo
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.common.pagination.PaginationManager
import com.robifr.ledger.ui.common.pagination.PaginationState
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import com.robifr.ledger.ui.common.state.updateEvent
import com.robifr.ledger.ui.selectproduct.SelectProductFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class SelectProductViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _productRepository: ProductRepository
) : ViewModel() {
  private var _expandedProductJob: Job? = null
  private val _paginationManager: PaginationManager<ProductPaginatedInfo> =
      PaginationManager(
          state = { _uiState.safeValue.pagination },
          onStateChanged = { _uiState.setValue(_uiState.safeValue.copy(pagination = it)) },
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
            _productRepository.countFilteredProducts(ProductFilters(null to null))
          },
          _selectItemsByPageOffset = { pageNumber, limit ->
            _productRepository.selectPaginatedInfoByOffset(
                pageNumber,
                limit,
                ProductSortMethod(ProductSortMethod.SortBy.NAME, true),
                ProductFilters(null to null))
          })
  private val _productChangedListener: ModelSyncListener<ProductModel, ProductModel> =
      ModelSyncListener(
          onSync = { _, _ ->
            _onReloadPage(
                _uiState.safeValue.pagination.firstLoadedPageNumber,
                _uiState.safeValue.pagination.lastLoadedPageNumber)
          })

  private val _uiEvent: SafeMutableLiveData<SelectProductEvent> =
      SafeMutableLiveData(SelectProductEvent())
  val uiEvent: SafeLiveData<SelectProductEvent>
    get() = _uiEvent

  private val _uiState: SafeMutableLiveData<SelectProductState> =
      SafeMutableLiveData(
          SelectProductState(
              initialSelectedProduct =
                  savedStateHandle.get<ProductModel>(
                      SelectProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_PARCELABLE.key()),
              selectedProductOnDatabase = null,
              pagination =
                  PaginationState(
                      isLoading = false,
                      firstLoadedPageNumber = 1,
                      lastLoadedPageNumber = 1,
                      isRecyclerStateIdle = false,
                      paginatedItems = listOf(),
                      totalItem = 0,
                  ),
              expandedProductIndex = -1,
              isSelectedProductPreviewExpanded = false))
  val uiState: SafeLiveData<SelectProductState>
    get() = _uiState

  init {
    _productRepository.addModelChangedListener(_productChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _onReloadPage(1, 1)
  }

  override fun onCleared() {
    _productRepository.removeModelChangedListener(_productChangedListener)
  }

  fun onLoadPreviousPage() {
    _paginationManager.onLoadPreviousPage {
      if (_uiState.safeValue.expandedProduct == null) return@onLoadPreviousPage
      // Re-expand current expanded product.
      val expandedProductIndex: Int =
          _uiState.safeValue.pagination.paginatedItems.indexOfFirst {
            it.id == _uiState.safeValue.expandedProduct?.id
          }
      if (expandedProductIndex != -1) {
        // +1 offset because header holder.
        _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(expandedProductIndex + 1))
      }
    }
  }

  fun onLoadNextPage() {
    _paginationManager.onLoadNextPage {
      if (_uiState.safeValue.expandedProduct == null) return@onLoadNextPage
      // Re-expand current expanded product.
      val expandedProductIndex: Int =
          _uiState.safeValue.pagination.paginatedItems.indexOfFirst {
            it.id == _uiState.safeValue.expandedProduct?.id
          }
      if (expandedProductIndex != -1) {
        // +1 offset because header holder.
        _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(expandedProductIndex + 1))
      }
    }
  }

  fun onRecyclerStateIdle(isIdle: Boolean) {
    _paginationManager.onRecyclerStateIdleNotifyItemRangeChanged(isIdle)
  }

  fun onSelectedProductPreviewExpanded(isExpanded: Boolean) {
    _uiState.setValue(_uiState.safeValue.copy(isSelectedProductPreviewExpanded = isExpanded))
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(0)) // Update header holder.
  }

  fun onExpandedProductIndexChanged(index: Int) {
    _expandedProductJob?.cancel()
    _expandedProductJob =
        viewModelScope.launch {
          delay(200)
          val previousExpandedIndex: Int = _uiState.safeValue.expandedProductIndex
          val shouldExpand: Boolean = previousExpandedIndex != index
          // Unlike queue, there's no need to load for the product's
          // `ProductPaginatedInfo.fullModel`. They're loaded by default.
          _uiState.setValue(
              _uiState.safeValue.copy(expandedProductIndex = if (shouldExpand) index else -1))
          // Update both previous and current expanded product. +1 offset because header holder.
          _onRecyclerAdapterRefreshed(
              RecyclerAdapterState.ItemChanged(
                  listOfNotNull(
                      previousExpandedIndex.takeIf { it != -1 && it != index }?.inc(), index + 1)))
        }
  }

  fun onProductSelected(product: ProductPaginatedInfo?) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SelectProductResultState(product?.id),
          onSet = { this?.copy(selectResult = it) },
          onReset = { this?.copy(selectResult = null) })
    }
  }

  private fun _onSelectedProductOnDatabaseChanged(product: ProductModel?) {
    _uiState.setValue(_uiState.safeValue.copy(selectedProductOnDatabase = product))
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
      val selectedProductOnDb: ProductModel? =
          _productRepository.selectById(_uiState.safeValue.initialSelectedProduct?.id)
      _paginationManager.onReloadPage(firstVisiblePageNumber, lastVisiblePageNumber) {
        _onSelectedProductOnDatabaseChanged(selectedProductOnDb)
      }
    }
  }
}
