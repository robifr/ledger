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

package com.robifr.ledger.ui.editproduct.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.repository.ProductRepository;
import com.robifr.ledger.ui.StringResources;
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductViewModel;
import com.robifr.ledger.ui.editproduct.EditProductFragment;
import com.robifr.ledger.util.CurrencyFormat;
import com.robifr.ledger.util.livedata.SafeEvent;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

@HiltViewModel
public class EditProductViewModel extends CreateProductViewModel {
  @Nullable private ProductModel _initialProductToEdit = null;

  @NonNull
  private final MutableLiveData<SafeEvent<Optional<Long>>> _resultEditedProductId =
      new MutableLiveData<>();

  @Inject
  public EditProductViewModel(
      @NonNull ProductRepository productRepository, @NonNull SavedStateHandle savedStateHandle) {
    super(productRepository);
    Objects.requireNonNull(savedStateHandle);

    // Setting up initial values inside a fragment is painful. See commit d5604599.
    SafeEvent.observeOnce(
        // Shouldn't be null when editing data.
        this.selectProductById(
            Objects.requireNonNull(
                savedStateHandle.get(
                    EditProductFragment.Arguments.INITIAL_PRODUCT_ID_TO_EDIT_LONG.key()))),
        product -> {
          this._initialProductToEdit = product;
          this.onNameTextChanged(product.name());
          this.onPriceTextChanged(
              CurrencyFormat.format(
                  BigDecimal.valueOf(product.price()),
                  AppCompatDelegate.getApplicationLocales().toLanguageTags()));
        },
        Objects::nonNull);
  }

  @Override
  @NonNull
  public ProductModel inputtedProduct() {
    final Long id =
        this._initialProductToEdit != null && this._initialProductToEdit.id() != null
            ? this._initialProductToEdit.id()
            : null;
    return ProductModel.toBuilder(super.inputtedProduct()).setId(id).build();
  }

  @Override
  public void onSave() {
    if (this._inputtedNameText.getValue().isBlank()) {
      this._inputtedNameError.setValue(
          Optional.of(new StringResources.Strings(R.string.createProduct_name_emptyError)));
      return;
    }

    this._updateProduct(this.inputtedProduct());
  }

  @NonNull
  public LiveData<SafeEvent<Optional<Long>>> resultEditedProductId() {
    return this._resultEditedProductId;
  }

  @NonNull
  public LiveData<ProductModel> selectProductById(@Nullable Long productId) {
    final MutableLiveData<ProductModel> result = new MutableLiveData<>();

    this._productRepository
        .selectById(productId)
        .thenAcceptAsync(
            product -> {
              if (product == null) {
                this._snackbarMessage.postValue(
                    new SafeEvent<>(
                        new StringResources.Strings(R.string.createProduct_fetchProductError)));
              }

              result.postValue(product);
            });
    return result;
  }

  private void _updateProduct(@NonNull ProductModel product) {
    Objects.requireNonNull(product);

    this._productRepository
        .update(product)
        .thenAcceptAsync(
            effected -> {
              if (effected > 0) {
                this._resultEditedProductId.postValue(
                    new SafeEvent<>(Optional.ofNullable(product.id())));
              }

              final StringResources stringRes =
                  effected > 0
                      ? new StringResources.Plurals(
                          R.plurals.createProduct_updated_n_product, effected, effected)
                      : new StringResources.Strings(R.string.createProduct_updateProductError);
              this._snackbarMessage.postValue(new SafeEvent<>(stringRes));
            });
  }
}
