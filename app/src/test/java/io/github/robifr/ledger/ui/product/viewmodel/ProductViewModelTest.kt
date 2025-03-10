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

import androidx.lifecycle.Observer
import io.github.robifr.ledger.InstantTaskExecutorExtension
import io.github.robifr.ledger.LifecycleOwnerExtension
import io.github.robifr.ledger.LifecycleTestOwner
import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.display.ProductSortMethod
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductPaginatedInfo
import io.github.robifr.ledger.local.access.FakeProductDao
import io.github.robifr.ledger.onLifecycleOwnerDestroyed
import io.github.robifr.ledger.repository.ProductRepository
import io.github.robifr.ledger.ui.common.state.RecyclerAdapterState
import io.mockk.clearAllMocks
import io.mockk.clearMocks
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
class ProductViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _productRepository: ProductRepository
  private lateinit var _productDao: FakeProductDao
  private lateinit var _viewModel: ProductViewModel
  private lateinit var _uiEventObserver: Observer<ProductEvent>

  private val _firstProduct: ProductModel = ProductModel(id = 111L, name = "Apple", price = 200L)
  private val _secondProduct: ProductModel = ProductModel(id = 222L, name = "Banana", price = 300L)
  private val _thirdProduct: ProductModel = ProductModel(id = 333L, name = "Cherry", price = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productDao = FakeProductDao(mutableListOf(_firstProduct, _secondProduct, _thirdProduct))
    _productRepository = spyk(ProductRepository(_productDao))
    _uiEventObserver = mockk(relaxed = true)
    _viewModel =
        ProductViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _productRepository = _productRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on initialize with empty data`(isTableEmpty: Boolean) {
    _productDao.data.clear()
    if (!isTableEmpty) _productDao.data.add(_firstProduct)

    _viewModel =
        ProductViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _productRepository = _productRepository)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Show illustration for no products added")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            if (isTableEmpty) listOf()
                            else listOf(ProductPaginatedInfo(_firstProduct))),
                isNoProductsAddedIllustrationVisible = isTableEmpty))
  }

  @Test
  fun `on initialize with unordered name`() {
    _productDao.data.clear()
    _productDao.data.addAll(mutableListOf(_thirdProduct, _secondProduct, _firstProduct))

    _viewModel =
        ProductViewModel(
            maxPaginatedItemPerPage = 2,
            maxPaginatedItemInMemory = 2,
            _dispatcher = _dispatcher,
            _productRepository = _productRepository)
    assertThat(_viewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Sort products based from the default sort method")
        .isEqualTo(listOf(_firstProduct, _secondProduct).map { ProductPaginatedInfo(it) })
  }

  @Test
  fun `on cleared`() {
    _viewModel.onLifecycleOwnerDestroyed()
    assertThatCode { verify { _productRepository.removeModelChangedListener(any()) } }
        .describedAs("Remove attached listener from the repository")
        .doesNotThrowAnyException()
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on product menu dialog shown`(isShown: Boolean) = runTest {
    _viewModel.onExpandedProductIndexChanged(0)
    advanceUntilIdle()
    _viewModel.onSortMethodChanged(ProductSortMethod(ProductSortMethod.SortBy.NAME, false))
    _viewModel.onSortMethodDialogClosed()

    if (isShown) _viewModel.onProductMenuDialogShown(ProductPaginatedInfo(_firstProduct))
    else _viewModel.onProductMenuDialogClosed()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            ProductState(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdProduct, _secondProduct).map { ProductPaginatedInfo(it) }),
                expandedProductIndex = 0,
                isProductMenuDialogShown = isShown,
                selectedProductMenu = if (isShown) ProductPaginatedInfo(_firstProduct) else null,
                isNoProductsAddedIllustrationVisible = false,
                sortMethod = ProductSortMethod(ProductSortMethod.SortBy.NAME, false),
                isSortMethodDialogShown = false))
  }

  @Test
  fun `on sort method changed with different sort method`() {
    val sortMethod: ProductSortMethod = ProductSortMethod(ProductSortMethod.SortBy.PRICE, true)
    _viewModel.onSortMethodChanged(sortMethod)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Sort products based from the sorting method")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdProduct, _firstProduct).map { ProductPaginatedInfo(it) }),
                sortMethod = sortMethod))
  }

  @Test
  fun `on sort method changed with same sort`() {
    _viewModel.onSortMethodChanged(ProductSortMethod.SortBy.NAME)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Reverse sort order when selecting the same sort option")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdProduct, _secondProduct).map { ProductPaginatedInfo(it) }),
                sortMethod = ProductSortMethod(ProductSortMethod.SortBy.NAME, false)))
  }

  @Test
  fun `on sort method changed with different sort`() {
    _viewModel.onSortMethodChanged(ProductSortMethod.SortBy.PRICE)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Sort products based from the sorting method")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdProduct, _firstProduct).map { ProductPaginatedInfo(it) }),
                sortMethod = ProductSortMethod(ProductSortMethod.SortBy.PRICE, true)))
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on sort method dialog shown`(isShown: Boolean) = runTest {
    _viewModel.onExpandedProductIndexChanged(0)
    advanceUntilIdle()
    _viewModel.onProductMenuDialogClosed()
    _viewModel.onSortMethodChanged(ProductSortMethod(ProductSortMethod.SortBy.NAME, false))

    if (isShown) _viewModel.onSortMethodDialogShown() else _viewModel.onSortMethodDialogClosed()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve other fields when the dialog shown or closed")
        .isEqualTo(
            ProductState(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_thirdProduct, _secondProduct).map { ProductPaginatedInfo(it) }),
                expandedProductIndex = 0,
                isProductMenuDialogShown = false,
                selectedProductMenu = null,
                isNoProductsAddedIllustrationVisible = false,
                sortMethod = ProductSortMethod(ProductSortMethod.SortBy.NAME, false),
                isSortMethodDialogShown = isShown))
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
  @ValueSource(longs = [0L, 111L])
  fun `on delete product`(idToDelete: Long) {
    _viewModel.onDeleteProduct(idToDelete)
    assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
        .describedAs("Notify the delete result via snackbar")
        .isNotNull()
  }

  @Test
  fun `on sync product from database`() = runTest {
    val updatedProduct: ProductModel = _firstProduct.copy(price = _firstProduct.price + 100L)
    _productRepository.update(updatedProduct)
    assertThat(_viewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Sync products when any are updated in the database")
        .isEqualTo(listOf(updatedProduct, _secondProduct).map { ProductPaginatedInfo(it) })
  }

  @Test
  fun `on sync product from database result empty data`() = runTest {
    _productRepository.delete(_firstProduct.id)
    _productRepository.delete(_secondProduct.id)
    _productRepository.delete(_thirdProduct.id)
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Show illustration for no products created")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(paginatedItems = listOf()),
                isNoProductsAddedIllustrationVisible = true))
  }

  @Test
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    clearMocks(_uiEventObserver)
    _productRepository.add(_firstProduct.copy(id = null))
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod)
    _viewModel.onSortMethodChanged(_viewModel.uiState.safeValue.sortMethod.sortBy)
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
