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

package io.github.robifr.ledger.ui.selectproduct.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import io.github.robifr.ledger.InstantTaskExecutorExtension
import io.github.robifr.ledger.LifecycleOwnerExtension
import io.github.robifr.ledger.LifecycleTestOwner
import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductPaginatedInfo
import io.github.robifr.ledger.local.access.FakeProductDao
import io.github.robifr.ledger.onLifecycleOwnerDestroyed
import io.github.robifr.ledger.repository.ProductRepository
import io.github.robifr.ledger.ui.common.state.RecyclerAdapterState
import io.github.robifr.ledger.ui.selectproduct.SelectProductFragment
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
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Match state with the retrieved data from the fragment argument")
        .isEqualTo(
            SelectProductState(
                initialSelectedProduct = _firstProduct,
                selectedProductOnDatabase = _firstProduct,
                pagination =
                    _viewModel.uiState.safeValue.pagination.copy(
                        paginatedItems =
                            listOf(_firstProduct, _secondProduct).map { ProductPaginatedInfo(it) }),
                expandedProductIndex = -1,
                isSelectedProductPreviewExpanded = false))
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
    assertThat(_viewModel.uiState.safeValue.pagination.paginatedItems)
        .describedAs("Sort products based from its name")
        .isEqualTo(listOf(_firstProduct, _secondProduct).map { ProductPaginatedInfo(it) })
  }

  @Test
  fun `on initialize result notify recycler adapter item changes`() {
    assertThat(_viewModel.uiEvent.safeValue.recyclerAdapter?.data)
        .describedAs("Notify recycler adapter of header holder changes")
        .isEqualTo(RecyclerAdapterState.ItemChanged(0))
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
  fun `on selected product preview expanded`(isExpanded: Boolean) {
    _viewModel.onSelectedProductPreviewExpanded(isExpanded)
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.isSelectedProductPreviewExpanded)
          .describedAs("Update whether selected product preview is expanded")
          .isEqualTo(isExpanded)
      it.assertThat(_viewModel.uiEvent.safeValue.recyclerAdapter?.data)
          .describedAs("Notify recycler adapter of header holder changes")
          .isEqualTo(RecyclerAdapterState.ItemChanged(0))
    }
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
  fun `on product selected`(isProductNull: Boolean) {
    val product: ProductPaginatedInfo? =
        if (!isProductNull) ProductPaginatedInfo(_secondProduct) else null
    _viewModel.onProductSelected(product)
    assertThat(_viewModel.uiEvent.safeValue.selectResult?.data)
        .describedAs("Update result state based from the selected product")
        .isEqualTo(SelectProductResultState(product?.id))
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
  fun `on state changed result notify recycler adapter dataset changes`() = runTest {
    clearMocks(_uiEventObserver)
    _productRepository.add(_firstProduct.copy(id = null))
    assertThatCode {
          verify(exactly = 1) {
            _uiEventObserver.onChanged(
                match { it.recyclerAdapter?.data == RecyclerAdapterState.DataSetChanged })
          }
        }
        .describedAs("Notify recycler adapter of dataset changes")
        .doesNotThrowAnyException()
  }
}
