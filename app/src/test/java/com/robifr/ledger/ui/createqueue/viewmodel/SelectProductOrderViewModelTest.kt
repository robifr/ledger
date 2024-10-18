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
class SelectProductOrderViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _createQueueViewModel: CreateQueueViewModel
  private lateinit var _viewModel: SelectProductOrderViewModel

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
    _createQueueViewModel = CreateQueueViewModel(mockk(), mockk(), mockk(), _dispatcher)
    _viewModel = _createQueueViewModel.selectProductOrderView
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on product order checked`(isChecked: Boolean) {
    _viewModel.onProductOrderCheckedChanged(0, isChecked)
    _viewModel.onProductOrderCheckedChanged(1, isChecked)
    assertEquals(
        if (isChecked) setOf(0, 1) else setOf(),
        _viewModel.uiState.safeValue.selectedIndexes,
        "Add the index of the checked product order to the state and remove it when unchecked")
  }

  @Test
  fun `on delete selected product order with unordered selection`() {
    val productOrders: List<ProductOrderModel> =
        listOf(
            _productOrder,
            _productOrder.copy(id = 222L),
            _productOrder.copy(id = 333L),
            _productOrder.copy(id = 444L),
            _productOrder.copy(id = 555L))
    _createQueueViewModel.onProductOrdersChanged(productOrders)
    _viewModel.onProductOrderCheckedChanged(4, true)
    _viewModel.onProductOrderCheckedChanged(0, true)
    _viewModel.onProductOrderCheckedChanged(2, true)
    _viewModel.onProductOrderCheckedChanged(1, true)

    _viewModel.onDeleteSelectedProductOrder()
    assertEquals(
        productOrders.filterIndexed { index, _ -> index !in setOf(0, 1, 2, 4) },
        _createQueueViewModel.uiState.safeValue.productOrders,
        "Remove selected product order from the queue view model's state")
  }

  @Test
  fun `on disable contextual mode`() {
    _viewModel.onDisableContextualMode()
    assertEquals(
        SelectProductOrderState(setOf()),
        _viewModel.uiState.safeValue,
        "Reset entire state when the contextual mode disabled")
  }
}
