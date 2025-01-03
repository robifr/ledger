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

import androidx.lifecycle.Observer
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.display.ProductSortMethod
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.SnackbarState
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(
    InstantTaskExecutorExtension::class,
    MainCoroutineExtension::class,
    LifecycleOwnerExtension::class)
class ProductViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _productRepository: ProductRepository
  private val _productChangedListenerCaptor: CapturingSlot<ModelSyncListener<ProductModel>> = slot()
  private lateinit var _viewModel: ProductViewModel
  private lateinit var _snackbarStateObserver: Observer<SnackbarState>
  private lateinit var _recyclerAdapterStateObserver: Observer<RecyclerAdapterState>

  private val _firstProduct: ProductModel = ProductModel(id = 111L, name = "Apple", price = 200L)
  private val _secondProduct: ProductModel = ProductModel(id = 222L, name = "Banana", price = 300L)
  private val _thirdProduct: ProductModel = ProductModel(id = 333L, name = "Cherry", price = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productRepository = mockk()
    _snackbarStateObserver = mockk(relaxed = true)
    _recyclerAdapterStateObserver = mockk(relaxed = true)

    every {
      _productRepository.addModelChangedListener(capture(_productChangedListenerCaptor))
    } just Runs
    coEvery { _productRepository.selectAll() } returns
        listOf(_firstProduct, _secondProduct, _thirdProduct)
    _viewModel = ProductViewModel(_dispatcher, _productRepository)
    _viewModel.snackbarState.observe(_lifecycleOwner, _snackbarStateObserver)
    _viewModel.recyclerAdapterState.observe(_lifecycleOwner, _recyclerAdapterStateObserver)
  }

  @Test
  fun `on initialize with unordered name`() {
    coEvery { _productRepository.selectAll() } returns
        listOf(_thirdProduct, _firstProduct, _secondProduct)
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
          assertDoesNotThrow("Notify recycler adapter of item changes") {
            verify {
              _recyclerAdapterStateObserver.onChanged(
                  eq(RecyclerAdapterState.ItemChanged(updatedIndexes)))
            }
          }
        })
  }

  @Test
  fun `on delete product`() {
    coEvery { _productRepository.delete(any()) } returns 1
    _viewModel.onDeleteProduct(_firstProduct)
    assertDoesNotThrow("Notify the delete result via snackbar") {
      verify { _snackbarStateObserver.onChanged(any()) }
    }
  }

  @Test
  fun `on sync product from database`() {
    val updatedProducts: List<ProductModel> =
        listOf(
            _firstProduct.copy(price = _firstProduct.price + 100L), _secondProduct, _thirdProduct)
    _productChangedListenerCaptor.captured.onModelUpdated(updatedProducts)
    assertEquals(
        updatedProducts,
        _viewModel.uiState.safeValue.products,
        "Sync products when any are updated in the database")
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() {
    clearMocks(_recyclerAdapterStateObserver)
    _productChangedListenerCaptor.captured.onModelAdded(listOf(_firstProduct))
    _viewModel.onProductsChanged(_viewModel.uiState.safeValue.products)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod.sortBy)
    assertDoesNotThrow("Notify recycler adapter of dataset changes") {
      verify(exactly = 4) {
        _recyclerAdapterStateObserver.onChanged(eq(RecyclerAdapterState.DataSetChanged))
      }
    }
  }
}
