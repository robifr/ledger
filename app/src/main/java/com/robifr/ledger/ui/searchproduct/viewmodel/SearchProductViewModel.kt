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

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
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
  private val _productChangedListener: ModelSyncListener<ProductModel> =
      ModelSyncListener(
          currentModel = { _uiState.safeValue.products }, onSyncModels = ::_onProductsChanged)
  private var _searchJob: Job? = null

  private val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  private val _uiState: SafeMutableLiveData<SearchProductState> =
      SafeMutableLiveData(
          SearchProductState(
              isSelectionEnabled =
                  savedStateHandle.get<Boolean>(
                      SearchProductFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key) ?: false,
              initialQuery =
                  savedStateHandle.get<String>(
                      SearchProductFragment.Arguments.INITIAL_QUERY_STRING.key) ?: "",
              query = "",
              initialSelectedProductIds =
                  savedStateHandle
                      .get<LongArray>(
                          SearchProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_IDS_LONG_ARRAY
                              .key)
                      ?.toList() ?: listOf(),
              products = listOf(),
              expandedProductIndex = -1))
  val uiState: SafeLiveData<SearchProductState>
    get() = _uiState

  private val _recyclerAdapterState: SingleLiveEvent<RecyclerAdapterState> = SingleLiveEvent()
  val recyclerAdapterState: LiveData<RecyclerAdapterState>
    get() = _recyclerAdapterState

  private val _resultState: SingleLiveEvent<SearchProductResultState> = SingleLiveEvent()
  val resultState: LiveData<SearchProductResultState>
    get() = _resultState

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
            _recyclerAdapterState.postValue(RecyclerAdapterState.DataSetChanged)
          }
        }
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

  fun onProductSelected(product: ProductModel?) {
    _resultState.setValue(SearchProductResultState(product?.id))
  }

  fun onDeleteProduct(product: ProductModel) {
    viewModelScope.launch(_dispatcher) {
      _productRepository.delete(product).also { effected ->
        _snackbarState.postValue(
            SnackbarState(
                if (effected > 0) {
                  PluralResource(R.plurals.searchProduct_deleted_n_product, effected, effected)
                } else {
                  StringResource(R.string.searchProduct_deleteProductError)
                }))
      }
    }
  }

  private fun _onProductsChanged(products: List<ProductModel>) {
    _uiState.setValue(_uiState.safeValue.copy(products = products))
    _recyclerAdapterState.setValue(RecyclerAdapterState.DataSetChanged)
  }
}
