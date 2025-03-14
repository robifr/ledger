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

package io.github.robifr.ledger.ui.createqueue

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.databinding.CreateQueueDialogProductOrderBinding
import io.github.robifr.ledger.ui.common.CurrencyTextWatcher
import io.github.robifr.ledger.ui.selectproduct.SelectProductFragment
import io.github.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal

class CreateQueueMakeProductOrder(private val _fragment: CreateQueueFragment) {
  private val _dialogBinding: CreateQueueDialogProductOrderBinding =
      CreateQueueDialogProductOrderBinding.inflate(_fragment.layoutInflater).apply {
        product.setOnClickListener {
          _fragment
              .findNavController()
              .navigate(
                  R.id.selectProductFragment,
                  Bundle().apply {
                    putParcelable(
                        SelectProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_PARCELABLE.key(),
                        _fragment.createQueueViewModel.makeProductOrderView.uiState.safeValue
                            .product)
                  })
          _dialog.hide()
        }
      }
  private val _dialog: AlertDialog =
      MaterialAlertDialogBuilder(_fragment.requireContext())
          .setView(_dialogBinding.root)
          .setNegativeButton(R.string.action_cancel) { _, _ -> }
          .setPositiveButton(R.string.action_add) { _, _ ->
            _fragment.createQueueViewModel.makeProductOrderView.onSave()
          }
          .create()
          .apply {
            setOnDismissListener {
              _fragment.createQueueViewModel.makeProductOrderView.onDialogClosed()
            }
          }
  private val _quantityTextWatcher: QuantityTextWatcher =
      QuantityTextWatcher(_fragment, _dialogBinding.quantity)
  private val _discountTextWatcher: DiscountTextWatcher =
      DiscountTextWatcher(_fragment, _dialogBinding.discount)

  init {
    _dialogBinding.quantity.addTextChangedListener(_quantityTextWatcher)
    _dialogBinding.discount.addTextChangedListener(_discountTextWatcher)
  }

  fun setInputtedProduct(product: ProductModel?) {
    _dialogBinding.product.setText(
        product?.let {
          val productName: String = it.name + "\n"
          val productPrice: String =
              CurrencyFormat.formatCents(
                  it.price.toBigDecimal(),
                  AppCompatDelegate.getApplicationLocales().toLanguageTags())
          SpannableString(productName + productPrice).apply {
            // Set product price text smaller than its name.
            setSpan(
                AbsoluteSizeSpan(_fragment.resources.getDimensionPixelSize(R.dimen.text_small)),
                productName.length,
                productName.length + productPrice.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            // Coloring product price text with gray.
            setSpan(
                ForegroundColorSpan(_fragment.requireContext().getColor(R.color.text_disabled)),
                productName.length,
                productName.length + productPrice.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
          }
        })
  }

  fun setInputtedQuantityText(formattedQuantity: String) {
    if (_dialogBinding.quantity.text.toString() == formattedQuantity) return
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.quantity.removeTextChangedListener(_quantityTextWatcher)
    _dialogBinding.quantity.setText(formattedQuantity)
    _dialogBinding.quantity.addTextChangedListener(_quantityTextWatcher)
  }

  fun setInputtedDiscountText(formattedDiscount: String) {
    if (_dialogBinding.discount.text.toString() == formattedDiscount) return
    // Remove listener to prevent any sort of formatting.
    _dialogBinding.discount.removeTextChangedListener(_discountTextWatcher)
    _dialogBinding.discount.setText(formattedDiscount)
    _dialogBinding.discount.addTextChangedListener(_discountTextWatcher)
  }

  fun setInputtedTotalPrice(totalPrice: BigDecimal) {
    _dialogBinding.totalPrice.text =
        CurrencyFormat.formatCents(
            totalPrice, AppCompatDelegate.getApplicationLocales().toLanguageTags())
  }

  fun showCreateDialog(isAddButtonEnabled: Boolean) {
    _dialogBinding.title.setText(R.string.createQueue_productOrders_makeProductOrders)
    _dialog.show()
    _dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setText(R.string.action_add)
    _dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = isAddButtonEnabled
  }

  fun showEditDialog() {
    _dialogBinding.title.setText(R.string.createQueue_productOrders_editProductOrders)
    _dialog.show()
    _dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setText(R.string.action_save)
    _dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = true
  }

  fun dismissDialog() {
    _dialog.dismiss()
    _dialog.currentFocus?.clearFocus()
  }
}

private class QuantityTextWatcher(private val _fragment: CreateQueueFragment, view: EditText) :
    CurrencyTextWatcher(
        view = view,
        _maximumAmount = 10_000.toBigDecimal(),
        _isSymbolHidden = true,
        _fractionDigits = ProductOrderModel.QUANTITY_MAX_FRACTION_DIGITS) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.createQueueViewModel.makeProductOrderView.onQuantityTextChanged(newText())
  }
}

private class DiscountTextWatcher(private val _fragment: CreateQueueFragment, view: EditText) :
    CurrencyTextWatcher(view) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.createQueueViewModel.makeProductOrderView.onDiscountTextChanged(newText())
  }
}
