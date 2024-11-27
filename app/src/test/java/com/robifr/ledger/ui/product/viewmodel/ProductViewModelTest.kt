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

package com.robifr.ledger.ui.product.viewmodel

import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.display.ProductSortMethod
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class ProductViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _productRepository: ProductRepository
  private val _productChangedListenerCaptor: CapturingSlot<ModelSyncListener<ProductModel>> = slot()
  private lateinit var _viewModel: ProductViewModel

  private val _firstProduct: ProductModel = ProductModel(id = 111L, name = "Apple", price = 200L)
  private val _secondProduct: ProductModel = ProductModel(id = 222L, name = "Banana", price = 300L)
  private val _thirdProduct: ProductModel = ProductModel(id = 333L, name = "Cherry", price = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productRepository = mockk()

    every {
      _productRepository.addModelChangedListener(capture(_productChangedListenerCaptor))
    } just Runs
    every { _productRepository.selectAll() } returns
        CompletableFuture.completedFuture(listOf(_firstProduct, _secondProduct, _thirdProduct))
    _viewModel = ProductViewModel(_dispatcher, _productRepository)
  }

  @Test
  fun `on initialize with unordered name`() {
    every { _productRepository.selectAll() } returns
        CompletableFuture.completedFuture(listOf(_thirdProduct, _firstProduct, _secondProduct))
    _viewModel = ProductViewModel(_dispatcher, _productRepository)
    assertEquals(
        listOf(_firstProduct, _secondProduct, _thirdProduct),
        _viewModel.uiState.safeValue.products,
        "Sort products based from the default sort method")
  }

  @Test
  fun `on cleared`() {
    every { _productRepository.removeModelChangedListener(any()) } just Runs
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listener from the repository") {
      verify { _productRepository.removeModelChangedListener(any()) }
    }
  }

  @Test
  fun `on products changed with unsorted list`() {
    _viewModel.onProductsChanged(listOf(_thirdProduct, _firstProduct, _secondProduct))
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            products = listOf(_firstProduct, _secondProduct, _thirdProduct)),
        _viewModel.uiState.safeValue,
        "Update products with the new sorted list")
  }

  @Test
  fun `on sort method changed with different sort method`() {
    val sortMethod: ProductSortMethod = ProductSortMethod(ProductSortMethod.SortBy.PRICE, true)
    _viewModel.onSortMethodChanged(sortMethod)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            products = listOf(_thirdProduct, _firstProduct, _secondProduct),
            sortMethod = sortMethod),
        _viewModel.uiState.safeValue,
        "Sort products based from the sorting method")
  }

  @Test
  fun `on sort method changed with same sort`() {
    _viewModel.onSortMethodChanged(ProductSortMethod.SortBy.NAME)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            products = listOf(_thirdProduct, _secondProduct, _firstProduct),
            sortMethod = ProductSortMethod(ProductSortMethod.SortBy.NAME, false)),
        _viewModel.uiState.safeValue,
        "Reverse sort order when selecting the same sort option")
  }

  @Test
  fun `on sort method changed with different sort`() {
    _viewModel.onSortMethodChanged(ProductSortMethod.SortBy.PRICE)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            products = listOf(_thirdProduct, _firstProduct, _secondProduct),
            sortMethod = ProductSortMethod(ProductSortMethod.SortBy.PRICE, true)),
        _viewModel.uiState.safeValue,
        "Sort products based from the sorting method")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on expanded product index changed`(isSameIndexSelected: Boolean) {
    _viewModel.onExpandedProductIndexChanged(0)

    _viewModel.onExpandedProductIndexChanged(if (isSameIndexSelected) 0 else 1)
    assertEquals(
        if (isSameIndexSelected) -1 else 1,
        _viewModel.uiState.safeValue.expandedProductIndex,
        "Update expanded product index and reset when selecting the same one")
  }

  @Test
  fun `on sync product from database`() {
    val updatedProducts: List<ProductModel> =
        listOf(
            _firstProduct.copy(price = _firstProduct.price + 100L), _secondProduct, _thirdProduct)
    every { _productRepository.notifyModelUpdated(any()) } answers
        {
          _productChangedListenerCaptor.captured.onModelUpdated(updatedProducts)
        }
    _productRepository.notifyModelUpdated(updatedProducts)
    assertEquals(
        updatedProducts,
        _viewModel.uiState.safeValue.products,
        "Sync products when any are updated in the database")
  }
}
