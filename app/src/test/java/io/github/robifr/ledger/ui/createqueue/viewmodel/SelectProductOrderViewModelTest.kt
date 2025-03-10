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

package io.github.robifr.ledger.ui.createqueue.viewmodel

import io.github.robifr.ledger.InstantTaskExecutorExtension
import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.mockk.clearAllMocks
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.assertj.core.api.Assertions.assertThat
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
    _createQueueViewModel = CreateQueueViewModel(_dispatcher, mockk(), mockk(), mockk())
    _viewModel = _createQueueViewModel.selectProductOrderView
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on product order checked changed`(isSameProductOrderIndexChecked: Boolean) {
    _viewModel.onProductOrderCheckedChanged(0)

    _viewModel.onProductOrderCheckedChanged(if (isSameProductOrderIndexChecked) 0 else 1)
    assertThat(_viewModel.uiState.safeValue.selectedIndexes)
        .describedAs("Add checked index to the selected indexes and remove it when double checked")
        .isEqualTo(if (isSameProductOrderIndexChecked) setOf() else setOf(0, 1))
  }

  @Test
  fun `on delete selected product order with unordered selection`() {
    val unselectedProductOrder: ProductOrderModel = _productOrder.copy(id = 444L)
    val productOrders: List<ProductOrderModel> =
        listOf(
            _productOrder,
            _productOrder.copy(id = 222L),
            _productOrder.copy(id = 333L),
            unselectedProductOrder,
            _productOrder.copy(id = 555L))
    _createQueueViewModel.onProductOrdersChanged(productOrders)
    _viewModel.onProductOrderCheckedChanged(4)
    _viewModel.onProductOrderCheckedChanged(0)
    _viewModel.onProductOrderCheckedChanged(2)
    _viewModel.onProductOrderCheckedChanged(1)

    _viewModel.onDeleteSelectedProductOrder()
    assertThat(_createQueueViewModel.uiState.safeValue.productOrders)
        .describedAs("Remove selected product order from the queue view model's state")
        .isEqualTo(listOf(unselectedProductOrder))
  }

  @Test
  fun `on disable contextual mode`() {
    _viewModel.onDisableContextualMode()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Reset entire state when the contextual mode disabled")
        .isEqualTo(SelectProductOrderState(setOf()))
  }
}
