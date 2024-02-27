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

package com.robifr.ledger.ui.main.customer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.databinding.CustomerCardNormalBinding;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.util.Objects;

public class CustomerCardNormalComponent {
  @NonNull private final Context _context;
  @NonNull private final CustomerCardNormalBinding _binding;

  public CustomerCardNormalComponent(
      @NonNull Context context, @NonNull CustomerCardNormalBinding binding) {
    Objects.requireNonNull(context);

    this._context = context.getApplicationContext();
    this._binding = Objects.requireNonNull(binding);

    this._binding.image.shapeableImage.setShapeAppearanceModel(
        ShapeAppearanceModel.builder(
                this._context,
                com.google.android.material.R.style.Widget_MaterialComponents_ShapeableImageView,
                R.style.Shape_Round)
            .build());
  }

  public void setCustomer(@NonNull CustomerModel customer) {
    Objects.requireNonNull(customer);

    this._setId(customer.id());
    this._setName(customer.name());
    this._setBalance(customer.balance());
    this._setDebt(customer.debt());
  }

  private void _setId(@Nullable Long id) {
    final String customerId = id != null ? id.toString() : "n/a";
    this._binding.uniqueId.setText(customerId);
  }

  private void _setName(@NonNull String name) {
    Objects.requireNonNull(name);

    this._binding.name.setText(name);
    this._binding.image.text.setText(name.trim().substring(0, Math.min(1, name.trim().length())));
  }

  private void _setBalance(@NonNull Long balance) {
    Objects.requireNonNull(balance);

    this._binding.balance.setText(CurrencyFormat.format(BigDecimal.valueOf(balance), "id", "ID"));
  }

  private void _setDebt(@NonNull BigDecimal debt) {
    Objects.requireNonNull(debt);

    final String debtText = CurrencyFormat.format(debt, "id", "ID");
    final int debtTextColor =
        debt.compareTo(BigDecimal.ZERO) < 0
            // Negative debt will be shown red.
            ? ContextCompat.getColor(this._context, R.color.red)
            : ContextCompat.getColor(this._context, R.color.text_enabled);

    this._binding.debt.setText(debtText);
    this._binding.debt.setTextColor(debtTextColor);
  }
}
