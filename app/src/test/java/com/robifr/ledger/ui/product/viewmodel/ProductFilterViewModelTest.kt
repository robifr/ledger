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
import com.robifr.ledger.repository.ProductRepository
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class ProductFilterViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _productRepository: ProductRepository
  private lateinit var _productViewModel: ProductViewModel
  private lateinit var _viewModel: ProductFilterViewModel

  private val _firstProduct: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)
  private val _secondProduct: ProductModel = ProductModel(id = 222L, name = "Banana", price = 200L)
  private val _thirdProduct: ProductModel = ProductModel(id = 333L, name = "Cherry", price = 300L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productRepository = mockk()

    every { _productRepository.addModelChangedListener(any()) } just Runs
    coEvery { _productRepository.selectAll() } returns
        listOf(_firstProduct, _secondProduct, _thirdProduct)
    coEvery { _productRepository.isTableEmpty() } returns false
    _productViewModel = ProductViewModel(_dispatcher, _productRepository)
    _viewModel = _productViewModel.filterView
  }

  @Test
  fun `on state changed`() {
    _viewModel.onDialogShown()
    _viewModel.onMinPriceTextChanged("$0")
    _viewModel.onMaxPriceTextChanged("$1.00")
    assertEquals(
        ProductFilterState(
            isDialogShown = true, formattedMinPrice = "$0", formattedMaxPrice = "$1.00"),
        _viewModel.uiState.safeValue,
        "Preserve all values except for the changed field")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on dialog shown`(isShown: Boolean) {
    _viewModel.onMinPriceTextChanged("$0")
    _viewModel.onMaxPriceTextChanged("$1")

    if (isShown) _viewModel.onDialogShown() else _viewModel.onDialogClosed()
    assertEquals(
        ProductFilterState(
            isDialogShown = isShown, formattedMinPrice = "$0", formattedMaxPrice = "$1"),
        _viewModel.uiState.safeValue,
        "Preserve other fields when the dialog shown or closed")
  }

  @Test
  fun `on dialog closed with sorted products`() {
    _productViewModel.onSortMethodChanged(ProductSortMethod(ProductSortMethod.SortBy.PRICE, false))
    _viewModel.onMinPriceTextChanged("$2.00")
    _viewModel.onMaxPriceTextChanged("$3.00")

    _viewModel.onDialogClosed()
    assertEquals(
        listOf(_thirdProduct, _secondProduct),
        _productViewModel.uiState.safeValue.products,
        "Apply filter to the products while retaining the sorted list")
  }

  private fun `_on dialog closed with unbounded price range cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf("$0", "$0", "", "", listOf(_firstProduct, _secondProduct, _thirdProduct)),
          arrayOf("$0", "$0", "$2.00", "", listOf(_secondProduct, _thirdProduct)),
          arrayOf("$0", "$0", "", "$2.00", listOf(_firstProduct, _secondProduct)))

  @ParameterizedTest
  @MethodSource("_on dialog closed with unbounded price range cases")
  fun `on dialog closed with unbounded price range`(
      oldFormattedMinPrice: String,
      oldFormattedMaxPrice: String,
      newFormattedMinPrice: String,
      newFormattedMaxPrice: String,
      filteredProducts: List<ProductModel>
  ) {
    _viewModel.onMinPriceTextChanged(oldFormattedMinPrice)
    _viewModel.onMaxPriceTextChanged(oldFormattedMaxPrice)
    _viewModel.onDialogClosed()

    _viewModel.onMinPriceTextChanged(newFormattedMinPrice)
    _viewModel.onMaxPriceTextChanged(newFormattedMaxPrice)

    _viewModel.onDialogClosed()
    assertEquals(
        filteredProducts,
        _productViewModel.uiState.safeValue.products,
        "Include any product whose price falls within the unbounded range")
  }

  private fun `_on dialog closed with product excluded from previous filter cases`():
      Array<Array<Any>> =
      arrayOf(
          // `_firstProduct` was previously excluded.
          arrayOf("$2.00", "", "", "", listOf(_firstProduct, _secondProduct, _thirdProduct)),
          // `_firstProduct` was previously excluded, but then exclude `_thirdProduct`.
          arrayOf("$2.00", "", "", "$2.00", listOf(_firstProduct, _secondProduct)))

  @ParameterizedTest
  @MethodSource("_on dialog closed with product excluded from previous filter cases")
  fun `on dialog closed with product excluded from previous filter`(
      oldFormattedMinPrice: String,
      oldFormattedMaxPrice: String,
      newFormattedMinPrice: String,
      newFormattedMaxPrice: String,
      filteredProduct: List<ProductModel>
  ) {
    _viewModel.onMinPriceTextChanged(oldFormattedMinPrice)
    _viewModel.onMaxPriceTextChanged(oldFormattedMaxPrice)
    _viewModel.onDialogClosed()

    _viewModel.onMinPriceTextChanged(newFormattedMinPrice)
    _viewModel.onMaxPriceTextChanged(newFormattedMaxPrice)

    _viewModel.onDialogClosed()
    assertEquals(
        filteredProduct,
        _productViewModel.uiState.safeValue.products,
        "Include product from the database that match the filter")
  }
}
