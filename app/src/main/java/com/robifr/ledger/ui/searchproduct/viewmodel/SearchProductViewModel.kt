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

package com.robifr.ledger.ui.searchproduct.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.ModelSynchronizer
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
import com.robifr.ledger.ui.search.viewmodel.SearchState
import com.robifr.ledger.ui.searchproduct.SearchProductFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class SearchProductViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _productRepository: ProductRepository
) : ViewModel() {
  private val _productChangedListener: ModelSyncListener<ProductModel, ProductModel> =
      ModelSyncListener(
          onAdd = { ModelSynchronizer.addModel(_uiState.safeValue.products, it) },
          onUpdate = { ModelSynchronizer.updateModel(_uiState.safeValue.products, it) },
          onDelete = { ModelSynchronizer.deleteModel(_uiState.safeValue.products, it) },
          onUpsert = { ModelSynchronizer.upsertModel(_uiState.safeValue.products, it) },
          onSync = { _, updatedModels -> _onProductsChanged(updatedModels) })
  private var _searchJob: Job? = null
  private var _expandedProductJob: Job? = null

  private val _uiEvent: SafeMutableLiveData<SearchProductEvent> =
      SafeMutableLiveData(SearchProductEvent())
  val uiEvent: SafeLiveData<SearchProductEvent>
    get() = _uiEvent

  private val _uiState: SafeMutableLiveData<SearchProductState> =
      SafeMutableLiveData(
          SearchProductState(
              isSelectionEnabled =
                  savedStateHandle.get<Boolean>(
                      SearchProductFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key()) ?: false,
              isToolbarVisible =
                  savedStateHandle.get<Boolean>(
                      SearchProductFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key()) ?: true,
              initialQuery =
                  savedStateHandle.get<String>(
                      SearchProductFragment.Arguments.INITIAL_QUERY_STRING.key()) ?: "",
              query = "",
              initialSelectedProductIds =
                  savedStateHandle
                      .get<LongArray>(
                          SearchProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_IDS_LONG_ARRAY
                              .key())
                      ?.toList() ?: listOf(),
              products = listOf(),
              expandedProductIndex = -1,
              isProductMenuDialogShown = false,
              selectedProductMenu = null))
  val uiState: SafeLiveData<SearchProductState>
    get() = _uiState

  init {
    _productRepository.addModelChangedListener(_productChangedListener)
  }

  override fun onCleared() {
    _productRepository.removeModelChangedListener(_productChangedListener)
  }

  fun onSearch(query: String) {
    // Remove old job to ensure old query results don't appear in the future.
    _searchJob?.cancel()
    _searchJob =
        viewModelScope.launch(_dispatcher) {
          delay(300L)
          _productRepository.search(query).let {
            _uiState.postValue(_uiState.safeValue.copy(query = query, products = it))
            _onRecyclerAdapterRefreshed(RecyclerAdapterState.DataSetChanged)
          }
        }
  }

  fun onExpandedProductIndexChanged(index: Int) {
    _expandedProductJob?.cancel()
    _expandedProductJob =
        viewModelScope.launch {
          delay(200)
          // Update both previous and current expanded product. +1 offset because header holder.
          _onRecyclerAdapterRefreshed(
              RecyclerAdapterState.ItemChanged(
                  listOfNotNull(
                      _uiState.safeValue.expandedProductIndex
                          .takeIf { it != -1 && it != index }
                          ?.inc(),
                      index + 1)))
          _uiState.setValue(
              _uiState.safeValue.copy(
                  expandedProductIndex =
                      if (_uiState.safeValue.expandedProductIndex != index) index else -1))
        }
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

  fun onProductSelected(product: ProductModel?) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SearchProductResultState(product?.id),
          onSet = { this?.copy(searchResult = it) },
          onReset = { this?.copy(searchResult = null) })
    }
  }

  fun onDeleteProduct(productId: Long?) {
    viewModelScope.launch(_dispatcher) {
      _productRepository.delete(productId).also { effected ->
        _onSnackbarShown(
            if (effected > 0) {
              PluralResource(R.plurals.searchProduct_deleted_n_product, effected, effected)
            } else {
              StringResource(R.string.searchProduct_deleteProductError)
            })
      }
    }
  }

  fun onSearchUiStateChanged(state: SearchState) {
    _uiState.setValue(_uiState.safeValue.copy(query = state.query, products = state.products))
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.DataSetChanged)
  }

  private fun _onProductsChanged(products: List<ProductModel>) {
    _uiState.setValue(_uiState.safeValue.copy(products = products))
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.DataSetChanged)
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
