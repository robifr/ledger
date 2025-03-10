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

package io.github.robifr.ledger.components

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.shape.ShapeAppearanceModel
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.databinding.ProductCardWideBinding
import io.github.robifr.ledger.util.CurrencyFormat

class ProductCardWideComponent(
    private val _context: Context,
    private val _binding: ProductCardWideBinding
) {
  init {
    val imageShape: ShapeAppearanceModel =
        ShapeAppearanceModel.builder()
            .setAllCornerSizes(_context.resources.getDimension(R.dimen.corner_small))
            .build()
    _binding.normalCard.image.shapeableImage.shapeAppearanceModel = imageShape
    _binding.expandedCard.image.shapeableImage.shapeAppearanceModel = imageShape
  }

  fun setNormalCardProduct(product: ProductModel) {
    _setId(product.id, true)
    _setName(product.name, true)
    _setPrice(product.price, true)
  }

  fun setExpandedCardProduct(product: ProductModel) {
    _setId(product.id, false)
    _setName(product.name, false)
    _setPrice(product.price, false)
  }

  fun setCardExpanded(isExpanded: Boolean) {
    _binding.normalCard.root.isVisible = !isExpanded
    _binding.expandedCard.root.isVisible = isExpanded
  }

  fun setCardChecked(isChecked: Boolean) {
    _binding.cardView.isChecked = isChecked
    _binding.normalCard.image.text.isVisible = !isChecked
    _binding.normalCard.image.icon.isVisible = isChecked
    _binding.expandedCard.image.text.isVisible = !isChecked
    _binding.expandedCard.image.icon.isVisible = isChecked
  }

  fun reset() {
    _binding.normalCard.uniqueId.text = null
    _binding.normalCard.uniqueId.isEnabled = false
    _binding.expandedCard.uniqueId.text = null
    _binding.expandedCard.uniqueId.isEnabled = false

    _binding.cardView.isChecked = false
    _binding.normalCard.name.text = null
    _binding.normalCard.image.text.text = null
    _binding.normalCard.image.icon.isGone = true
    _binding.expandedCard.name.text = null
    _binding.expandedCard.image.text.text = null
    _binding.expandedCard.image.icon.isGone = true

    _binding.normalCard.price.text = null
    _binding.expandedCard.price.text = null
  }

  private fun _setId(id: Long?, isNormalCard: Boolean) {
    val customerId: String = id?.toString() ?: _context.getString(R.string.symbol_notAvailable)
    if (isNormalCard) {
      _binding.normalCard.uniqueId.text = customerId
      _binding.normalCard.uniqueId.isEnabled = id != null
    } else {
      _binding.expandedCard.uniqueId.text = customerId
      _binding.expandedCard.uniqueId.isEnabled = id != null
    }
  }

  private fun _setName(name: String, isNormalCard: Boolean) {
    if (isNormalCard) {
      _binding.normalCard.name.text = name
      _binding.normalCard.image.text.text = name.take(1)
    } else {
      _binding.expandedCard.name.text = name
      _binding.expandedCard.image.text.text = name.take(1)
    }
  }

  private fun _setPrice(price: Long, isNormalCard: Boolean) {
    val formattedPrice: String =
        CurrencyFormat.formatCents(
            price.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
    if (isNormalCard) _binding.normalCard.price.text = formattedPrice
    else _binding.expandedCard.price.text = formattedPrice
  }
}
