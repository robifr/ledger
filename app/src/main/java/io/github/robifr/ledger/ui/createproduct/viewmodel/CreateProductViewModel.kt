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

package io.github.robifr.ledger.ui.createproduct.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.di.IoDispatcher
import io.github.robifr.ledger.repository.ProductRepository
import io.github.robifr.ledger.ui.common.PluralResource
import io.github.robifr.ledger.ui.common.StringResource
import io.github.robifr.ledger.ui.common.StringResourceType
import io.github.robifr.ledger.ui.common.state.SafeLiveData
import io.github.robifr.ledger.ui.common.state.SafeMutableLiveData
import io.github.robifr.ledger.ui.common.state.SnackbarState
import io.github.robifr.ledger.ui.common.state.updateEvent
import io.github.robifr.ledger.util.CurrencyFormat
import java.text.ParseException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@HiltViewModel
open class CreateProductViewModel
@Inject
constructor(
    @IoDispatcher protected val _dispatcher: CoroutineDispatcher,
    protected val _productRepository: ProductRepository
) : ViewModel() {
  private val _initialProductToCreate: ProductModel = ProductModel(name = "", price = 0L)

  protected val _uiEvent: SafeMutableLiveData<CreateProductEvent> =
      SafeMutableLiveData(CreateProductEvent())
  val uiEvent: SafeLiveData<CreateProductEvent>
    get() = _uiEvent

  protected val _uiState: SafeMutableLiveData<CreateProductState> =
      SafeMutableLiveData(
          CreateProductState(
              name = _initialProductToCreate.name, nameErrorMessageRes = null, formattedPrice = ""))
  val uiState: SafeLiveData<CreateProductState>
    get() = _uiState

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
    viewModelScope.launch(_dispatcher) { _addProduct(_parseInputtedProduct()) }
  }

  open fun onBackPressed() {
    if (_initialProductToCreate != _parseInputtedProduct()) _onUnsavedChangesDialogShown()
    else _onFragmentFinished()
  }

  protected open fun _parseInputtedProduct(): ProductModel {
    val price: Long =
        try {
          CurrencyFormat.parseToCents(
                  _uiState.safeValue.formattedPrice,
                  AppCompatDelegate.getApplicationLocales().toLanguageTags())
              .toLong()
        } catch (_: ParseException) {
          0L
        }
    return ProductModel(name = _uiState.safeValue.name, price = price)
  }

  protected fun _onSnackbarShown(messageRes: StringResourceType) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SnackbarState(messageRes),
          onSet = { this?.copy(snackbar = it) },
          onReset = { this?.copy(snackbar = null) })
    }
  }

  protected fun _onFragmentFinished() {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = true,
          onSet = { this?.copy(isFragmentFinished = it) },
          onReset = { this?.copy(isFragmentFinished = null) })
    }
  }

  protected fun _onUnsavedChangesDialogShown() {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = true,
          onSet = { this?.copy(isUnsavedChangesDialogShown = it) },
          onReset = { this?.copy(isUnsavedChangesDialogShown = null) })
    }
  }

  private suspend fun _addProduct(product: ProductModel) {
    _productRepository.add(product).let { id ->
      _onSnackbarShown(
          if (id != 0L) PluralResource(R.plurals.createProduct_added_n_product, 1, 1)
          else StringResource(R.string.createProduct_addProductError))
      if (id != 0L) {
        _uiEvent.updateEvent(
            data = CreateProductResultState(id),
            onSet = { this?.copy(createResult = it) },
            onReset = { this?.copy(createResult = null) })
      }
    }
  }
}
