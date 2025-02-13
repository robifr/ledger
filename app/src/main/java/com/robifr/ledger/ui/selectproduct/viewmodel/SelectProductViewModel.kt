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
import com.robifr.ledger.data.ModelSynchronizer
import com.robifr.ledger.data.display.ProductSorter
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import com.robifr.ledger.ui.common.state.updateEvent
import com.robifr.ledger.ui.selectproduct.SelectProductFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SelectProductViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _productRepository: ProductRepository
) : ViewModel() {
  private val _sorter: ProductSorter = ProductSorter()
  private val _productChangedListener: ModelSyncListener<ProductModel, ProductModel> =
      ModelSyncListener(
          onAdd = { ModelSynchronizer.addModel(_uiState.safeValue.products, it) },
          onUpdate = { ModelSynchronizer.updateModel(_uiState.safeValue.products, it) },
          onDelete = { ModelSynchronizer.deleteModel(_uiState.safeValue.products, it) },
          onUpsert = { ModelSynchronizer.upsertModel(_uiState.safeValue.products, it) },
          onSync = { _, updatedModels -> _onProductsChanged(updatedModels) })

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
              products = listOf(),
              expandedProductIndex = -1,
              isSelectedProductPreviewExpanded = false))
  val uiState: SafeLiveData<SelectProductState>
    get() = _uiState

  init {
    _productRepository.addModelChangedListener(_productChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadAllProducts()
  }

  override fun onCleared() {
    _productRepository.removeModelChangedListener(_productChangedListener)
  }

  fun onSelectedProductPreviewExpanded(isExpanded: Boolean) {
    _uiState.setValue(_uiState.safeValue.copy(isSelectedProductPreviewExpanded = isExpanded))
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(0)) // Update header holder.
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

  fun onProductSelected(product: ProductModel?) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SelectProductResultState(product?.id),
          onSet = { this?.copy(selectResult = it) },
          onReset = { this?.copy(selectResult = null) })
    }
  }

  private fun _onProductsChanged(products: List<ProductModel>) {
    _uiState.setValue(_uiState.safeValue.copy(products = _sorter.sort(products)))
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.DataSetChanged)
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

  private suspend fun _selectAllProducts(): List<ProductModel> = _productRepository.selectAll()

  private fun _loadAllProducts() {
    viewModelScope.launch(_dispatcher) {
      val products: List<ProductModel> = _selectAllProducts()
      val selectedProductOnDb: ProductModel? =
          _productRepository.selectById(_uiState.safeValue.initialSelectedProduct?.id)
      withContext(Dispatchers.Main) {
        _onProductsChanged(products)
        _onSelectedProductOnDatabaseChanged(selectedProductOnDb)
      }
    }
  }
}
