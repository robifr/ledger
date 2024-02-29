/**
 * Copyright (c) 2022-present Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package com.robifr.ledger.ui.search_product.view_model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.repository.ProductRepository;
import com.robifr.ledger.ui.LiveDataEvent;
import java.util.List;
import java.util.Objects;

public class SearchProductViewModel extends ViewModel {
  @NonNull private final ProductRepository _productRepository;
  @NonNull private final Handler _handler = new Handler(Looper.getMainLooper());

  @NonNull
  private final MutableLiveData<LiveDataEvent<Long>> _selectedProductId = new MutableLiveData<>();

  @NonNull private final MutableLiveData<List<ProductModel>> _products = new MutableLiveData<>();

  public SearchProductViewModel(@NonNull ProductRepository productRepository) {
    this._productRepository = Objects.requireNonNull(productRepository);
  }

  @NonNull
  public LiveData<LiveDataEvent<Long>> selectedProductId() {
    return this._selectedProductId;
  }

  @NonNull
  public LiveData<List<ProductModel>> products() {
    return this._products;
  }

  public void onSearch(@NonNull String query) {
    Objects.requireNonNull(query);

    // Remove old runnable to ensure old query result wouldn't appear in future.
    this._handler.removeCallbacksAndMessages(null);
    this._handler.postDelayed(
        () -> {
          // Send null when user hasn't type anything to prevent
          // no-results-found illustration shows up.
          if (query.isEmpty()) this._products.postValue(null);
          else this._productRepository.search(query).thenAcceptAsync(this._products::postValue);
        },
        300);
  }

  public void onProductSelected(@Nullable ProductModel product) {
    final Long productId = product != null && product.id() != null ? product.id() : null;
    this._selectedProductId.setValue(new LiveDataEvent<>(productId));
  }

  public static class Factory implements ViewModelProvider.Factory {
    @NonNull private final Context _context;

    public Factory(@NonNull Context context) {
      Objects.requireNonNull(context);

      this._context = context.getApplicationContext();
    }

    @Override
    @NonNull
    public <T extends ViewModel> T create(@NonNull Class<T> cls) {
      Objects.requireNonNull(cls);

      final SearchProductViewModel viewModel =
          new SearchProductViewModel(ProductRepository.instance(this._context));
      return Objects.requireNonNull(cls.cast(viewModel));
    }
  }
}
