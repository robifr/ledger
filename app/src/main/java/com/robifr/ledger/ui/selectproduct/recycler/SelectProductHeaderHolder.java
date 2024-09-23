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

package com.robifr.ledger.ui.selectproduct.recycler;

import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.databinding.ListableListSelectedItemBinding;
import com.robifr.ledger.databinding.ProductCardWideBinding;
import com.robifr.ledger.ui.RecyclerViewHolder;
import com.robifr.ledger.ui.product.ProductCardNormalComponent;
import com.robifr.ledger.ui.product.ProductListAction;
import java.util.Objects;
import java.util.Optional;

public class SelectProductHeaderHolder<T extends ProductListAction>
    extends RecyclerViewHolder<Optional<ProductModel>, T> implements View.OnClickListener {
  @NonNull private final ListableListSelectedItemBinding _headerBinding;
  @NonNull private final ProductCardWideBinding _selectedCardBinding;
  @NonNull private final ProductCardNormalComponent _selectedNormalCard;

  public SelectProductHeaderHolder(
      @NonNull ListableListSelectedItemBinding binding, @NonNull T action) {
    super(binding.getRoot(), action);
    this._headerBinding = Objects.requireNonNull(binding);
    this._selectedCardBinding =
        ProductCardWideBinding.inflate(
            LayoutInflater.from(this.itemView.getContext()),
            this._headerBinding.selectedItemContainer,
            false);
    this._selectedNormalCard =
        new ProductCardNormalComponent(
            this.itemView.getContext(), this._selectedCardBinding.normalCard);

    this._headerBinding.selectedItemTitle.setText(
        this.itemView.getContext().getString(R.string.text_selected_product));
    this._headerBinding.selectedItemContainer.addView(this._selectedCardBinding.getRoot());
    this._headerBinding.allListTitle.setText(
        this.itemView.getContext().getString(R.string.text_all_products));
    this._headerBinding.newButton.setOnClickListener(this);
    // Don't set menu button to `View.GONE` as the position will be occupied by expand button.
    this._selectedCardBinding.normalCard.menuButton.setVisibility(View.INVISIBLE);
  }

  @Override
  public void bind(@NonNull Optional<ProductModel> selectedProduct) {
    if (!selectedProduct.isPresent()) {
      this._selectedCardBinding.cardView.setChecked(false);
      this._headerBinding.selectedItemDescription.setVisibility(View.GONE);
      this._headerBinding.selectedItemTitle.setVisibility(View.GONE);
      this._headerBinding.selectedItemContainer.setVisibility(View.GONE);
      return;
    }

    final ProductModel selectedProductOnDb =
        this._action.products().stream()
            .filter(
                product -> product.id() != null && product.id().equals(selectedProduct.get().id()))
            .findFirst()
            .orElse(null);

    // The original product on database was deleted.
    if (selectedProductOnDb == null) {
      this._headerBinding.selectedItemDescription.setText(
          this.itemView
              .getContext()
              .getString(R.string.text_originally_selected_product_was_deleted));
      this._headerBinding.selectedItemDescription.setVisibility(View.VISIBLE);

      // The original product on database was edited.
    } else if (!selectedProduct.get().equals(selectedProductOnDb)) {
      this._headerBinding.selectedItemDescription.setText(
          this.itemView
              .getContext()
              .getString(R.string.text_originally_selected_product_was_changed));
      this._headerBinding.selectedItemDescription.setVisibility(View.VISIBLE);

      // It's the same unchanged product.
    } else {
      this._headerBinding.selectedItemDescription.setVisibility(View.GONE);
    }

    this._selectedCardBinding.cardView.setChecked(true);
    this._selectedNormalCard.setProduct(selectedProduct.orElse(null));
    this._headerBinding.selectedItemTitle.setVisibility(View.VISIBLE);
    this._headerBinding.selectedItemContainer.setVisibility(View.VISIBLE);
  }

  @Override
  public void onClick(@NonNull View view) {
    Objects.requireNonNull(view);

    switch (view.getId()) {
      case R.id.newButton ->
          Navigation.findNavController(this.itemView).navigate(R.id.createProductFragment);
    }
  }
}
