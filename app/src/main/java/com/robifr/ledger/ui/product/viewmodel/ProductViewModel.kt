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
import com.robifr.ledger.data.display.ProductSorter
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.common.PluralResource
import com.robifr.ledger.ui.common.StringResource
import com.robifr.ledger.ui.common.StringResourceType
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import com.robifr.ledger.ui.common.state.SnackbarState
import com.robifr.ledger.ui.common.state.updateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ProductViewModel
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _productRepository: ProductRepository
) : ViewModel() {
  private val _sorter: ProductSorter = ProductSorter()
  private val _productChangedListener: ModelSyncListener<ProductModel> =
      ModelSyncListener(
          currentModel = { _uiState.safeValue.products },
          onSyncModels = {
            viewModelScope.launch(_dispatcher) {
              val isTableEmpty: Boolean = _productRepository.isTableEmpty()
              withContext(Dispatchers.Main) {
                _setNoProductsAddedIllustrationVisible(isTableEmpty)
                filterView._onFiltersChanged(products = it)
              }
            }
          })

  private val _uiEvent: SafeMutableLiveData<ProductEvent> = SafeMutableLiveData(ProductEvent())
  val uiEvent: SafeLiveData<ProductEvent>
    get() = _uiEvent

  private val _uiState: SafeMutableLiveData<ProductState> =
      SafeMutableLiveData(
          ProductState(
              products = listOf(),
              expandedProductIndex = -1,
              isProductMenuDialogShown = false,
              selectedProductMenu = null,
              sortMethod = _sorter.sortMethod,
              isNoProductsAddedIllustrationVisible = false,
              isSortMethodDialogShown = false))
  val uiState: SafeLiveData<ProductState>
    get() = _uiState

  val filterView: ProductFilterViewModel =
      ProductFilterViewModel(
          _viewModel = this,
          _dispatcher = _dispatcher,
          _selectAllProducts = { _selectAllProducts() })

  init {
    _productRepository.addModelChangedListener(_productChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadAllProducts()
  }

  override fun onCleared() {
    _productRepository.removeModelChangedListener(_productChangedListener)
  }

  fun onProductsChanged(products: List<ProductModel>) {
    _uiState.setValue(_uiState.safeValue.copy(products = _sorter.sort(products)))
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.DataSetChanged)
  }

  fun onExpandedProductIndexChanged(index: Int) {
    // Update both previous and current expanded product. +1 offset because header holder.
    _onRecyclerAdapterRefreshed(
        RecyclerAdapterState.ItemChanged(
            listOfNotNull(
                _uiState.safeValue.expandedProductIndex.takeIf { it != -1 && it != index }?.inc(),
                index + 1)))
    _uiState.setValue(
        _uiState.safeValue.copy(
            expandedProductIndex =
                if (_uiState.safeValue.expandedProductIndex != index) index else -1))
  }

  fun onProductMenuDialogShown(selectedProduct: ProductModel) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            isProductMenuDialogShown = true, selectedProductMenu = selectedProduct))
  }

  fun onProductMenuDialogClosed() {
    _uiState.setValue(
        _uiState.safeValue.copy(isProductMenuDialogShown = false, selectedProductMenu = null))
  }

  fun onSortMethodChanged(
      sortMethod: ProductSortMethod,
      products: List<ProductModel> = _uiState.safeValue.products
  ) {
    _sorter.sortMethod = sortMethod
    _uiState.setValue(_uiState.safeValue.copy(sortMethod = sortMethod))
    onProductsChanged(products)
  }

  /**
   * Sort [ProductState.products] based on specified [ProductSortMethod.SortBy] type. Doing so will
   * reverse the order — Ascending becomes descending and vice versa. Use [onSortMethodChanged] that
   * takes a [ProductSortMethod] if you want to apply the order by yourself.
   */
  fun onSortMethodChanged(
      sortBy: ProductSortMethod.SortBy,
      products: List<ProductModel> = _uiState.safeValue.products
  ) {
    onSortMethodChanged(
        ProductSortMethod(
            sortBy,
            // Reverse sort order when selecting same sort option.
            if (_uiState.safeValue.sortMethod.sortBy == sortBy) {
              !_uiState.safeValue.sortMethod.isAscending
            } else {
              _uiState.safeValue.sortMethod.isAscending
            }),
        products)
  }

  fun onSortMethodDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = true))
  }

  fun onSortMethodDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = false))
  }

  fun onDeleteProduct(product: ProductModel) {
    viewModelScope.launch(_dispatcher) {
      _productRepository.delete(product.id).let { effected ->
        _onSnackbarShown(
            if (effected > 0) {
              PluralResource(R.plurals.product_deleted_n_product, effected, effected)
            } else {
              StringResource(R.string.product_deleteProductError)
            })
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

  private suspend fun _selectAllProducts(): List<ProductModel> = _productRepository.selectAll()

  private fun _loadAllProducts() {
    viewModelScope.launch(_dispatcher) {
      val products: List<ProductModel> = _selectAllProducts()
      val isTableEmpty: Boolean = _productRepository.isTableEmpty()
      withContext(Dispatchers.Main) {
        _setNoProductsAddedIllustrationVisible(isTableEmpty)
        filterView._onFiltersChanged(products = products)
      }
    }
  }
}
