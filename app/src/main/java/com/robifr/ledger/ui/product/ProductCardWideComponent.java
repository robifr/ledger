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

package com.robifr.ledger.ui.product;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.databinding.ProductCardWideBinding;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.util.Objects;

public class ProductCardWideComponent {
  @NonNull private final Context _context;
  @NonNull private final ProductCardWideBinding _binding;

  public ProductCardWideComponent(
      @NonNull Context context, @NonNull ProductCardWideBinding binding) {
    this._context = Objects.requireNonNull(context);
    this._binding = Objects.requireNonNull(binding);

    final ShapeAppearanceModel imageShape =
        ShapeAppearanceModel.builder(
                this._context,
                com.google.android.material.R.style.Widget_MaterialComponents_ShapeableImageView,
                R.style.Shape_Card)
            .build();
    this._binding.normalCard.image.shapeableImage.setShapeAppearanceModel(imageShape);
    this._binding.expandedCard.image.shapeableImage.setShapeAppearanceModel(imageShape);
  }

  public void setNormalCardProduct(@NonNull ProductModel product) {
    Objects.requireNonNull(product);

    this._setId(product.id(), true);
    this._setName(product.name(), true);
    this._setPrice(product.price(), true);
  }

  public void setExpandedCardProduct(@NonNull ProductModel product) {
    Objects.requireNonNull(product);

    this._setId(product.id(), false);
    this._setName(product.name(), false);
    this._setPrice(product.price(), false);
  }

  public void setCardExpanded(boolean isExpanded) {
    final int normalCardVisibility = isExpanded ? View.GONE : View.VISIBLE;
    final int expandedCardVisibility = isExpanded ? View.VISIBLE : View.GONE;

    this._binding.normalCard.getRoot().setVisibility(normalCardVisibility);
    this._binding.expandedCard.getRoot().setVisibility(expandedCardVisibility);
  }

  public void setCardChecked(boolean isChecked) {
    final int textVisibility = isChecked ? View.GONE : View.VISIBLE;
    final int iconVisibility = isChecked ? View.VISIBLE : View.GONE;

    this._binding.cardView.setChecked(isChecked);
    this._binding.normalCard.image.text.setVisibility(textVisibility);
    this._binding.normalCard.image.icon.setVisibility(iconVisibility);
    this._binding.expandedCard.image.text.setVisibility(textVisibility);
    this._binding.expandedCard.image.icon.setVisibility(iconVisibility);
  }

  public void reset() {
    this._binding.normalCard.uniqueId.setText(null);
    this._binding.normalCard.uniqueId.setEnabled(false);
    this._binding.expandedCard.uniqueId.setText(null);
    this._binding.expandedCard.uniqueId.setEnabled(false);

    this._binding.cardView.setChecked(false);
    this._binding.normalCard.name.setText(null);
    this._binding.normalCard.image.text.setText(null);
    this._binding.normalCard.image.icon.setVisibility(View.GONE);
    this._binding.expandedCard.name.setText(null);
    this._binding.expandedCard.image.text.setText(null);
    this._binding.expandedCard.image.icon.setVisibility(View.GONE);

    this._binding.normalCard.price.setText(null);
    this._binding.expandedCard.price.setText(null);
  }

  private void _setId(@Nullable Long id, boolean isNormalCard) {
    final boolean isIdExists = id != null;
    final String productId =
        isIdExists ? id.toString() : this._context.getString(R.string.symbol_notavailable);

    if (isNormalCard) {
      this._binding.normalCard.uniqueId.setText(productId);
      this._binding.normalCard.uniqueId.setEnabled(isIdExists);
    } else {
      this._binding.expandedCard.uniqueId.setText(productId);
      this._binding.expandedCard.uniqueId.setEnabled(isIdExists);
    }
  }

  private void _setName(@NonNull String name, boolean isNormalCard) {
    Objects.requireNonNull(name);

    final String initialLetterName = name.trim().substring(0, Math.min(1, name.trim().length()));

    if (isNormalCard) {
      this._binding.normalCard.name.setText(name);
      this._binding.normalCard.image.text.setText(initialLetterName);
    } else {
      this._binding.expandedCard.name.setText(name);
      this._binding.expandedCard.image.text.setText(initialLetterName);
    }
  }

  private void _setPrice(long price, boolean isNormalCard) {
    final String formattedPrice =
        CurrencyFormat.format(
            BigDecimal.valueOf(price), AppCompatDelegate.getApplicationLocales().toLanguageTags());

    if (isNormalCard) this._binding.normalCard.price.setText(formattedPrice);
    else this._binding.expandedCard.price.setText(formattedPrice);
  }
}
