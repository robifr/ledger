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

package com.robifr.ledger.ui.product.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.display.ProductSortMethod
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductPaginatedInfo
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
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
class ProductViewModel(
    maxPaginatedItemPerPage: Int = 20,
    maxPaginatedItemInMemory: Int = maxPaginatedItemPerPage * 3,
    private val _dispatcher: CoroutineDispatcher,
    private val _productRepository: ProductRepository
) : ViewModel() {
  private var _expandedProductJob: Job? = null
  private val _paginationManager: PaginationManager<ProductPaginatedInfo> =
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
            _productRepository.countFilteredProducts(filterView.parseInputtedFilters())
          },
          _selectItemsByPageOffset = { pageNumber, limit ->
            _productRepository.selectPaginatedInfoByOffset(
                pageNumber = pageNumber,
                itemPerPage = _paginationManager.maxItemPerPage,
                limit = limit,
                sortMethod = _uiState.safeValue.sortMethod,
                filters = filterView.parseInputtedFilters())
          })
  private val _productChangedListener: ModelSyncListener<ProductModel, Unit> =
      ModelSyncListener(
          onSync = { _, _ ->
            onReloadPage(
                _uiState.safeValue.pagination.firstLoadedPageNumber,
                _uiState.safeValue.pagination.lastLoadedPageNumber)
          })

  private val _uiEvent: SafeMutableLiveData<ProductEvent> = SafeMutableLiveData(ProductEvent())
  val uiEvent: SafeLiveData<ProductEvent>
    get() = _uiEvent

  private val _uiState: SafeMutableLiveData<ProductState> =
      SafeMutableLiveData(
          ProductState(
              pagination =
                  PaginationState(
                      isLoading = false,
                      firstLoadedPageNumber = 1,
                      lastLoadedPageNumber = 1,
                      isRecyclerStateIdle = false,
                      paginatedItems = listOf(),
                      totalItem = 0L,
                  ),
              expandedProductIndex = -1,
              isProductMenuDialogShown = false,
              selectedProductMenu = null,
              sortMethod = ProductSortMethod(ProductSortMethod.SortBy.NAME, true),
              isNoProductsAddedIllustrationVisible = false,
              isSortMethodDialogShown = false))
  val uiState: SafeLiveData<ProductState>
    get() = _uiState

  val filterView: ProductFilterViewModel by lazy { ProductFilterViewModel(this) }

  @Inject
  constructor(
      @IoDispatcher dispatcher: CoroutineDispatcher,
      productRepository: ProductRepository
  ) : this(_dispatcher = dispatcher, _productRepository = productRepository)

  init {
    _productRepository.addModelChangedListener(_productChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    onReloadPage(1, 1)
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

  fun onProductMenuDialogShown(selectedProduct: ProductPaginatedInfo) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            isProductMenuDialogShown = true, selectedProductMenu = selectedProduct))
  }

  fun onProductMenuDialogClosed() {
    _uiState.setValue(
        _uiState.safeValue.copy(isProductMenuDialogShown = false, selectedProductMenu = null))
  }

  fun onSortMethodChanged(sortMethod: ProductSortMethod) {
    _uiState.setValue(_uiState.safeValue.copy(sortMethod = sortMethod))
    onReloadPage(1, 1)
  }

  /**
   * Sort [PaginationState.paginatedItems] based on specified [ProductSortMethod.SortBy] type. Doing
   * so will reverse the order — Ascending becomes descending and vice versa. Use
   * [onSortMethodChanged] that takes a [ProductSortMethod] if you want to apply the order by
   * yourself.
   */
  fun onSortMethodChanged(sortBy: ProductSortMethod.SortBy) {
    onSortMethodChanged(
        ProductSortMethod(
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

  fun onDeleteProduct(productId: Long?) {
    viewModelScope.launch(_dispatcher) {
      _productRepository.delete(productId).let { effected ->
        _onSnackbarShown(
            if (effected > 0) {
              PluralResource(R.plurals.product_deleted_n_product, effected, effected)
            } else {
              StringResource(R.string.product_deleteProductError)
            })
      }
    }
  }

  fun onReloadPage(firstVisiblePageNumber: Int, lastVisiblePageNumber: Int) {
    viewModelScope.launch(_dispatcher) {
      val isTableEmpty: Boolean = _productRepository.isTableEmpty()
      _paginationManager.onReloadPage(firstVisiblePageNumber, lastVisiblePageNumber) {
        _setNoProductsAddedIllustrationVisible(isTableEmpty)
      }
    }
  }

  private fun _setNoProductsAddedIllustrationVisible(isVisible: Boolean) {
    _uiState.setValue(_uiState.safeValue.copy(isNoProductsAddedIllustrationVisible = isVisible))
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
}
