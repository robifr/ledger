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

package io.github.robifr.ledger.repository

import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.local.access.FakeProductDao
import io.mockk.clearAllMocks
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(MainCoroutineExtension::class)
class ProductRepositoryTest {
  private lateinit var _productRepository: ProductRepository
  private lateinit var _localDao: FakeProductDao
  private lateinit var _modelChangedListener: ModelChangedListener<ProductModel>

  private val _product: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _modelChangedListener = spyk()
    _localDao = FakeProductDao(mutableListOf(_product))
    _productRepository = ProductRepository(_localDao)
    _productRepository.addModelChangedListener(_modelChangedListener)
  }

  private fun `_add product cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(null, 222L, _product, 1),
          // Product with the same ID already exist.
          arrayOf(111L, 0L, null, 0))

  @ParameterizedTest
  @MethodSource("_add product cases")
  fun `add product`(
      initialId: Long?,
      insertedId: Long,
      insertedProduct: ProductModel?,
      notifyCount: Int
  ) {
    assertSoftly {
      runTest {
        it.assertThat(_productRepository.add(_product.copy(id = initialId)))
            .describedAs("Return the inserted product ID")
            .isEqualTo(insertedId)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelAdded(
                  listOfNotNull(insertedProduct?.copy(id = insertedId)).ifEmpty { any() })
            }
          }
          .describedAs("Notify added product to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
  }

  private fun `_update product cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, _product, 1),
          // Product with the same ID doesn't exist.
          arrayOf(222L, null, 0),
          arrayOf(null, null, 0))

  @ParameterizedTest
  @MethodSource("_update product cases")
  fun `update product`(initialId: Long?, updatedProduct: ProductModel?, notifyCount: Int) {
    assertSoftly {
      runTest {
        it.assertThat(_productRepository.update(_product.copy(id = initialId)))
            .describedAs("Return the number of effected rows")
            .isEqualTo(listOfNotNull(updatedProduct).size)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelUpdated(listOfNotNull(updatedProduct).ifEmpty { any() })
            }
          }
          .describedAs("Notify updated product to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
  }

  private fun `_delete product cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, _product, 1),
          // Product with the same ID doesn't exist.
          arrayOf(222L, null, 0),
          arrayOf(null, null, 0))

  @ParameterizedTest
  @MethodSource("_delete product cases")
  fun `delete product`(initialId: Long?, deletedProduct: ProductModel?, notifyCount: Int) {
    assertSoftly {
      runTest {
        it.assertThat(_productRepository.delete(initialId))
            .describedAs("Return the number of effected rows")
            .isEqualTo(listOfNotNull(deletedProduct).size)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelDeleted(listOfNotNull(deletedProduct).ifEmpty { any() })
            }
          }
          .describedAs("Notify deleted product to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
  }

  private fun `_search product cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(listOf("Apple", "Banana"), "A", listOf("Apple", "Banana")),
          arrayOf(listOf("Apple", "Banana"), "a", listOf("Apple", "Banana")),
          arrayOf(listOf("Apple", "Banana"), "Apple", listOf("Apple")),
          arrayOf(listOf("Apple", "Banana"), " ", listOf<String>()))

  @ParameterizedTest
  @MethodSource("_search product cases")
  fun `search product`(
      productNamesInDb: List<String>,
      query: String,
      productNamesFound: List<String>
  ) = runTest {
    // Simulate exact amount of the products within the database.
    _localDao.data.clear()
    _localDao.idGenerator.lastIncrement = 0
    productNamesInDb.map { _localDao.data.add(_product.copy(name = it)) }

    assertThat(_productRepository.search(query).map { it.name })
        .describedAs("Search for products whose names contain the query")
        .isEqualTo(productNamesFound)
  }
}
