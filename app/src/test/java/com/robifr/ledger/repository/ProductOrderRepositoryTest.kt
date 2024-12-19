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
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.local.access.FakeProductOrderDao
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
class ProductOrderRepositoryTest {
  private lateinit var _productOrderRepository: ProductOrderRepository
  private lateinit var _localDao: FakeProductOrderDao
  private lateinit var _modelChangedListener: ModelChangedListener<ProductOrderModel>

  private val _productOrder: ProductOrderModel =
      ProductOrderModel(
          id = 111L,
          queueId = 111L,
          productId = 111L,
          productName = "Apple",
          productPrice = 100L,
          quantity = 1.0,
          discount = 0L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _modelChangedListener = spyk()
    _localDao = FakeProductOrderDao(mutableListOf(_productOrder))
    _productOrderRepository = ProductOrderRepository(_localDao)
    _productOrderRepository.addModelChangedListener(_modelChangedListener)
  }

  private fun `_add product order cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(null, 222L, _productOrder, true, 1),
          arrayOf(null, 222L, _productOrder, false, 1),
          // Product order with the same ID already exist.
          arrayOf(111L, null, null, true, 0),
          arrayOf(111L, 0L, null, false, 0))

  @ParameterizedTest
  @MethodSource("_add product order cases")
  fun `add product order`(
      initialId: Long?,
      insertedId: Long?,
      insertedProductOrder: ProductOrderModel?,
      isUsingList: Boolean,
      notifyCount: Int
  ) {
    assertAll(
        {
          runTest {
            assertEquals(
                if (isUsingList) listOfNotNull(insertedId) else insertedId,
                if (isUsingList) {
                  _productOrderRepository.add(listOf(_productOrder.copy(id = initialId)))
                } else {
                  _productOrderRepository.add(_productOrder.copy(id = initialId))
                },
                "Return the inserted product order ID")
          }
        },
        {
          assertDoesNotThrow("Notify added product order to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelAdded(
                  listOfNotNull(insertedProductOrder?.copy(id = insertedId)).ifEmpty { any() })
            }
          }
        })
  }

  private fun `_update product order cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, _productOrder, true, 1),
          arrayOf(111L, _productOrder, false, 1),
          // Product order with the same ID doesn't exist.
          arrayOf(222L, null, true, 0),
          arrayOf(222L, null, false, 0),
          arrayOf(null, null, true, 0),
          arrayOf(null, null, false, 0))

  @ParameterizedTest
  @MethodSource("_update product order cases")
  fun `update product order`(
      initialId: Long?,
      updatedProductOrder: ProductOrderModel?,
      isUsingList: Boolean,
      notifyCount: Int
  ) {
    assertAll(
        {
          runTest {
            assertEquals(
                listOfNotNull(updatedProductOrder).size,
                if (isUsingList) {
                  _productOrderRepository.update(listOf(_productOrder.copy(id = initialId)))
                } else {
                  _productOrderRepository.update(_productOrder.copy(id = initialId))
                },
                "Return the number of effected rows")
          }
        },
        {
          assertDoesNotThrow("Notify updated product order to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelUpdated(
                  listOfNotNull(updatedProductOrder).ifEmpty { any() })
            }
          }
        })
  }

  private fun `_delete product order cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, _productOrder, true, 1),
          arrayOf(111L, _productOrder, false, 1),
          // Product order with the same ID doesn't exist.
          arrayOf(222L, null, true, 0),
          arrayOf(222L, null, false, 0),
          arrayOf(null, null, true, 0),
          arrayOf(null, null, false, 0))

  @ParameterizedTest
  @MethodSource("_delete product order cases")
  fun `delete product order`(
      initialId: Long?,
      deletedProductOrder: ProductOrderModel?,
      isUsingList: Boolean,
      notifyCount: Int
  ) {
    assertAll(
        {
          runTest {
            assertEquals(
                listOfNotNull(deletedProductOrder).size,
                if (isUsingList) {
                  _productOrderRepository.delete(listOf(_productOrder.copy(id = initialId)))
                } else {
                  _productOrderRepository.delete(_productOrder.copy(id = initialId))
                },
                "Return the number of effected rows")
          }
        },
        {
          assertDoesNotThrow("Notify deleted product order to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelDeleted(
                  listOfNotNull(deletedProductOrder).ifEmpty { any() })
            }
          }
        })
  }

  private fun `_upsert product order cases`(): Array<Array<Any?>> =
      arrayOf(
          // Update when the product order with the same ID already exist.
          arrayOf(111L, 111L, _productOrder, true, 1),
          arrayOf(111L, 111L, _productOrder, false, 1),
          // Insert when the product order with the same ID doesn't exist.
          // There's no way for the query to fail.
          arrayOf(null, 222L, _productOrder, true, 1),
          arrayOf(null, 222L, _productOrder, false, 1))

  @ParameterizedTest
  @MethodSource("_upsert product order cases")
  fun `upsert product order`(
      initialId: Long?,
      upsertedId: Long,
      upsertedProductOrder: ProductOrderModel,
      isUsingList: Boolean,
      notifyCount: Int
  ) {
    assertAll(
        {
          runTest {
            assertEquals(
                if (isUsingList) listOfNotNull(upsertedId) else upsertedId,
                if (isUsingList) {
                  _productOrderRepository.upsert(listOf(_productOrder.copy(id = initialId)))
                } else {
                  _productOrderRepository.upsert(_productOrder.copy(id = initialId))
                },
                "Return the upserted product order row ID")
          }
        },
        {
          assertDoesNotThrow("Notify upserted product order to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelUpserted(
                  listOfNotNull(upsertedProductOrder.copy(id = upsertedId)).ifEmpty { any() })
            }
          }
        })
  }
}
