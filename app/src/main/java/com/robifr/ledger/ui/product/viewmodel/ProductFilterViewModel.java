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

package com.robifr.ledger.ui.product.viewmodel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.robifr.ledger.data.display.ProductFilterer;
import com.robifr.ledger.data.display.ProductFilters;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.util.CurrencyFormat;
import com.robifr.ledger.util.livedata.SafeLiveData;
import com.robifr.ledger.util.livedata.SafeMutableLiveData;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;

public class ProductFilterViewModel {
  @NonNull private final ProductViewModel _viewModel;
  @NonNull private final ProductFilterer _filterer;

  @NonNull
  private final SafeMutableLiveData<String> _inputtedMinPriceText = new SafeMutableLiveData<>("");

  @NonNull
  private final SafeMutableLiveData<String> _inputtedMaxPriceText = new SafeMutableLiveData<>("");

  public ProductFilterViewModel(
      @NonNull ProductViewModel viewModel, @NonNull ProductFilterer filterer) {
    this._viewModel = Objects.requireNonNull(viewModel);
    this._filterer = Objects.requireNonNull(filterer);
  }

  @NonNull
  public SafeLiveData<String> inputtedMinPriceText() {
    return this._inputtedMinPriceText;
  }

  @NonNull
  public SafeLiveData<String> inputtedMaxPriceText() {
    return this._inputtedMaxPriceText;
  }

  /**
   * Get current inputted filter from any corresponding inputted live data. If any live data is set
   * using {@link MutableLiveData#postValue(Object)}, calling this method may not immediately
   * reflect the latest changes. For accurate results in asynchronous operations, consider calling
   * this method inside {@link Observer}.
   */
  @NonNull
  public ProductFilters inputtedFilters() {
    // Nullable value to represent unbounded range.
    Long minPrice = null;
    Long maxPrice = null;

    try {
      if (!this._inputtedMinPriceText.getValue().isBlank()) {
        minPrice =
            CurrencyFormat.parse(
                    this._inputtedMinPriceText.getValue(),
                    AppCompatDelegate.getApplicationLocales().toLanguageTags())
                .longValue();
      }

    } catch (ParseException ignore) {
    }

    try {
      if (!this._inputtedMaxPriceText.getValue().isBlank()) {
        maxPrice =
            CurrencyFormat.parse(
                    this._inputtedMaxPriceText.getValue(),
                    AppCompatDelegate.getApplicationLocales().toLanguageTags())
                .longValue();
      }

    } catch (ParseException ignore) {
    }

    return ProductFilters.toBuilder().setFilteredPrice(new Pair<>(minPrice, maxPrice)).build();
  }

  public void onMinPriceTextChanged(@NonNull String minPrice) {
    Objects.requireNonNull(minPrice);

    this._inputtedMinPriceText.setValue(minPrice);
  }

  public void onMaxPriceTextChanged(@NonNull String maxPrice) {
    Objects.requireNonNull(maxPrice);

    this._inputtedMaxPriceText.setValue(maxPrice);
  }

  public void onFiltersChanged(@NonNull ProductFilters filters) {
    this.onFiltersChanged(filters, this._viewModel.products().getValue());
  }

  public void onFiltersChanged(
      @NonNull ProductFilters filters, @NonNull List<ProductModel> products) {
    Objects.requireNonNull(filters);
    Objects.requireNonNull(products);

    final String minTotalPrice =
        filters.filteredPrice().first != null
            ? CurrencyFormat.format(
                BigDecimal.valueOf(filters.filteredPrice().first),
                AppCompatDelegate.getApplicationLocales().toLanguageTags())
            : "";
    final String maxTotalPrice =
        filters.filteredPrice().second != null
            ? CurrencyFormat.format(
                BigDecimal.valueOf(filters.filteredPrice().second),
                AppCompatDelegate.getApplicationLocales().toLanguageTags())
            : "";

    this.onMinPriceTextChanged(minTotalPrice);
    this.onMaxPriceTextChanged(maxTotalPrice);
    this._filterer.setFilters(filters);

    final List<ProductModel> filteredProducts = this._filterer.filter(products);
    // Re-sort the list, after previously re-populating the list with a new filtered value.
    this._viewModel.onSortMethodChanged(this._viewModel.sortMethod().getValue(), filteredProducts);
  }
}
