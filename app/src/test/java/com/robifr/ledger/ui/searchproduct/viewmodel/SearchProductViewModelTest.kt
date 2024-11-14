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

package com.robifr.ledger.ui.searchproduct.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.searchproduct.SearchProductFragment
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class SearchProductViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _productRepository: ProductRepository
  private val _productChangedListenerCaptor: CapturingSlot<ModelSyncListener<ProductModel>> = slot()
  private lateinit var _viewModel: SearchProductViewModel

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productRepository = mockk()

    every {
      _productRepository.addModelChangedListener(capture(_productChangedListenerCaptor))
    } just Runs
    _viewModel = SearchProductViewModel(SavedStateHandle(), _dispatcher, _productRepository)
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        SearchProductViewModel(
            SavedStateHandle().apply {
              set(SearchProductFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key, true)
              set(SearchProductFragment.Arguments.INITIAL_QUERY_STRING.key, "Apple")
              set(
                  SearchProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_IDS_LONG_ARRAY.key,
                  longArrayOf(111L))
            },
            _dispatcher,
            _productRepository)
    assertEquals(
        SearchProductState(
            isSelectionEnabled = true,
            initialQuery = "Apple",
            query = "",
            initialSelectedProductIds = listOf(111L),
            products = listOf(),
            expandedProductIndex = -1),
        _viewModel.uiState.safeValue,
        "Match state with the retrieved data from the fragment argument")
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
  fun `on search with fast input`() = runTest {
    every { _productRepository.search(any()) } returns CompletableFuture.completedFuture(listOf())
    _viewModel.onSearch("A")
    _viewModel.onSearch("B")
    _viewModel.onSearch("C")
    advanceUntilIdle()
    assertDoesNotThrow("Prevent search from triggering multiple times when typing quickly") {
      verify(atMost = 1) { _productRepository.search(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["A", "Apple", "Banana", "  "])
  fun `on search with complete query`(query: String) = runTest {
    val products: List<ProductModel> =
        if (query.contains("A", ignoreCase = true)) {
          listOf(ProductModel(id = 111L, name = "Apple"), ProductModel(id = 222L, name = "Banana"))
        } else {
          listOf()
        }
    every { _productRepository.search(query) } returns CompletableFuture.completedFuture(products)
    _viewModel.onSearch(query)
    advanceUntilIdle()
    assertEquals(
        _viewModel.uiState.safeValue.copy(query = query, products = products),
        _viewModel.uiState.safeValue,
        "Update products based from the queried search result")
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

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on product selected`(isProductNull: Boolean) {
    val product: ProductModel? =
        if (!isProductNull) ProductModel(id = 111L, name = "Apple") else null
    _viewModel.onProductSelected(product)
    assertEquals(
        SearchProductResultState(product?.id),
        _viewModel.resultState.value,
        "Update result state based from the selected product")
  }

  @Test
  fun `on sync product from database`() = runTest {
    val product: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)
    every { _productRepository.search(any()) } returns
        CompletableFuture.completedFuture(listOf(product))
    _viewModel.onSearch("Apple")
    advanceUntilIdle()

    val updatedProduct: ProductModel = product.copy(price = product.price + 100L)
    every { _productRepository.notifyModelUpdated(any()) } answers
        {
          _productChangedListenerCaptor.captured.onModelUpdated(listOf(updatedProduct))
        }
    _productRepository.notifyModelUpdated(listOf(updatedProduct))
    assertEquals(
        listOf(updatedProduct),
        _viewModel.uiState.safeValue.products,
        "Sync products when any are updated in the database")
  }
}
