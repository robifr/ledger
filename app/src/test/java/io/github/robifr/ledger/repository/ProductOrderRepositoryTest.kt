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
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.local.access.FakeProductOrderDao
import io.mockk.clearAllMocks
import io.mockk.spyk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
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
  private val _queue: QueueModel =
      QueueModel(
          id = 111L,
          customerId = null,
          customer = null,
          status = QueueModel.Status.COMPLETED,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE,
          productOrders = listOf(_productOrder))

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _modelChangedListener = spyk()
    _localDao =
        FakeProductOrderDao(data = mutableListOf(_productOrder), queueData = mutableListOf(_queue))
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
    assertSoftly {
      runTest {
        it.assertThat(
                if (isUsingList) {
                  _productOrderRepository.add(listOf(_productOrder.copy(id = initialId)))
                } else {
                  _productOrderRepository.add(_productOrder.copy(id = initialId))
                })
            .describedAs("Return the inserted product order ID")
            .isEqualTo(if (isUsingList) listOfNotNull(insertedId) else insertedId)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelAdded(
                  listOfNotNull(insertedProductOrder?.copy(id = insertedId)).ifEmpty { any() })
            }
          }
          .describedAs("Notify added product order to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
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
    assertSoftly {
      runTest {
        it.assertThat(
                if (isUsingList) {
                  _productOrderRepository.update(listOf(_productOrder.copy(id = initialId)))
                } else {
                  _productOrderRepository.update(_productOrder.copy(id = initialId))
                })
            .describedAs("Return the number of effected rows")
            .isEqualTo(listOfNotNull(updatedProductOrder).size)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelUpdated(
                  listOfNotNull(updatedProductOrder).ifEmpty { any() })
            }
          }
          .describedAs("Notify updated product order to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
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
    assertSoftly {
      runTest {
        it.assertThat(
                if (isUsingList) {
                  _productOrderRepository.delete(listOf(_productOrder.copy(id = initialId)))
                } else {
                  _productOrderRepository.delete(initialId)
                })
            .describedAs("Return the number of effected rows")
            .isEqualTo(listOfNotNull(deletedProductOrder).size)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelDeleted(
                  listOfNotNull(deletedProductOrder).ifEmpty { any() })
            }
          }
          .describedAs("Notify deleted product order to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
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
    assertSoftly {
      runTest {
        it.assertThat(
                if (isUsingList) {
                  _productOrderRepository.upsert(listOf(_productOrder.copy(id = initialId)))
                } else {
                  _productOrderRepository.upsert(_productOrder.copy(id = initialId))
                })
            .describedAs("Return the upserted product order ID")
            .isEqualTo(if (isUsingList) listOfNotNull(upsertedId) else upsertedId)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelUpserted(
                  listOfNotNull(upsertedProductOrder.copy(id = upsertedId)).ifEmpty { any() })
            }
          }
          .describedAs("Notify upserted product order to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
  }
}
