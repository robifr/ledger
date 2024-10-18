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

package com.robifr.ledger.ui.createqueue.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.text.ParseException

class MakeProductOrderViewModel(private val _viewModel: CreateQueueViewModel) {
  private val _uiState: SafeMutableLiveData<MakeProductOrderState> =
      SafeMutableLiveData(
          MakeProductOrderState(
              isDialogShown = false,
              product = null,
              formattedQuantity = "",
              formattedDiscount = "",
              totalPrice = 0.toBigDecimal(),
              productOrderToEdit = null))
  val uiState: SafeLiveData<MakeProductOrderState>
    get() = _uiState

  private val _inputtedProductOrder: ProductOrderModel
    get() {
      var quantity: Double = 0.0
      try {
        if (_uiState.safeValue.formattedQuantity.isNotBlank()) {
          quantity =
              CurrencyFormat.parse(
                      _uiState.safeValue.formattedQuantity,
                      AppCompatDelegate.getApplicationLocales().toLanguageTags())
                  .stripTrailingZeros()
                  .toDouble()
        }
      } catch (_: ParseException) {}

      var discount: Long = 0L
      try {
        if (_uiState.safeValue.formattedDiscount.isNotBlank()) {
          discount =
              CurrencyFormat.parse(
                      _uiState.safeValue.formattedDiscount,
                      AppCompatDelegate.getApplicationLocales().toLanguageTags())
                  .toLong()
        }
      } catch (_: ParseException) {}

      return ProductOrderModel(
          id = _uiState.safeValue.productOrderToEdit?.id,
          queueId = _uiState.safeValue.productOrderToEdit?.queueId,
          productId = _uiState.safeValue.product?.id,
          productName = _uiState.safeValue.product?.name,
          productPrice = _uiState.safeValue.product?.price,
          totalPrice = _uiState.safeValue.totalPrice,
          quantity = quantity,
          discount = discount)
    }

  fun onProductChanged(product: ProductModel?) {
    _uiState.setValue(_uiState.safeValue.copy(product = product))
    with(_inputtedProductOrder) {
      _onTotalPriceChanged(ProductOrderModel.calculateTotalPrice(productPrice, quantity, discount))
    }
  }

  fun onQuantityTextChanged(formattedQuantity: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedQuantity = formattedQuantity))
    with(_inputtedProductOrder) {
      _onTotalPriceChanged(ProductOrderModel.calculateTotalPrice(productPrice, quantity, discount))
    }
  }

  fun onDiscountTextChanged(formattedDiscount: String) {
    _uiState.setValue(_uiState.safeValue.copy(formattedDiscount = formattedDiscount))
    with(_inputtedProductOrder) {
      _onTotalPriceChanged(ProductOrderModel.calculateTotalPrice(productPrice, quantity, discount))
    }
  }

  fun onShowDialog(productOrder: ProductOrderModel? = null) {
    _uiState.setValue(
        MakeProductOrderState(
            isDialogShown = true,
            product = productOrder?.referencedProduct(),
            formattedQuantity =
                productOrder?.let {
                  CurrencyFormat.format(
                      it.quantity.toBigDecimal(),
                      AppCompatDelegate.getApplicationLocales().toLanguageTags(),
                      "")
                } ?: "",
            formattedDiscount =
                productOrder?.let {
                  CurrencyFormat.format(
                      it.discount.toBigDecimal(),
                      AppCompatDelegate.getApplicationLocales().toLanguageTags())
                } ?: "",
            totalPrice = productOrder?.totalPrice ?: 0.toBigDecimal(),
            productOrderToEdit = productOrder))
  }

  fun onCloseDialog() {
    _uiState.setValue(
        MakeProductOrderState(
            isDialogShown = false,
            product = null,
            formattedQuantity = "",
            formattedDiscount = "",
            totalPrice = 0.toBigDecimal(),
            productOrderToEdit = null))
  }

  fun onSave() {
    _viewModel.onProductOrdersChanged(
        _viewModel.uiState.safeValue.productOrders.toMutableList().apply {
          val indexToUpdate: Int = indexOf(_uiState.safeValue.productOrderToEdit)
          // Add as new when there's no product order to update,
          // otherwise update them with the one user inputted.
          if (indexToUpdate == -1) add(_inputtedProductOrder)
          else set(indexToUpdate, _inputtedProductOrder)
        })
  }

  private fun _onTotalPriceChanged(totalPrice: BigDecimal) {
    _uiState.setValue(_uiState.safeValue.copy(totalPrice = totalPrice))
  }
}
