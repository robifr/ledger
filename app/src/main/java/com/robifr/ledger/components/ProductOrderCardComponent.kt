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

package com.robifr.ledger.components

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import com.google.android.material.shape.ShapeAppearanceModel
import com.robifr.ledger.R
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.databinding.ProductOrderCardBinding
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal

class ProductOrderCardComponent(
    private val _context: Context,
    private val _binding: ProductOrderCardBinding
) {
  init {
    _binding.productImage.shapeableImage.shapeAppearanceModel =
        ShapeAppearanceModel.builder()
            .setAllCornerSizes(_context.resources.getDimension(R.dimen.corner_small))
            .build()
  }

  fun setProductOrder(productOrder: ProductOrderModel) {
    _setProductName(productOrder.productName)
    _setProductPriceAndQuantity(productOrder.productPrice, productOrder.quantity)
    _setTotalPrice(productOrder.totalPrice)
    _setDiscount(productOrder.discountPercent())
  }

  private fun _setProductName(productName: String?) {
    val name: String = productName ?: _context.getString(R.string.symbol_notAvailable)
    _binding.productName.text = name
    _binding.productName.isEnabled = productName != null
    _binding.productImage.text.text = name.take(1)
  }

  private fun _setProductPriceAndQuantity(productPrice: Long?, quantity: Double) {
    val formattedProductPrice: String =
        productPrice?.let {
          CurrencyFormat.formatCents(
              it.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } ?: _context.getString(R.string.symbol_notAvailable)
    val formattedQuantity: String =
        CurrencyFormat.format(
            quantity.toBigDecimal(),
            AppCompatDelegate.getApplicationLocales().toLanguageTags(),
            "",
            CurrencyFormat.countDecimalPlace(quantity.toBigDecimal()))
    _binding.productPriceQuantity.text =
        _context.getString(
            R.string.createQueue_productOrders_n_multiply_n,
            formattedProductPrice,
            formattedQuantity)
  }

  private fun _setTotalPrice(totalPrice: BigDecimal) {
    _binding.totalPrice.text =
        CurrencyFormat.formatCents(
            totalPrice, AppCompatDelegate.getApplicationLocales().toLanguageTags())
  }

  private fun _setDiscount(discountPercent: BigDecimal) {
    _binding.discount.isVisible = discountPercent.compareTo(0.toBigDecimal()) != 0
    _binding.discount.text =
        if (discountPercent.compareTo(0.toBigDecimal()) != 0) {
          _context.getString(
              R.string.createQueue_productOrders_n_off, discountPercent.toPlainString())
        } else {
          null
        }
  }
}
