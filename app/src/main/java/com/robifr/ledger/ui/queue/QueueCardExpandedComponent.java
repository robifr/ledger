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

package com.robifr.ledger.ui.queue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.data.model.ProductOrderModel;
import com.robifr.ledger.data.model.QueueModel;
import com.robifr.ledger.databinding.QueueCardWideExpandedBinding;
import com.robifr.ledger.databinding.QueueCardWideExpandedOrderBinding;
import com.robifr.ledger.databinding.QueueCardWideExpandedOrderDataBinding;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class QueueCardExpandedComponent {
  @NonNull private final Context _context;
  @NonNull private final QueueCardWideExpandedBinding _binding;
  @NonNull private final QueueCardWideExpandedOrderBinding _productOrderBinding;

  public QueueCardExpandedComponent(
      @NonNull Context context, @NonNull QueueCardWideExpandedBinding binding) {
    this._context = Objects.requireNonNull(context);
    this._binding = Objects.requireNonNull(binding);
    this._productOrderBinding = QueueCardWideExpandedOrderBinding.bind(this._binding.getRoot());

    this._binding.customerImage.shapeableImage.setShapeAppearanceModel(
        ShapeAppearanceModel.builder(
                this._context,
                com.google.android.material.R.style.Widget_MaterialComponents_ShapeableImageView,
                R.style.Shape_Round)
            .build());
  }

  public void setQueue(@NonNull QueueModel queue) {
    Objects.requireNonNull(queue);

    this._setId(queue.id());
    this._setDate(queue.date().atZone(ZoneId.systemDefault()));
    this._setStatus(queue.status());
    this._setPaymentMethod(queue.paymentMethod());
    this._setTotalDiscount(queue.totalDiscount());
    this._setGrandTotalPrice(queue.grandTotalPrice());
    this._setCustomer(queue.customer());
    this._setProductOrders(queue.productOrders());
  }

  public void reset() {
    this._binding.uniqueId.setText(null);
    this._binding.date.setText(null);
    this._binding.paymentMethod.setText(null);

    this._binding.statusChip.setText(null);
    this._binding.statusChip.setTextColor(0);
    this._binding.statusChip.setChipBackgroundColor(null);
    this._binding.coloredSideline.setBackgroundColor(0);

    this._binding.customerImage.text.setText(null);
    this._binding.customerName.setText(null);

    this._productOrderBinding.table.removeAllViews();
    this._productOrderBinding.totalDiscount.setText(null);
    this._productOrderBinding.grandTotalPrice.setText(null);
    this._productOrderBinding.customerBalanceTitle.setText(null);
    this._productOrderBinding.customerBalance.setText(null);
    this._productOrderBinding.customerDebtTitle.setText(null);
    this._productOrderBinding.customerDebt.setText(null);
    this._productOrderBinding.customerDebt.setTextColor(0);
  }

  private void _setId(@Nullable Long id) {
    final boolean isIdExists = id != null;
    final String queueId =
        isIdExists ? id.toString() : this._context.getString(R.string.symbol_notavailable);

    this._binding.uniqueId.setText(queueId);
    this._binding.uniqueId.setEnabled(isIdExists);
  }

  private void _setDate(@NonNull ZonedDateTime date) {
    Objects.requireNonNull(date);

    this._binding.date.setText(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy")));
  }

  private void _setStatus(@NonNull QueueModel.Status status) {
    Objects.requireNonNull(status);

    final int statusBackground = this._context.getColor(status.resourceBackgroundColor());

    this._binding.statusChip.setText(this._context.getString(status.resourceString()));
    this._binding.statusChip.setTextColor(this._context.getColor(status.resourceTextColor()));
    this._binding.statusChip.setChipBackgroundColor(ColorStateList.valueOf(statusBackground));
    this._binding.coloredSideline.setBackgroundColor(statusBackground);
  }

  private void _setPaymentMethod(@NonNull QueueModel.PaymentMethod paymentMethod) {
    Objects.requireNonNull(paymentMethod);

    this._binding.paymentMethod.setText(this._context.getString(paymentMethod.resourceString()));
  }

  private void _setTotalDiscount(@NonNull BigDecimal totalDiscount) {
    Objects.requireNonNull(totalDiscount);

    this._productOrderBinding.totalDiscount.setText(
        CurrencyFormat.format(totalDiscount, "id", "ID"));
  }

  private void _setGrandTotalPrice(@NonNull BigDecimal grandTotalPrice) {
    Objects.requireNonNull(grandTotalPrice);

    this._productOrderBinding.grandTotalPrice.setText(
        CurrencyFormat.format(grandTotalPrice, "id", "ID"));
  }

  private void _setCustomer(@Nullable CustomerModel customer) {
    if (customer == null) {
      this._binding.customerImage.text.setText(null);
      this._binding.customerName.setText(this._context.getString(R.string.symbol_notavailable));
      this._binding.customerName.setEnabled(false);
      this._productOrderBinding.customerBalanceTitle.setVisibility(View.GONE);
      this._productOrderBinding.customerBalance.setVisibility(View.GONE);
      this._productOrderBinding.customerDebtTitle.setVisibility(View.GONE);
      this._productOrderBinding.customerDebt.setVisibility(View.GONE);
      return;
    }

    final String croppedName =
        customer.name().length() > 12 ? customer.name().substring(0, 12) : customer.name();
    final int debtTextColor =
        customer.debt().compareTo(BigDecimal.ZERO) < 0
            // Negative debt will be shown red.
            ? this._context.getColor(R.color.red)
            : this._context.getColor(R.color.text_enabled);

    this._binding.customerImage.text.setText(
        customer.name().trim().substring(0, Math.min(1, customer.name().trim().length())));
    this._binding.customerName.setText(customer.name());
    this._binding.customerName.setEnabled(true);

    // Displaying customer data on the product orders detail.
    this._productOrderBinding.customerBalanceTitle.setText(
        this._context.getString(R.string.productordercard_customerbalance_title, croppedName));
    this._productOrderBinding.customerBalanceTitle.setVisibility(View.VISIBLE);
    this._productOrderBinding.customerBalance.setText(
        CurrencyFormat.format(BigDecimal.valueOf(customer.balance()), "id", "ID"));
    this._productOrderBinding.customerBalance.setVisibility(View.VISIBLE);
    this._productOrderBinding.customerDebtTitle.setText(
        this._context.getString(R.string.productordercard_customerdebt_title, croppedName));
    this._productOrderBinding.customerDebtTitle.setVisibility(View.VISIBLE);
    this._productOrderBinding.customerDebt.setText(
        CurrencyFormat.format(customer.debt(), "id", "ID"));
    this._productOrderBinding.customerDebt.setTextColor(debtTextColor);
    this._productOrderBinding.customerDebt.setVisibility(View.VISIBLE);
  }

  private void _setProductOrders(@NonNull List<ProductOrderModel> productOrders) {
    Objects.requireNonNull(productOrders);

    this._productOrderBinding.table.removeAllViews();

    for (ProductOrderModel productOrder : productOrders) {
      final QueueCardWideExpandedOrderDataBinding dataRowBinding =
          QueueCardWideExpandedOrderDataBinding.inflate(LayoutInflater.from(this._context));
      final boolean isProductNameExists = productOrder.productName() != null;
      final boolean isProductPriceExists = productOrder.productPrice() != null;
      final boolean isProductExists = isProductNameExists && isProductPriceExists;

      final String productName =
          isProductNameExists
              ? productOrder.productName()
              : this._context.getString(R.string.symbol_notavailable);
      final String productPrice =
          isProductPriceExists
              ? CurrencyFormat.format(BigDecimal.valueOf(productOrder.productPrice()), "id", "ID")
              : this._context.getString(R.string.symbol_notavailable);
      final int discountVisibility =
          productOrder.discountPercent().compareTo(BigDecimal.ZERO) == 0 ? View.GONE : View.VISIBLE;

      dataRowBinding.productName.setText(productName);
      dataRowBinding.productName.setEnabled(isProductExists);
      dataRowBinding.productPrice.setText(productPrice);
      dataRowBinding.quantity.setText(
          CurrencyFormat.format(BigDecimal.valueOf(productOrder.quantity()), "id", "ID", ""));
      dataRowBinding.totalPrice.setText(
          CurrencyFormat.format(productOrder.totalPrice(), "id", "ID"));
      dataRowBinding.discount.setText(
          this._context.getString(
              R.string.productordercard_discount_title,
              productOrder.discountPercent().toPlainString()));
      dataRowBinding.discount.setVisibility(discountVisibility);
      this._productOrderBinding.table.addView(dataRowBinding.getRoot());
    }
  }
}
