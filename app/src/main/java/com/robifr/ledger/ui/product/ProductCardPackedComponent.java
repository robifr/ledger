/**
 * Copyright (c) 2024 Robi
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

package com.robifr.ledger.ui.product;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.databinding.ProductCardPackedBinding;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.util.Objects;

public class ProductCardPackedComponent {
  @NonNull private final Context _context;
  @NonNull private final ProductCardPackedBinding _binding;

  public ProductCardPackedComponent(
      @NonNull Context context, @NonNull ProductCardPackedBinding binding) {
    this._context = Objects.requireNonNull(context);
    this._binding = Objects.requireNonNull(binding);

    this._binding.image.shapeableImage.setShapeAppearanceModel(
        ShapeAppearanceModel.builder(
                this._context,
                com.google.android.material.R.style.Widget_MaterialComponents_ShapeableImageView,
                R.style.Shape_Card)
            .build());
  }

  public void setProduct(@NonNull ProductModel product) {
    Objects.requireNonNull(product);

    this._setName(product.name());
    this._setPrice(product.price());
  }

  private void _setName(@NonNull String name) {
    Objects.requireNonNull(name);

    this._binding.name.setText(name);
    this._binding.image.text.setText(name.trim().substring(0, Math.min(1, name.trim().length())));
  }

  private void _setPrice(long price) {
    this._binding.price.setText(CurrencyFormat.format(BigDecimal.valueOf(price), "id", "ID"));
  }
}
