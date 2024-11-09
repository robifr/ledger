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

package com.robifr.ledger.ui.product.recycler;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.databinding.ProductCardWideBinding;
import com.robifr.ledger.ui.RecyclerViewHolder;
import com.robifr.ledger.ui.product.ProductAction;
import com.robifr.ledger.ui.product.ProductCardWideComponent;
import com.robifr.ledger.ui.product.ProductListAction;
import java.util.Objects;

@Deprecated
public class ProductListHolder<T extends ProductListAction & ProductAction>
    extends RecyclerViewHolder<ProductModel, T> implements View.OnClickListener {
  @NonNull protected final ProductCardWideBinding _cardBinding;
  @NonNull protected final ProductCardWideComponent _card;
  @NonNull protected final ProductListMenu _menu;
  @Nullable protected ProductModel _boundProduct;

  public ProductListHolder(@NonNull ProductCardWideBinding binding, @NonNull T action) {
    super(binding.getRoot(), action);
    this._cardBinding = Objects.requireNonNull(binding);
    this._card = new ProductCardWideComponent(this.itemView.getContext(), this._cardBinding);
    this._menu = new ProductListMenu(this);

    this._cardBinding.cardView.setOnClickListener(this);
    this._cardBinding.normalCard.menuButton.setOnClickListener(v -> this._menu.openDialog());
    this._cardBinding.expandedCard.menuButton.setOnClickListener(v -> this._menu.openDialog());
  }

  @Override
  public void bind(@NonNull ProductModel product) {
    this._boundProduct = Objects.requireNonNull(product);
    // Prevent reused view holder card from being expanded.
    final boolean shouldCardExpanded =
        this._action.expandedProductIndex() != -1
            && this._boundProduct.equals(
                this._action.products().get(this._action.expandedProductIndex()));

    this._card.reset();
    this._card.setNormalCardProduct(this._boundProduct);
    this.setCardExpanded(shouldCardExpanded);
  }

  @Override
  public void onClick(@NonNull View view) {
    Objects.requireNonNull(view);

    switch (view.getId()) {
      case R.id.cardView -> {
        final int expandedQueueIndex =
            this._cardBinding.expandedCard.getRoot().getVisibility() != View.VISIBLE
                ? this._action.products().indexOf(this._boundProduct)
                : -1;
        this._action.onExpandedProductIndexChanged(expandedQueueIndex);
      }
    }
  }

  @NonNull
  public ProductModel boundProduct() {
    return Objects.requireNonNull(this._boundProduct);
  }

  public void setCardExpanded(boolean isExpanded) {
    Objects.requireNonNull(this._boundProduct);

    this._card.setCardExpanded(isExpanded);
    // Only fill the view when it's shown on screen.
    if (isExpanded) this._card.setExpandedCardProduct(this._boundProduct);
  }
}
