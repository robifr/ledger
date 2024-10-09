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

package com.robifr.ledger.ui.createproduct.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.PluralResource
import com.robifr.ledger.ui.SafeEvent
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.StringResource
import com.robifr.ledger.util.CurrencyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.ParseException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

@HiltViewModel
open class CreateProductViewModel
@Inject
constructor(
    protected val _productRepository: ProductRepository,
    @IoDispatcher protected val _dispatcher: CoroutineDispatcher
) : ViewModel() {
  protected val _snackbarState: MutableLiveData<SafeEvent<SnackbarState>> = MutableLiveData()
  val snackbarState: MutableLiveData<SafeEvent<SnackbarState>>
    get() = _snackbarState

  protected val _uiState: SafeMutableLiveData<CreateProductState> =
      SafeMutableLiveData(
          CreateProductState(name = "", nameErrorMessageRes = null, formattedPrice = ""))
  val uiState: SafeLiveData<CreateProductState>
    get() = _uiState

  private val _resultState: MutableLiveData<SafeEvent<CreateProductResultState>> = MutableLiveData()
  val resultState: LiveData<SafeEvent<CreateProductResultState>>
    get() = _resultState

  protected open val _inputtedProduct: ProductModel
    get() {
      var price: Long = 0L
      try {
        price =
            CurrencyFormat.parse(
                    _uiState.safeValue.formattedPrice,
                    AppCompatDelegate.getApplicationLocales().toLanguageTags())
                .toLong()
      } catch (ignore: ParseException) {}
      return ProductModel(name = _uiState.safeValue.name, price = price)
    }

  fun onNameTextChanged(name: String) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            name = name,
            // Disable error when name field filled.
            nameErrorMessageRes =
                if (name.isBlank()) _uiState.safeValue.nameErrorMessageRes else null))
  }

  fun onPriceTextChanged(formattedPrice: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedPrice = formattedPrice))
  }

  open fun onSave() {
    if (_uiState.safeValue.name.isBlank()) {
      _uiState.setValue(
          _uiState.safeValue.copy(
              nameErrorMessageRes = StringResource(R.string.createProduct_name_emptyError)))
      return
    }
    viewModelScope.launch(_dispatcher) { _addProduct(_inputtedProduct) }
  }

  private suspend fun _addProduct(product: ProductModel) =
      _productRepository.add(product).await().also { id: Long? ->
        if (id != 0L) _resultState.postValue(SafeEvent(CreateProductResultState(id)))
        _snackbarState.postValue(
            SafeEvent(
                SnackbarState(
                    if (id != 0L) PluralResource(R.plurals.createProduct_added_n_product, 1, 1)
                    else StringResource(R.string.createProduct_addProductError))))
      }
}
