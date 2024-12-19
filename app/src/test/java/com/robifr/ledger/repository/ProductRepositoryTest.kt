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

package com.robifr.ledger.repository

import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.local.access.FakeProductDao
import io.mockk.clearAllMocks
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
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
    assertAll(
        {
          runTest {
            assertEquals(
                insertedId,
                _productRepository.add(_product.copy(id = initialId)),
                "Return the inserted product ID")
          }
        },
        {
          assertDoesNotThrow("Notify added product to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelAdded(
                  listOfNotNull(insertedProduct?.copy(id = insertedId)).ifEmpty { any() })
            }
          }
        })
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
    assertAll(
        {
          runTest {
            assertEquals(
                listOfNotNull(updatedProduct).size,
                _productRepository.update(_product.copy(id = initialId)),
                "Return the number of effected rows")
          }
        },
        {
          assertDoesNotThrow("Notify updated product to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelUpdated(listOfNotNull(updatedProduct).ifEmpty { any() })
            }
          }
        })
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
    assertAll(
        {
          runTest {
            assertEquals(
                listOfNotNull(deletedProduct).size,
                _productRepository.delete(_product.copy(id = initialId)),
                "Return the number of effected rows")
          }
        },
        {
          assertDoesNotThrow("Notify deleted product to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelDeleted(listOfNotNull(deletedProduct).ifEmpty { any() })
            }
          }
        })
  }
}
