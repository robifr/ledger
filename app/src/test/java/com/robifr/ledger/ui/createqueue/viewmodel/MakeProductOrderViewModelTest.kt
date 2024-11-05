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
import androidx.core.os.LocaleListCompat
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductOrderModel
import io.mockk.clearAllMocks
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class MakeProductOrderViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _createQueueViewModel: CreateQueueViewModel
  private lateinit var _viewModel: MakeProductOrderViewModel

  private val _product: ProductModel = ProductModel(id = 111L, name = "Apple", price = 500)
  private val _productOrder: ProductOrderModel =
      ProductOrderModel(
          id = 111L,
          queueId = 111L,
          productId = _product.id,
          productName = _product.name,
          productPrice = _product.price,
          quantity = 1.0,
          discount = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en-us"))
    _createQueueViewModel = CreateQueueViewModel(_dispatcher, mockk(), mockk(), mockk())
    _viewModel = _createQueueViewModel.makeProductOrderView
  }

  @Test
  fun `on state changed`() {
    _viewModel.onProductChanged(_product)
    _viewModel.onQuantityTextChanged(_productOrder.quantity.toString())
    _viewModel.onDiscountTextChanged("$${_productOrder.discount}")
    assertEquals(
        MakeProductOrderState(
            isDialogShown = false,
            product = _product,
            formattedQuantity = _productOrder.quantity.toString(),
            formattedDiscount = "$${_productOrder.discount}",
            totalPrice = _productOrder.totalPrice,
            productOrderToEdit = null),
        _viewModel.uiState.safeValue,
        "Preserve all values except for the changed field")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on dialog shown`(isProductOrderNull: Boolean) {
    _viewModel.onDialogShown(if (!isProductOrderNull) _productOrder else null)
    assertEquals(
        MakeProductOrderState(
            isDialogShown = true,
            product = if (!isProductOrderNull) _productOrder.referencedProduct() else null,
            formattedQuantity =
                if (!isProductOrderNull) {
                  _productOrder.quantity.toBigDecimal().stripTrailingZeros().toPlainString()
                } else {
                  ""
                },
            formattedDiscount = if (!isProductOrderNull) "$${_productOrder.discount}" else "",
            totalPrice = if (!isProductOrderNull) _productOrder.totalPrice else 0.toBigDecimal(),
            productOrderToEdit = if (!isProductOrderNull) _productOrder else null),
        _viewModel.uiState.safeValue,
        "Correctly update state based on the provided product order when the dialog opens")
  }

  @Test
  fun `on dialog closed`() {
    _viewModel.onDialogClosed()
    assertEquals(
        MakeProductOrderState(
            isDialogShown = false,
            product = null,
            formattedQuantity = "",
            formattedDiscount = "",
            totalPrice = 0.toBigDecimal(),
            productOrderToEdit = null),
        _viewModel.uiState.safeValue,
        "Reset entire state when the dialog closes")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on save`(isEditingProductOrder: Boolean) {
    _createQueueViewModel.onProductOrdersChanged(
        if (isEditingProductOrder) listOf(_productOrder) else listOf())
    _viewModel.onDialogShown(_productOrder)
    _viewModel.onDiscountTextChanged("$${_productOrder.discount + 100L}")

    _viewModel.onSave()
    assertEquals(
        listOf(
            _productOrder.copy(
                discount = _productOrder.discount + 100L,
                totalPrice = _productOrder.totalPrice - 100.toBigDecimal())),
        _createQueueViewModel.uiState.safeValue.productOrders,
        "Add saved product order to the queue view model's state")
  }
}