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
import android.content.res.ColorStateList
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.display.LanguageOption
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.data.model.QueuePaginatedInfo
import io.github.robifr.ledger.databinding.QueueCardWideBinding
import io.github.robifr.ledger.databinding.QueueCardWideExpandedOrderBinding
import io.github.robifr.ledger.databinding.QueueCardWideExpandedOrderDataBinding
import io.github.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class QueueCardWideComponent(
    private val _context: Context,
    private val _binding: QueueCardWideBinding
) {
  private val _productOrderBinding: QueueCardWideExpandedOrderBinding =
      QueueCardWideExpandedOrderBinding.bind(_binding.expandedCard.root)

  init {
    val imageShape: ShapeAppearanceModel =
        ShapeAppearanceModel.builder().setAllCornerSizes(RelativeCornerSize(0.5f)).build()
    _binding.normalCard.customerImage.shapeableImage.shapeAppearanceModel = imageShape
    _binding.expandedCard.customerImage.shapeableImage.shapeAppearanceModel = imageShape
  }

  fun setNormalCardQueue(queue: QueuePaginatedInfo) {
    _setId(queue.id, true)
    _setDate(queue.date.atZone(ZoneId.systemDefault()), true)
    _setStatus(queue.status, true)
    _setGrandTotalPrice(queue.grandTotalPrice, true)
    _setCustomerOnNormalCard(queue.customerId, queue.customerName)
  }

  fun setExpandedCardQueue(queue: QueueModel) {
    _setId(queue.id, false)
    _setDate(queue.date.atZone(ZoneId.systemDefault()), false)
    _setStatus(queue.status, false)
    _setGrandTotalPrice(queue.grandTotalPrice(), false)
    _setCustomerOnExpandedCard(queue.customer)
    _setPaymentMethod(queue.paymentMethod, queue.status)
    _setTotalDiscount(queue.totalDiscount())
    _setProductOrders(queue.productOrders)
    _setNote(queue.note)
  }

  fun setCardExpanded(isExpanded: Boolean) {
    _binding.normalCard.root.isVisible = !isExpanded
    _binding.expandedCard.root.isVisible = isExpanded
  }

  fun reset() {
    _binding.normalCard.uniqueId.text = null
    _binding.normalCard.uniqueId.isEnabled = false
    _binding.expandedCard.uniqueId.text = null
    _binding.expandedCard.uniqueId.isEnabled = false

    _binding.normalCard.date.text = null
    _binding.expandedCard.date.text = null

    _binding.normalCard.statusChip.text = null
    _binding.normalCard.statusChip.setTextColor(0)
    _binding.normalCard.statusChip.chipBackgroundColor = ColorStateList.valueOf(0)
    _binding.normalCard.coloredSideline.setBackgroundColor(0)

    _binding.expandedCard.statusChip.text = null
    _binding.expandedCard.statusChip.setTextColor(0)
    _binding.expandedCard.statusChip.chipBackgroundColor = ColorStateList.valueOf(0)
    _binding.expandedCard.coloredSideline.setBackgroundColor(0)

    _binding.expandedCard.paymentMethod.text = null

    _productOrderBinding.table.removeAllViews()
    _productOrderBinding.totalDiscount.text = null
    _binding.normalCard.grandTotalPrice.text = null
    _productOrderBinding.grandTotalPrice.text = null

    _binding.normalCard.customerImage.text.text = null
    _binding.normalCard.customerName.text = null
    _binding.normalCard.customerName.isEnabled = false
    _binding.expandedCard.customerImage.text.text = null
    _binding.expandedCard.customerName.text = null
    _binding.expandedCard.customerName.isEnabled = false
    // Customer data on the expanded card's product orders detail.
    _productOrderBinding.customerBalanceTitle.text = null
    _productOrderBinding.customerBalanceTitle.isGone = true
    _productOrderBinding.customerBalance.text = null
    _productOrderBinding.customerBalance.isGone = true
    _productOrderBinding.customerDebtTitle.text = null
    _productOrderBinding.customerDebtTitle.isGone = true
    _productOrderBinding.customerDebt.text = null
    _productOrderBinding.customerDebt.setTextColor(0)
    _productOrderBinding.customerDebt.isGone = true
  }

  private fun _setId(id: Long?, isNormalCard: Boolean) {
    val queueId: String = id?.toString() ?: _context.getString(R.string.symbol_notAvailable)
    if (isNormalCard) {
      _binding.normalCard.uniqueId.text = queueId
      _binding.normalCard.uniqueId.isEnabled = id != null
    } else {
      _binding.expandedCard.uniqueId.text = queueId
      _binding.expandedCard.uniqueId.isEnabled = id != null
    }
  }

  private fun _setDate(date: ZonedDateTime, isNormalCard: Boolean) {
    val formattedDate: String =
        date.format(
            DateTimeFormatter.ofPattern(
                _context.getString(
                    LanguageOption.entries
                        .find {
                          it.languageTag ==
                              AppCompatDelegate.getApplicationLocales().toLanguageTags()
                        }
                        ?.shortDateFormat ?: LanguageOption.ENGLISH_US.shortDateFormat)))
    if (isNormalCard) _binding.normalCard.date.text = formattedDate
    else _binding.expandedCard.date.text = formattedDate
  }

  private fun _setStatus(status: QueueModel.Status, isNormalCard: Boolean) {
    val statusTextColor: Int = _context.getColor(status.textColorRes)
    val statusBackground: Int = _context.getColor(status.backgroundColorRes)
    if (isNormalCard) {
      _binding.normalCard.statusChip.setText(status.stringRes)
      _binding.normalCard.statusChip.setTextColor(statusTextColor)
      _binding.normalCard.statusChip.chipBackgroundColor = ColorStateList.valueOf(statusBackground)
      _binding.normalCard.coloredSideline.setBackgroundColor(statusBackground)
    } else {
      _binding.expandedCard.statusChip.setText(status.stringRes)
      _binding.expandedCard.statusChip.setTextColor(statusTextColor)
      _binding.expandedCard.statusChip.chipBackgroundColor =
          ColorStateList.valueOf(statusBackground)
      _binding.expandedCard.coloredSideline.setBackgroundColor(statusBackground)
    }
  }

  private fun _setPaymentMethod(
      paymentMethod: QueueModel.PaymentMethod,
      status: QueueModel.Status
  ) {
    val isStatusCompleted: Boolean = status == QueueModel.Status.COMPLETED
    _binding.expandedCard.paymentMethodTitle.isVisible = isStatusCompleted
    _binding.expandedCard.paymentMethod.setText(paymentMethod.stringRes)
    _binding.expandedCard.paymentMethod.isVisible = isStatusCompleted
  }

  private fun _setTotalDiscount(totalDiscount: BigDecimal) {
    _productOrderBinding.totalDiscount.text =
        CurrencyFormat.formatCents(
            totalDiscount, AppCompatDelegate.getApplicationLocales().toLanguageTags())
  }

  private fun _setGrandTotalPrice(grandTotalPrice: BigDecimal, isNormalCard: Boolean) {
    val formattedGrandTotalPrice: String =
        CurrencyFormat.formatCents(
            grandTotalPrice, AppCompatDelegate.getApplicationLocales().toLanguageTags())
    if (isNormalCard) _binding.normalCard.grandTotalPrice.text = formattedGrandTotalPrice
    else _productOrderBinding.grandTotalPrice.text = formattedGrandTotalPrice
  }

  private fun _setCustomerOnNormalCard(customerId: Long?, customerName: String?) {
    val customerImageText: String? = customerName?.take(1)
    val customerName: String = customerName ?: _context.getString(R.string.symbol_notAvailable)
    _binding.normalCard.customerImage.text.text = customerImageText
    _binding.normalCard.customerName.text = customerName
    _binding.normalCard.customerName.isEnabled = customerId != null
  }

  private fun _setCustomerOnExpandedCard(customer: CustomerModel?) {
    val customerImageText: String? = customer?.name?.take(1)
    val customerName: String = customer?.name ?: _context.getString(R.string.symbol_notAvailable)
    val croppedCustomerName: String? =
        customer?.let { if (it.name.length > 12) it.name.take(12) else it.name }
    _binding.expandedCard.customerImage.text.text = customerImageText
    _binding.expandedCard.customerName.text = customerName
    _binding.expandedCard.customerName.isEnabled = customer != null
    // Displaying customer data on the product orders detail.
    _productOrderBinding.customerBalanceTitle.text =
        _context.getString(R.string.queue_card_x_balance, croppedCustomerName)
    _productOrderBinding.customerBalanceTitle.isVisible = customer != null
    _productOrderBinding.customerBalance.text =
        customer?.let {
          CurrencyFormat.formatCents(
              it.balance.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
        }
    _productOrderBinding.customerBalance.isVisible = customer != null
    _productOrderBinding.customerDebtTitle.text =
        _context.getString(R.string.queue_card_x_debt, croppedCustomerName)
    _productOrderBinding.customerDebtTitle.isVisible = customer != null
    _productOrderBinding.customerDebt.text =
        customer?.let {
          CurrencyFormat.formatCents(
              it.debt, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        }
    _productOrderBinding.customerDebt.setTextColor(
        if (customer != null && customer.debt.compareTo(0.toBigDecimal()) < 0) {
          _context.getColor(R.color.red) // Negative debt will be shown red.
        } else {
          _context.getColor(R.color.text_enabled)
        })
    _productOrderBinding.customerDebt.isVisible = customer != null
  }

  private fun _setProductOrders(productOrders: List<ProductOrderModel>) {
    _productOrderBinding.table.removeAllViews()
    for (productOrder in productOrders) {
      _productOrderBinding.table.addView(
          QueueCardWideExpandedOrderDataBinding.inflate(LayoutInflater.from(_context))
              .apply {
                productName.text =
                    productOrder.productName ?: _context.getString(R.string.symbol_notAvailable)
                productName.isEnabled =
                    productOrder.productName != null && productOrder.productPrice != null
                productPrice.text =
                    productOrder.productPrice?.let {
                      CurrencyFormat.formatCents(
                          it.toBigDecimal(),
                          AppCompatDelegate.getApplicationLocales().toLanguageTags())
                    } ?: _context.getString(R.string.symbol_notAvailable)
                quantity.text =
                    CurrencyFormat.format(
                        productOrder.quantity.toBigDecimal(),
                        AppCompatDelegate.getApplicationLocales().toLanguageTags(),
                        "",
                        CurrencyFormat.countDecimalPlace(productOrder.quantity.toBigDecimal()))
                totalPrice.text =
                    CurrencyFormat.formatCents(
                        productOrder.totalPrice,
                        AppCompatDelegate.getApplicationLocales().toLanguageTags())
                discount.text =
                    _context.getString(
                        R.string.queue_card_productOrders_n_off,
                        productOrder.discountPercent().toPlainString())
                discount.isVisible = productOrder.discountPercent().compareTo(0.toBigDecimal()) != 0
              }
              .root)
    }
  }

  private fun _setNote(note: String) {
    _binding.expandedCard.dividerForProductOrders.isGone = note.isBlank()
    _binding.expandedCard.noteTitle.isGone = note.isBlank()
    _binding.expandedCard.note.text = note
    _binding.expandedCard.note.isGone = note.isBlank()
  }
}
