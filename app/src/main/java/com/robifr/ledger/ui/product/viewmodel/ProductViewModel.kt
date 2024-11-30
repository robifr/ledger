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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.display.ProductSortMethod
import com.robifr.ledger.data.display.ProductSorter
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
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
import kotlinx.coroutines.future.await
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
          onSyncModels = { filterView._onFiltersChanged(products = it) })

  private val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  private val _uiState: SafeMutableLiveData<ProductState> =
      SafeMutableLiveData(
          ProductState(
              products = listOf(), expandedProductIndex = -1, sortMethod = _sorter.sortMethod))
  val uiState: SafeLiveData<ProductState>
    get() = _uiState

  private val _recyclerAdapterState: SingleLiveEvent<RecyclerAdapterState> = SingleLiveEvent()
  val recyclerAdapterState: LiveData<RecyclerAdapterState>
    get() = _recyclerAdapterState

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
    _recyclerAdapterState.setValue(RecyclerAdapterState.DataSetChanged)
  }

  fun onExpandedProductIndexChanged(index: Int) {
    // Update both previous and current expanded product. +1 offset because header holder.
    _recyclerAdapterState.setValue(
        RecyclerAdapterState.ItemChanged(
            listOfNotNull(
                _uiState.safeValue.expandedProductIndex.takeIf { it != -1 }?.let { it + 1 },
                index + 1)))
    _uiState.setValue(
        _uiState.safeValue.copy(
            expandedProductIndex =
                if (_uiState.safeValue.expandedProductIndex != index) index else -1))
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

  fun onDeleteProduct(product: ProductModel) {
    viewModelScope.launch(_dispatcher) {
      _productRepository.delete(product).await()?.let { effected ->
        _snackbarState.postValue(
            SnackbarState(
                if (effected > 0) {
                  PluralResource(R.plurals.product_deleted_n_product, effected, effected)
                } else {
                  StringResource(R.string.product_deleteProductError)
                }))
      }
    }
  }

  private suspend fun _selectAllProducts(): List<ProductModel> =
      _productRepository.selectAll().await()

  private fun _loadAllProducts() {
    viewModelScope.launch(_dispatcher) {
      _selectAllProducts().let {
        withContext(Dispatchers.Main) { filterView._onFiltersChanged(products = it) }
      }
    }
  }
}
