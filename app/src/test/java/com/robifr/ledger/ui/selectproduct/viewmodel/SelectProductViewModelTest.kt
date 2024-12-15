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

import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.selectproduct.SelectProductFragment
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
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
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class SelectProductViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _productRepository: ProductRepository
  private val _productChangedListenerCaptor: CapturingSlot<ModelSyncListener<ProductModel>> = slot()
  private lateinit var _viewModel: SelectProductViewModel

  private val _firstProduct: ProductModel = ProductModel(id = 111L, name = "Apple")
  private val _secondProduct: ProductModel = ProductModel(id = 222L, name = "Banana")
  private val _thirdProduct: ProductModel = ProductModel(id = 333L, name = "Cherry")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productRepository = mockk()

    every {
      _productRepository.addModelChangedListener(capture(_productChangedListenerCaptor))
    } just Runs
    coEvery { _productRepository.selectAll() } returns
        listOf(_firstProduct, _secondProduct, _thirdProduct)
    coEvery { _productRepository.selectById(any<Long>()) } returns _firstProduct
    _viewModel = SelectProductViewModel(SavedStateHandle(), _dispatcher, _productRepository)
  }

  @Test
  fun `on initialize with arguments`() {
    _viewModel =
        SelectProductViewModel(
            SavedStateHandle().apply {
              set(
                  SelectProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_PARCELABLE.key(),
                  _firstProduct)
            },
            _dispatcher,
            _productRepository)
    assertEquals(
        SelectProductState(
            initialSelectedProduct = _firstProduct,
            selectedProductOnDatabase = _firstProduct,
            products = listOf(_firstProduct, _secondProduct, _thirdProduct),
            expandedProductIndex = -1,
            isSelectedProductPreviewExpanded = false),
        _viewModel.uiState.safeValue,
        "Match state with the retrieved data from the fragment argument")
  }

  @Test
  fun `on initialize with unordered name`() {
    val firstProduct: ProductModel = _firstProduct.copy(name = "Cherry")
    val secondProduct: ProductModel = _secondProduct.copy(name = "Apple")
    val thirdProduct: ProductModel = _thirdProduct.copy(name = "Banana")
    coEvery { _productRepository.selectAll() } returns
        listOf(firstProduct, secondProduct, thirdProduct)
    _viewModel = SelectProductViewModel(SavedStateHandle(), _dispatcher, _productRepository)
    assertEquals(
        listOf(secondProduct, thirdProduct, firstProduct),
        _viewModel.uiState.safeValue.products,
        "Sort products based from its name")
  }

  @Test
  fun `on cleared`() {
    every { _productRepository.removeModelChangedListener(any()) } just Runs
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listener from the repository") {
      verify { _productRepository.removeModelChangedListener(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on selected product preview expanded`(isExpanded: Boolean) {
    _viewModel.onSelectedProductPreviewExpanded(isExpanded)
    assertEquals(
        isExpanded,
        _viewModel.uiState.safeValue.isSelectedProductPreviewExpanded,
        "Update whether selected product preview is expanded")
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
    val product: ProductModel? = if (!isProductNull) _secondProduct else null
    _viewModel.onProductSelected(product)
    assertEquals(
        SelectProductResultState(product?.id),
        _viewModel.resultState.value,
        "Update result state based from the selected product")
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
}
