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

package com.robifr.ledger.ui.editproduct.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.R
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.PluralResource
import com.robifr.ledger.ui.SafeEvent
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductViewModel
import com.robifr.ledger.ui.editproduct.EditProductFragment
import com.robifr.ledger.util.CurrencyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditProductViewModel
@Inject
constructor(productRepository: ProductRepository, savedStateHandle: SavedStateHandle) :
    CreateProductViewModel(productRepository) {
  private lateinit var _initialProductToEdit: ProductModel

  private val _editResultState: MutableLiveData<SafeEvent<EditProductResultState>> =
      MutableLiveData()
  val editResultState: LiveData<SafeEvent<EditProductResultState>>
    get() = _editResultState

  init {
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    // The initial product ID also shouldn't be null when editing data.
    SafeEvent.observeOnce(
        _selectProductById(
            savedStateHandle.get<Long>(
                EditProductFragment.Arguments.INITIAL_PRODUCT_ID_TO_EDIT_LONG.key)!!),
    ) { product ->
      product?.let {
        _initialProductToEdit = it
        onNameTextChanged(it.name)
        onPriceTextChanged(
            CurrencyFormat.format(
                it.price.toBigDecimal(),
                AppCompatDelegate.getApplicationLocales().toLanguageTags()))
      }
    }
  }

  override fun onSave() {
    if (_uiState.safeValue.name.isBlank()) {
      _uiState.setValue(
          _uiState.safeValue.copy(
              nameErrorMessageRes = StringResource(R.string.createProduct_name_emptyError)))
      return
    }
    _updateProduct(_inputtedProduct())
  }

  override fun _inputtedProduct(): ProductModel =
      super._inputtedProduct().copy(id = _initialProductToEdit.id)

  private fun _selectProductById(productId: Long?): LiveData<ProductModel?> {
    val result: MutableLiveData<ProductModel?> = MutableLiveData()
    _productRepository.selectById(productId).thenAcceptAsync { product: ProductModel? ->
      if (product == null) {
        _snackbarState.postValue(
            SafeEvent(SnackbarState(StringResource(R.string.createProduct_fetchProductError))))
      }
      result.postValue(product)
    }
    return result
  }

  private fun _updateProduct(product: ProductModel) {
    _productRepository.update(product).thenAcceptAsync { effected: Int? ->
      val updated: Int = effected ?: 0
      if (updated > 0) _editResultState.postValue(SafeEvent(EditProductResultState(product.id)))
      _snackbarState.postValue(
          SafeEvent(
              SnackbarState(
                  if (updated > 0) {
                    PluralResource(R.plurals.createProduct_updated_n_product, updated, updated)
                  } else {
                    StringResource(R.string.createProduct_updateProductError)
                  })))
    }
  }
}
