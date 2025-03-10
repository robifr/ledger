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

package io.github.robifr.ledger.ui.searchproduct.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import io.github.robifr.ledger.InstantTaskExecutorExtension
import io.github.robifr.ledger.LifecycleOwnerExtension
import io.github.robifr.ledger.LifecycleTestOwner
import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.local.access.FakeProductDao
import io.github.robifr.ledger.onLifecycleOwnerDestroyed
import io.github.robifr.ledger.repository.ProductRepository
import io.github.robifr.ledger.ui.common.state.RecyclerAdapterState
import io.github.robifr.ledger.ui.search.viewmodel.SearchState
import io.github.robifr.ledger.ui.searchproduct.SearchProductFragment
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
  private lateinit var _productDao: FakeProductDao
  private lateinit var _viewModel: SearchProductViewModel
  private lateinit var _uiEventObserver: Observer<SearchProductEvent>

  private val _product: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productDao = FakeProductDao(mutableListOf(_product))
    _productRepository = spyk(ProductRepository(_productDao))
    _uiEventObserver = mockk(relaxed = true)
    _viewModel = SearchProductViewModel(SavedStateHandle(), _dispatcher, _productRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on initialize with no arguments`() {
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Apply the default state if no fragment arguments are provided")
        .isEqualTo(
            SearchProductState(
                isSelectionEnabled = false,
                isToolbarVisible = true,
                initialQuery = "",
                query = "",
                initialSelectedProductIds = listOf(),
                products = listOf(),
                expandedProductIndex = -1,
                isProductMenuDialogShown = false,
                selectedProductMenu = null))
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
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Match state with the retrieved data from the fragment arguments")
        .isEqualTo(
            SearchProductState(
                isSelectionEnabled = true,
                isToolbarVisible = false,
                initialQuery = "Apple",
                query = "",
                initialSelectedProductIds = listOf(111L),
                products = listOf(),
                expandedProductIndex = -1,
                isProductMenuDialogShown = false,
                selectedProductMenu = null))
  }

  @Test
  fun `on cleared`() {
    _viewModel.onLifecycleOwnerDestroyed()
    assertThatCode { verify { _productRepository.removeModelChangedListener(any()) } }
        .describedAs("Remove attached listener from the repository")
        .doesNotThrowAnyException()
  }

  @Test
  fun `on search with fast input`() = runTest {
    _viewModel.onSearch("A")
    _viewModel.onSearch("B")
    _viewModel.onSearch("C")
    advanceUntilIdle()
    assertThatCode { coVerify(atMost = 1) { _productRepository.search(any()) } }
        .describedAs("Prevent search from triggering multiple times when typing quickly")
        .doesNotThrowAnyException()
  }

  @Test
  fun `on search with complete query`() = runTest {
    _viewModel.onSearch("A")
    advanceUntilIdle()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Update products based from the queried search result")
        .isEqualTo(_viewModel.uiState.safeValue.copy(query = "A", products = listOf(_product)))
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
  ) = runTest {
    _viewModel.onExpandedProductIndexChanged(oldIndex)
    advanceUntilIdle()

    _viewModel.onExpandedProductIndexChanged(newIndex)
    advanceUntilIdle()
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.expandedProductIndex)
          .describedAs("Update expanded product index and reset when selecting the same one")
          .isEqualTo(expandedIndex)
      it.assertThat(_viewModel.uiEvent.safeValue.recyclerAdapter?.data)
          .describedAs("Notify recycler adapter of item changes")
          .isEqualTo(RecyclerAdapterState.ItemChanged(updatedIndexes))
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on product menu dialog shown`(isShown: Boolean) = runTest {
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
    advanceUntilIdle()

    if (isShown) _viewModel.onProductMenuDialogShown(_product)
    else _viewModel.onProductMenuDialogClosed()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            SearchProductState(
                isSelectionEnabled = true,
                isToolbarVisible = false,
                initialQuery = "Apple",
                query = "",
                initialSelectedProductIds = listOf(111L),
                products = listOf(),
                expandedProductIndex = 0,
                isProductMenuDialogShown = isShown,
                selectedProductMenu = if (isShown) _product else null))
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on product selected`(isProductNull: Boolean) {
    val product: ProductModel? = if (!isProductNull) _product else null
    _viewModel.onProductSelected(product)
    assertThat(_viewModel.uiEvent.safeValue.searchResult?.data)
        .describedAs("Update result state based from the selected product")
        .isEqualTo(SearchProductResultState(product?.id))
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 111L])
  fun `on delete product`(idToDelete: Long) {
    _viewModel.onDeleteProduct(idToDelete)
    assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
        .describedAs("Notify the delete result via snackbar")
        .isNotNull()
  }

  @Test
  fun `on search ui state changed`() {
    val searchState: SearchState =
        SearchState(products = listOf(_product), customers = listOf(), query = "A")
    _viewModel.onSearchUiStateChanged(searchState)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Immediately update current state based from the search's UI state")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                products = searchState.products, query = searchState.query))
  }

  @Test
  fun `on sync product from database`() = runTest {
    _viewModel.onSearch("Apple")
    advanceUntilIdle()

    val updatedProduct: ProductModel = _product.copy(price = _product.price + 100L)
    _productRepository.update(updatedProduct)
    assertThat(_viewModel.uiState.safeValue.products)
        .describedAs("Sync products when any are updated in the database")
        .isEqualTo(listOf(updatedProduct))
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    _productRepository.add(_product.copy(id = null))
    _viewModel.onSearch(_viewModel.uiState.safeValue.query)
    advanceUntilIdle()
    _viewModel.onSearchUiStateChanged(SearchState(listOf(), listOf(), ""))
    assertThatCode {
          verify(exactly = 3) {
            _uiEventObserver.onChanged(
                match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
          }
        }
        .describedAs("Notify recycler adapter of dataset changes")
        .doesNotThrowAnyException()
  }
}
