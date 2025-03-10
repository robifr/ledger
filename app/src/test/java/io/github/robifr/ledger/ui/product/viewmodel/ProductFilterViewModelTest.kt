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

package io.github.robifr.ledger.ui.product.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.robifr.ledger.InstantTaskExecutorExtension
import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.display.ProductSortMethod
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductPaginatedInfo
import io.github.robifr.ledger.local.access.FakeProductDao
import io.github.robifr.ledger.repository.ProductRepository
import io.mockk.clearAllMocks
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.assertj.core.api.Assertions.assertThat
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
  private lateinit var _productDao: FakeProductDao
  private lateinit var _productViewModel: ProductViewModel
  private lateinit var _viewModel: ProductFilterViewModel

  private val _firstProduct: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)
  private val _secondProduct: ProductModel = ProductModel(id = 222L, name = "Banana", price = 200L)
  private val _thirdProduct: ProductModel = ProductModel(id = 333L, name = "Cherry", price = 300L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productDao = FakeProductDao(mutableListOf(_firstProduct, _secondProduct, _thirdProduct))
    _productRepository = spyk(ProductRepository(_productDao))
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en-US"))

    _productViewModel =
        ProductViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _productRepository = _productRepository)
    _viewModel = _productViewModel.filterView
  }

  @Test
  fun `on state changed`() {
    _viewModel.onDialogShown()
    _viewModel.onMinPriceTextChanged("$0")
    _viewModel.onMaxPriceTextChanged("$1.00")
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve all values except for the changed field")
        .isEqualTo(
            ProductFilterState(
                isDialogShown = true, formattedMinPrice = "$0", formattedMaxPrice = "$1.00"))
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on dialog shown`(isShown: Boolean) {
    _viewModel.onMinPriceTextChanged("$0")
    _viewModel.onMaxPriceTextChanged("$1")

    if (isShown) _viewModel.onDialogShown() else _viewModel.onDialogClosed()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            ProductFilterState(
                isDialogShown = isShown, formattedMinPrice = "$0", formattedMaxPrice = "$1"))
  }

  @Test
  fun `on dialog closed with sorted products`() {
    _productViewModel.onSortMethodChanged(ProductSortMethod(ProductSortMethod.SortBy.PRICE, false))
    _viewModel.onMinPriceTextChanged("$2.00")
    _viewModel.onMaxPriceTextChanged("$3.00")

    _viewModel.onDialogClosed()
    assertThat(_productViewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Apply filter to the products while retaining the sorted list")
        .isEqualTo(listOf(_thirdProduct, _secondProduct).map { ProductPaginatedInfo(it) })
  }

  private fun `_on dialog closed with unbounded price range cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf("$0", "$0", "", "", listOf(_firstProduct, _secondProduct)),
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
    assertThat(_productViewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Include any product whose price falls within the unbounded range")
        .isEqualTo(filteredProducts.map { ProductPaginatedInfo(it) })
  }

  private fun `_on dialog closed with product excluded from previous filter cases`():
      Array<Array<Any>> =
      arrayOf(
          // `_firstProduct` was previously excluded.
          arrayOf("$2.00", "", "", "", listOf(_firstProduct, _secondProduct)),
          // `_firstProduct` was previously excluded, but then exclude `_secondCustomer`.
          arrayOf("$2.00", "", "", "$1.00", listOf(_firstProduct)))

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
    assertThat(_productViewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Include product from the database that match the filter")
        .isEqualTo(filteredProduct.map { ProductPaginatedInfo(it) })
  }
}
