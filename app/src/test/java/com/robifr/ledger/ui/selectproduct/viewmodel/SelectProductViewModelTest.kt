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

package com.robifr.ledger.ui.selectproduct.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductPaginatedInfo
import com.robifr.ledger.local.access.FakeProductDao
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.selectproduct.SelectProductFragment
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
class SelectProductViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _productRepository: ProductRepository
  private lateinit var _productDao: FakeProductDao
  private lateinit var _viewModel: SelectProductViewModel
  private lateinit var _uiEventObserver: Observer<SelectProductEvent>

  private val _firstProduct: ProductModel = ProductModel(id = 111L, name = "Apple")
  private val _secondProduct: ProductModel = ProductModel(id = 222L, name = "Banana")
  private val _thirdProduct: ProductModel = ProductModel(id = 333L, name = "Cherry")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productDao = FakeProductDao(mutableListOf(_firstProduct, _secondProduct, _thirdProduct))
    _productRepository = spyk(ProductRepository(_productDao))
    _uiEventObserver = mockk(relaxed = true)
    _viewModel =
        SelectProductViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            savedStateHandle = SavedStateHandle(),
            _dispatcher = _dispatcher,
            _productRepository = _productRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        SelectProductViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            savedStateHandle =
                SavedStateHandle().apply {
                  set(
                      SelectProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_PARCELABLE.key(),
                      _firstProduct)
                },
            _dispatcher = _dispatcher,
            _productRepository = _productRepository)
    assertEquals(
        SelectProductState(
            initialSelectedProduct = _firstProduct,
            selectedProductOnDatabase = _firstProduct,
            pagination =
                _viewModel.uiState.safeValue.pagination.copy(
                    paginatedItems =
                        listOf(_firstProduct, _secondProduct).map { ProductPaginatedInfo(it) }),
            expandedProductIndex = -1,
            isSelectedProductPreviewExpanded = false),
        _viewModel.uiState.safeValue,
        "Match state with the retrieved data from the fragment argument")
  }

  @Test
  fun `on initialize with unordered name`() {
    _productDao.data.clear()
    _productDao.data.addAll(mutableListOf(_thirdProduct, _firstProduct, _secondProduct))

    _viewModel =
        SelectProductViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            savedStateHandle = SavedStateHandle(),
            _dispatcher = _dispatcher,
            _productRepository = _productRepository)
    assertEquals(
        listOf(_firstProduct, _secondProduct).map { ProductPaginatedInfo(it) },
        _viewModel.uiState.safeValue.pagination.paginatedItems,
        "Sort products based from its name")
  }

  @Test
  fun `on initialize result notify recycler adapter item changes`() {
    assertEquals(
        RecyclerAdapterState.ItemChanged(0),
        _viewModel.uiEvent.safeValue.recyclerAdapter?.data,
        "Notify recycler adapter of header holder changes")
  }

  @Test
  fun `on cleared`() {
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listener from the repository") {
      verify { _productRepository.removeModelChangedListener(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on selected product preview expanded`(isExpanded: Boolean) {
    _viewModel.onSelectedProductPreviewExpanded(isExpanded)
    assertAll(
        {
          assertEquals(
              isExpanded,
              _viewModel.uiState.safeValue.isSelectedProductPreviewExpanded,
              "Update whether selected product preview is expanded")
        },
        {
          assertEquals(
              RecyclerAdapterState.ItemChanged(0),
              _viewModel.uiEvent.safeValue.recyclerAdapter?.data,
              "Notify recycler adapter of header holder changes")
        })
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
  fun `on product selected`(isProductNull: Boolean) {
    val product: ProductPaginatedInfo? =
        if (!isProductNull) ProductPaginatedInfo(_secondProduct) else null
    _viewModel.onProductSelected(product)
    assertEquals(
        SelectProductResultState(product?.id),
        _viewModel.uiEvent.safeValue.selectResult?.data,
        "Update result state based from the selected product")
  }

  @Test
  fun `on sync product from database`() = runTest {
    val updatedProduct: ProductModel = _firstProduct.copy(price = _firstProduct.price + 100L)
    _productRepository.update(updatedProduct)
    assertEquals(
        listOf(updatedProduct, _secondProduct).map { ProductPaginatedInfo(it) },
        _viewModel.uiState.safeValue.pagination.paginatedItems,
        "Sync products when any are updated in the database")
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    clearMocks(_uiEventObserver)
    _productRepository.add(_firstProduct.copy(id = null))
    assertDoesNotThrow("Notify recycler adapter of dataset changes") {
      verify(exactly = 1) {
        _uiEventObserver.onChanged(
            match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
      }
    }
  }
}
