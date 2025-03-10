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
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.databinding.CustomerCardWideBinding
import io.github.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal

class CustomerCardWideComponent(
    private val _context: Context,
    private val _binding: CustomerCardWideBinding
) {
  init {
    val imageShape: ShapeAppearanceModel =
        ShapeAppearanceModel.builder().setAllCornerSizes(RelativeCornerSize(0.5f)).build()
    _binding.normalCard.image.shapeableImage.shapeAppearanceModel = imageShape
    _binding.expandedCard.image.shapeableImage.shapeAppearanceModel = imageShape
  }

  fun setNormalCardCustomer(customer: CustomerModel) {
    _setId(customer.id, true)
    _setName(customer.name, true)
    _setBalance(customer.balance, true)
    _setDebt(customer.debt, true)
  }

  fun setExpandedCardCustomer(customer: CustomerModel) {
    _setId(customer.id, false)
    _setName(customer.name, false)
    _setBalance(customer.balance, false)
    _setDebt(customer.debt, false)
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

    _binding.normalCard.name.text = null
    _binding.normalCard.image.text.text = null
    _binding.normalCard.image.icon.isGone = true
    _binding.expandedCard.name.text = null
    _binding.expandedCard.image.text.text = null
    _binding.expandedCard.image.icon.isGone = true

    _binding.normalCard.balance.text = null
    _binding.expandedCard.balance.text = null

    _binding.normalCard.debt.text = null
    _binding.normalCard.debt.setTextColor(0)
    _binding.expandedCard.debt.text = null
    _binding.expandedCard.debt.setTextColor(0)
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

  private fun _setBalance(balance: Long, isNormalCard: Boolean) {
    val formattedBalance: String =
        CurrencyFormat.formatCents(
            balance.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
    if (isNormalCard) _binding.normalCard.balance.text = formattedBalance
    else _binding.expandedCard.balance.text = formattedBalance
  }

  private fun _setDebt(debt: BigDecimal, isNormalCard: Boolean) {
    val debtText: String =
        CurrencyFormat.formatCents(debt, AppCompatDelegate.getApplicationLocales().toLanguageTags())
    // Negative debt will be shown red.
    val debtTextColor: Int =
        if (debt.compareTo(0.toBigDecimal()) < 0) _context.getColor(R.color.red)
        else _context.getColor(R.color.text_enabled)
    if (isNormalCard) {
      _binding.normalCard.debt.text = debtText
      _binding.normalCard.debt.setTextColor(debtTextColor)
    } else {
      _binding.expandedCard.debt.text = debtText
      _binding.expandedCard.debt.setTextColor(debtTextColor)
    }
  }
}
