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

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.search.viewmodel.SearchState
import com.robifr.ledger.ui.searchproduct.SearchProductFragment
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(
    InstantTaskExecutorExtension::class,
    MainCoroutineExtension::class,
    LifecycleOwnerExtension::class)
class SearchProductViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _productRepository: ProductRepository
  private val _productChangedListenerCaptor: CapturingSlot<ModelSyncListener<ProductModel>> = slot()
  private lateinit var _viewModel: SearchProductViewModel
  private lateinit var _uiEventObserver: Observer<SearchProductEvent>

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productRepository = mockk()
    _uiEventObserver = mockk(relaxed = true)

    every {
      _productRepository.addModelChangedListener(capture(_productChangedListenerCaptor))
    } just Runs
    _viewModel = SearchProductViewModel(SavedStateHandle(), _dispatcher, _productRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on initialize with no arguments`() {
    assertEquals(
        SearchProductState(
            isSelectionEnabled = false,
            isToolbarVisible = true,
            initialQuery = "",
            query = "",
            initialSelectedProductIds = listOf(),
            products = listOf(),
            expandedProductIndex = -1,
            isProductMenuDialogShown = false,
            selectedProductMenu = null),
        _viewModel.uiState.safeValue,
        "Apply the default state if no fragment arguments are provided")
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        SearchProductViewModel(
            SavedStateHandle().apply {
              set(SearchProductFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key(), true)
              set(SearchProductFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key(), false)
              set(SearchProductFragment.Arguments.INITIAL_QUERY_STRING.key(), "Apple")
              set(
                  SearchProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_IDS_LONG_ARRAY.key(),
                  longArrayOf(111L))
            },
            _dispatcher,
            _productRepository)
    assertEquals(
        SearchProductState(
            isSelectionEnabled = true,
            isToolbarVisible = false,
            initialQuery = "Apple",
            query = "",
            initialSelectedProductIds = listOf(111L),
            products = listOf(),
            expandedProductIndex = -1,
            isProductMenuDialogShown = false,
            selectedProductMenu = null),
        _viewModel.uiState.safeValue,
        "Match state with the retrieved data from the fragment arguments")
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
    coEvery { _productRepository.search(any()) } returns listOf()
    _viewModel.onSearch("A")
    _viewModel.onSearch("B")
    _viewModel.onSearch("C")
    advanceUntilIdle()
    assertDoesNotThrow("Prevent search from triggering multiple times when typing quickly") {
      coVerify(atMost = 1) { _productRepository.search(any()) }
    }
  }

  @Test
  fun `on search with complete query`() = runTest {
    val product: ProductModel = ProductModel(id = 111L, name = "Apple")
    coEvery { _productRepository.search(any()) } returns listOf(product)
    _viewModel.onSearch("A")
    advanceUntilIdle()
    assertEquals(
        _viewModel.uiState.safeValue.copy(query = "A", products = listOf(product)),
        _viewModel.uiState.safeValue,
        "Update products based from the queried search result")
  }

  private fun `_on expanded product index changed cases`(): Array<Array<Any>> =
      arrayOf(
          // The updated indexes have +1 offset due to header holder.
          arrayOf(0, 0, listOf(1), -1),
          arrayOf(-1, 0, listOf(1), 0),
          arrayOf(0, 1, listOf(1, 2), 1))

  @ParameterizedTest
  @MethodSource("_on expanded product index changed cases")
  fun `on expanded product index changed`(
      oldIndex: Int,
      newIndex: Int,
      updatedIndexes: List<Int>,
      expandedIndex: Int
  ) {
    _viewModel.onExpandedProductIndexChanged(oldIndex)

    _viewModel.onExpandedProductIndexChanged(newIndex)
    assertAll(
        {
          assertEquals(
              expandedIndex,
              _viewModel.uiState.safeValue.expandedProductIndex,
              "Update expanded product index and reset when selecting the same one")
        },
        {
          assertEquals(
              RecyclerAdapterState.ItemChanged(updatedIndexes),
              _viewModel.uiEvent.safeValue.recyclerAdapter?.data,
              "Notify recycler adapter of item changes")
        })
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on product menu dialog shown`(isShown: Boolean) {
    _viewModel =
        SearchProductViewModel(
            SavedStateHandle().apply {
              set(SearchProductFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key(), true)
              set(SearchProductFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key(), false)
              set(SearchProductFragment.Arguments.INITIAL_QUERY_STRING.key(), "Apple")
              set(
                  SearchProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_IDS_LONG_ARRAY.key(),
                  longArrayOf(111L))
            },
            _dispatcher,
            _productRepository)
    _viewModel.onExpandedProductIndexChanged(0)

    val selectedProduct: ProductModel = ProductModel(id = 111L, name = "Apple")
    if (isShown) _viewModel.onProductMenuDialogShown(selectedProduct)
    else _viewModel.onProductMenuDialogClosed()
    assertEquals(
        SearchProductState(
            isSelectionEnabled = true,
            isToolbarVisible = false,
            initialQuery = "Apple",
            query = "",
            initialSelectedProductIds = listOf(111L),
            products = listOf(),
            expandedProductIndex = 0,
            isProductMenuDialogShown = isShown,
            selectedProductMenu = if (isShown) selectedProduct else null),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on product selected`(isProductNull: Boolean) {
    val product: ProductModel? =
        if (!isProductNull) ProductModel(id = 111L, name = "Apple") else null
    _viewModel.onProductSelected(product)
    assertEquals(
        SearchProductResultState(product?.id),
        _viewModel.uiEvent.safeValue.searchResult?.data,
        "Update result state based from the selected product")
  }

  @Test
  fun `on delete product`() {
    coEvery { _productRepository.delete(any()) } returns 1
    _viewModel.onDeleteProduct(ProductModel(id = 111L, name = "Apple"))
    assertNotNull(
        _viewModel.uiEvent.safeValue.snackbar?.data, "Notify the delete result via snackbar")
  }

  @Test
  fun `on search ui state changed`() {
    val searchState: SearchState =
        SearchState(
            products = listOf(ProductModel(name = "Apple")), customers = listOf(), query = "A")
    _viewModel.onSearchUiStateChanged(searchState)
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            products = searchState.products, query = searchState.query),
        _viewModel.uiState.safeValue,
        "Immediately update current state based from the search's UI state")
  }

  @Test
  fun `on sync product from database`() = runTest {
    val product: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)
    coEvery { _productRepository.search(any()) } returns listOf(product)
    _viewModel.onSearch("Apple")
    advanceUntilIdle()

    val updatedProduct: ProductModel = product.copy(price = product.price + 100L)
    _productChangedListenerCaptor.captured.onModelUpdated(listOf(updatedProduct))
    assertEquals(
        listOf(updatedProduct),
        _viewModel.uiState.safeValue.products,
        "Sync products when any are updated in the database")
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    _productChangedListenerCaptor.captured.onModelAdded(
        listOf(ProductModel(id = 111L, name = "Apple")))
    coEvery { _productRepository.search(any()) } returns listOf()
    _viewModel.onSearch(_viewModel.uiState.safeValue.query)
    advanceUntilIdle()
    _viewModel.onSearchUiStateChanged(SearchState(listOf(), listOf(), ""))
    assertDoesNotThrow("Notify recycler adapter of dataset changes") {
      verify(exactly = 3) {
        _uiEventObserver.onChanged(
            match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
      }
    }
  }
}
