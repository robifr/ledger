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

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.data.display.ProductFilterer
import com.robifr.ledger.data.display.ProductFilters
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.util.CurrencyFormat
import java.text.ParseException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductFilterViewModel(
    private val _viewModel: ProductViewModel,
    private val _dispatcher: CoroutineDispatcher,
    private val _selectAllProducts: suspend () -> List<ProductModel>
) {
  private val _filterer: ProductFilterer = ProductFilterer()

  private val _uiState: SafeMutableLiveData<ProductFilterState> =
      SafeMutableLiveData(
          ProductFilterState(isDialogShown = false, formattedMinPrice = "", formattedMaxPrice = ""))
  val uiState: SafeLiveData<ProductFilterState>
    get() = _uiState

  fun onMinPriceTextChanged(formattedMinPrice: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedMinPrice = formattedMinPrice))
  }

  fun onMaxPriceTextChanged(formattedMaxPrice: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedMaxPrice = formattedMaxPrice))
  }

  fun onDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isDialogShown = true))
  }

  fun onDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isDialogShown = false))
    _viewModel.viewModelScope.launch(_dispatcher) {
      _selectAllProducts().let {
        withContext(Dispatchers.Main) { _onFiltersChanged(_parseInputtedFilters(), it) }
      }
    }
  }

  fun _onFiltersChanged(
      filters: ProductFilters = _parseInputtedFilters(),
      products: List<ProductModel> = _viewModel.uiState.safeValue.products
  ) {
    _filterer.filters = filters
    onMinPriceTextChanged(
        filters.filteredPrice.first?.let {
          CurrencyFormat.formatCents(
              it.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
    onMaxPriceTextChanged(
        filters.filteredPrice.second?.let {
          CurrencyFormat.formatCents(
              it.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: "")
    _viewModel.onProductsChanged(_filterer.filter(products))
  }

  private fun _parseInputtedFilters(): ProductFilters {
    // All these nullable value to represent unbounded range.
    var minPrice: Long? = null
    try {
      if (_uiState.safeValue.formattedMinPrice.isNotBlank()) {
        minPrice =
            CurrencyFormat.parseToCents(
                    _uiState.safeValue.formattedMinPrice,
                    AppCompatDelegate.getApplicationLocales().toLanguageTags())
                .toLong()
      }
    } catch (_: ParseException) {}

    var maxPrice: Long? = null
    try {
      if (_uiState.safeValue.formattedMaxPrice.isNotBlank()) {
        maxPrice =
            CurrencyFormat.parseToCents(
                    _uiState.safeValue.formattedMaxPrice,
                    AppCompatDelegate.getApplicationLocales().toLanguageTags())
                .toLong()
      }
    } catch (_: ParseException) {}

    return ProductFilters(minPrice to maxPrice)
  }
}
