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

package io.github.robifr.ledger.ui.editproduct.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.di.IoDispatcher
import io.github.robifr.ledger.repository.ProductRepository
import io.github.robifr.ledger.ui.common.PluralResource
import io.github.robifr.ledger.ui.common.StringResource
import io.github.robifr.ledger.ui.common.state.UiEvent
import io.github.robifr.ledger.ui.common.state.updateEvent
import io.github.robifr.ledger.ui.createproduct.viewmodel.CreateProductViewModel
import io.github.robifr.ledger.ui.editproduct.EditProductFragment
import io.github.robifr.ledger.util.CurrencyFormat
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditProductViewModel
@Inject
constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    productRepository: ProductRepository,
    private val _savedStateHandle: SavedStateHandle
) : CreateProductViewModel(dispatcher, productRepository) {
  private lateinit var _initialProductToEdit: ProductModel

  private val _editResultEvent: MutableLiveData<UiEvent<EditProductResultState>> = MutableLiveData()
  val editResultEvent: LiveData<UiEvent<EditProductResultState>>
    get() = _editResultEvent

  init {
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadProductToEdit()
  }

  override fun onSave() {
    if (_uiState.safeValue.name.isBlank()) {
      _uiState.setValue(
          _uiState.safeValue.copy(
              nameErrorMessageRes = StringResource(R.string.createProduct_name_emptyError)))
      return
    }
    viewModelScope.launch(_dispatcher) { _updateProduct(_parseInputtedProduct()) }
  }

  override fun onBackPressed() {
    if (_initialProductToEdit != _parseInputtedProduct()) _onUnsavedChangesDialogShown()
    else _onFragmentFinished()
  }

  override fun _parseInputtedProduct(): ProductModel =
      if (::_initialProductToEdit.isInitialized) {
        super._parseInputtedProduct().copy(id = _initialProductToEdit.id)
      } else {
        super._parseInputtedProduct()
      }

  private suspend fun _selectProductById(productId: Long?): ProductModel? =
      _productRepository.selectById(productId).also {
        if (it == null) _onSnackbarShown(StringResource(R.string.createProduct_fetchProductError))
      }

  private suspend fun _updateProduct(product: ProductModel) {
    _productRepository.update(product).let { effected ->
      _onSnackbarShown(
          if (effected > 0) {
            PluralResource(R.plurals.createProduct_updated_n_product, effected, effected)
          } else {
            StringResource(R.string.createProduct_updateProductError)
          })
      if (effected > 0) {
        _editResultEvent.updateEvent(
            data = EditProductResultState(product.id), onSet = { it }, onReset = { null })
      }
    }
  }

  private fun _loadProductToEdit() {
    viewModelScope.launch(_dispatcher) {
      // The initial product ID shouldn't be null when editing data.
      _selectProductById(
              _savedStateHandle.get<Long>(
                  EditProductFragment.Arguments.INITIAL_PRODUCT_ID_TO_EDIT_LONG.key())!!)
          ?.let {
            withContext(Dispatchers.Main) {
              _initialProductToEdit = it
              onNameTextChanged(it.name)
              onPriceTextChanged(
                  CurrencyFormat.formatCents(
                      it.price.toBigDecimal(),
                      AppCompatDelegate.getApplicationLocales().toLanguageTags()))
            }
          }
    }
  }
}
