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

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.databinding.ProductCardWideBinding;
import com.robifr.ledger.ui.RecyclerViewHolder;
import com.robifr.ledger.ui.product.ProductCardWideComponent;
import com.robifr.ledger.ui.product.ProductListAction;
import com.robifr.ledger.ui.selectproduct.SelectProductAction;
import java.util.Objects;

@Deprecated
public class SelectProductListHolder<T extends ProductListAction & SelectProductAction>
    extends RecyclerViewHolder<ProductModel, T> implements View.OnClickListener {
  @NonNull private final ProductCardWideBinding _cardBinding;
  @NonNull private final ProductCardWideComponent _card;
  @Nullable private ProductModel _boundProduct;

  public SelectProductListHolder(@NonNull ProductCardWideBinding binding, @NonNull T action) {
    super(binding.getRoot(), action);
    this._cardBinding = Objects.requireNonNull(binding);
    this._card = new ProductCardWideComponent(this.itemView.getContext(), this._cardBinding);

    this._cardBinding.cardView.setOnClickListener(this);
    // Don't set menu button to `View.GONE` as the position will be occupied by expand button.
    this._cardBinding.normalCard.menuButton.setVisibility(View.INVISIBLE);
    this._cardBinding.normalCard.expandButton.setVisibility(View.VISIBLE);
    this._cardBinding.normalCard.expandButton.setOnClickListener(this);
    this._cardBinding.expandedCard.menuButton.setVisibility(View.INVISIBLE);
    this._cardBinding.expandedCard.expandButton.setVisibility(View.VISIBLE);
    this._cardBinding.expandedCard.expandButton.setOnClickListener(this);
  }

  @Override
  public void bind(@NonNull ProductModel product) {
    this._boundProduct = Objects.requireNonNull(product);
    // Prevent reused view holder card from being expanded or checked.
    final boolean shouldCardExpanded =
        this._action.expandedProductIndex() != -1
            && this._boundProduct.equals(
                this._action.products().get(this._action.expandedProductIndex()));
    final boolean shouldChecked =
        this._action.initialSelectedProductIds().contains(this._boundProduct.id());

    this._card.reset();
    this._card.setNormalCardProduct(this._boundProduct);
    this._card.setCardChecked(shouldChecked);
    this.setCardExpanded(shouldCardExpanded);
  }

  @Override
  public void onClick(@NonNull View view) {
    Objects.requireNonNull(view);
    Objects.requireNonNull(this._boundProduct);

    switch (view.getId()) {
      case R.id.cardView -> this._action.onProductSelected(this._boundProduct);

      case R.id.expandButton -> {
        final boolean isExpanded =
            this._cardBinding.expandedCard.getRoot().getVisibility() == View.VISIBLE;
        final int expandedCustomerIndex =
            !isExpanded ? this._action.products().indexOf(this._boundProduct) : -1;

        this._action.onExpandedProductIndexChanged(expandedCustomerIndex);

        // Display ripple effect. The effect is gone due to the clicked view
        // set to `View.GONE` when the card expand/collapse.
        if (isExpanded) {
          this._cardBinding.normalCard.expandButton.setPressed(true);
          this._cardBinding.normalCard.expandButton.setPressed(false);
        } else {
          this._cardBinding.expandedCard.expandButton.setPressed(true);
          this._cardBinding.expandedCard.expandButton.setPressed(false);
        }
      }
    }
  }

  public void setCardExpanded(boolean isExpanded) {
    Objects.requireNonNull(this._boundProduct);

    this._card.setCardExpanded(isExpanded);
    // Only fill the view when it's shown on screen.
    if (isExpanded) this._card.setExpandedCardProduct(this._boundProduct);
  }
}
