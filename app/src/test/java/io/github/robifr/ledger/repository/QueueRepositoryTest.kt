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
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.local.TransactionProvider
import io.github.robifr.ledger.local.access.FakeCustomerDao
import io.github.robifr.ledger.local.access.FakeProductOrderDao
import io.github.robifr.ledger.local.access.FakeQueueDao
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(MainCoroutineExtension::class)
class QueueRepositoryTest {
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _localDao: FakeQueueDao
  private lateinit var _transactionProvider: TransactionProvider
  private val _withTransactionCaptor: CapturingSlot<suspend () -> Any> = slot()
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _customerDao: FakeCustomerDao
  private lateinit var _productOrderRepository: ProductOrderRepository
  private lateinit var _productOrderDao: FakeProductOrderDao
  private lateinit var _modelChangedListener: ModelChangedListener<QueueModel>

  private val _customer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 500L)
  private val _productOrder: ProductOrderModel =
      ProductOrderModel(
          id = 111L,
          queueId = 111L,
          productId = 111L,
          productName = "Apple",
          productPrice = 100L,
          quantity = 1.0)
  private val _queue: QueueModel =
      QueueModel(
          id = 111L,
          customerId = _customer.id,
          customer = _customer,
          status = QueueModel.Status.COMPLETED,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE,
          productOrders = listOf(_productOrder))

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _transactionProvider = mockk()
    _modelChangedListener = spyk()
    _localDao = FakeQueueDao(mutableListOf(_queue))
    _productOrderDao =
        FakeProductOrderDao(data = mutableListOf(_productOrder), queueData = mutableListOf(_queue))
    _productOrderRepository = ProductOrderRepository(_productOrderDao)
    _customerDao =
        FakeCustomerDao(
            data = mutableListOf(_customer),
            queueData = _localDao.data,
            productOrderData = _productOrderDao.data)
    _customerRepository = CustomerRepository(_customerDao)

    coEvery { _transactionProvider.withTransaction(capture(_withTransactionCaptor)) } coAnswers
        {
          _withTransactionCaptor.captured.invoke()
        }
    _queueRepository =
        QueueRepository(
            _localDao = _localDao,
            _transactionProvider = _transactionProvider,
            _customerRepository = _customerRepository,
            _productOrderRepository = _productOrderRepository)
    _queueRepository.addModelChangedListener(_modelChangedListener)
  }

  @Test
  fun `select query result mapped queue`() {
    // Simulate current queue in the database with unmapped property.
    _localDao.data[0] = _queue.copy(customer = null, productOrders = listOf())
    assertSoftly {
      val message: String = "Map every queue property that doesn't belong to the database table"
      runTest {
        it.assertThat(_queueRepository.selectAll()).describedAs(message).isEqualTo(listOf(_queue))
        it.assertThat(_queueRepository.selectById(_queue.id)).describedAs(message).isEqualTo(_queue)
        it.assertThat(_queueRepository.selectById(listOfNotNull(_queue.id)))
            .describedAs(message)
            .isEqualTo(listOf(_queue))
      }
    }
  }

  private fun `_add queue cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(null, 222L, _queue, 1),
          // Queue with the same ID already exist.
          arrayOf(111L, 0L, null, 0))

  @ParameterizedTest
  @MethodSource("_add queue cases")
  fun `add queue`(
      initialId: Long?,
      insertedId: Long,
      insertedQueue: QueueModel?,
      notifyCount: Int
  ) {
    assertSoftly {
      runTest {
        it.assertThat(
                _queueRepository.add(
                    _queue.copy(
                        id = initialId,
                        // Ignore foreign columns, they're tested in different test method.
                        customerId = null,
                        customer = null,
                        productOrders = listOf())))
            .describedAs("Return the inserted queue ID")
            .isEqualTo(insertedId)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelAdded(
                  listOfNotNull(
                          insertedQueue?.copy(
                              id = insertedId,
                              customerId = null,
                              customer = null,
                              productOrders = listOf()))
                      .ifEmpty { any() })
            }
          }
          .describedAs("Notify added queue to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
  }

  private fun `_add queue result product orders added cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(null, 222L, _productOrder, 1),
          // Queue with the same ID already exist.
          arrayOf(111L, 0L, null, -1))

  @ParameterizedTest
  @MethodSource("_add queue result product orders added cases")
  fun `add queue result product orders added`(
      initialId: Long?,
      insertedId: Long,
      addedProductOrder: ProductOrderModel?,
      addedProductOrderIndex: Int
  ) = runTest {
    _queueRepository.add(
        _queue.copy(
            id = initialId,
            productOrders = listOf(_productOrder.copy(id = initialId, queueId = initialId))))
    assertThat(_productOrderDao.data.getOrNull(addedProductOrderIndex))
        .describedAs("Add product orders mapped with the queue ID to the database")
        .isEqualTo(addedProductOrder?.copy(id = insertedId, queueId = insertedId))
  }

  private fun `_add queue result customer updated cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(null, _customer, 0),
          // Queue with the same ID already exist.
          arrayOf(111L, null, -1))

  @ParameterizedTest
  @MethodSource("_add queue result customer updated cases")
  fun `add queue result customer updated`(
      initialId: Long?,
      updatedCustomer: CustomerModel?,
      updatedCustomerIndex: Int
  ) = runTest {
    val productOrderToInsert: ProductOrderModel =
        _productOrder.copy(id = initialId, queueId = initialId)
    _queueRepository.add(
        _queue.copy(
            id = initialId,
            // Customer should pay by the amount of the product orders.
            productOrders = listOf(productOrderToInsert)))
    assertThat(_customerDao.data.getOrNull(updatedCustomerIndex))
        .describedAs("Deduct customer balance via `CustomerModel.balanceOnMadePayment()`")
        .isEqualTo(
            updatedCustomer?.copy(
                balance = updatedCustomer.balance - productOrderToInsert.totalPrice.toLong()))
  }

  private fun `_update queue cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, _queue, 1),
          // Queue with the same ID doesn't exist.
          arrayOf(222L, null, 0),
          arrayOf(null, null, 0))

  @ParameterizedTest
  @MethodSource("_update queue cases")
  fun `update queue`(initialId: Long?, updatedQueue: QueueModel?, notifyCount: Int) {
    assertSoftly {
      runTest {
        it.assertThat(_queueRepository.update(_queue.copy(id = initialId)))
            .describedAs("Return the number of effected rows")
            .isEqualTo(listOfNotNull(updatedQueue).size)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelUpdated(
                  listOfNotNull(updatedQueue?.copy(id = initialId)).ifEmpty { any() })
            }
          }
          .describedAs("Notify updated queue to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
  }

  private fun `_update queue result product orders updated cases`(): Array<Array<Any?>> =
      arrayOf(
          // ID 111 updated, ID 222 deleted, and ID 333 (from null) inserted.
          arrayOf(111L, listOf(111L, 222L), listOf(111L, null), listOf(111L, 333L)),
          // Queue with the same ID doesn't exist.
          arrayOf(null, listOf(111L, 222L), listOf(111L, null), listOf(111L, 222L)))

  @ParameterizedTest
  @MethodSource("_update queue result product orders updated cases")
  fun `update queue result product orders updated`(
      initialId: Long?,
      oldProductOrderIdsInDb: List<Long>,
      referencedProductOrderIds: List<Long?>,
      updatedProductOrderIdsInDb: List<Long>
  ) = runTest {
    // Simulate exact amount of the old product orders within the database.
    _productOrderDao.data.clear()
    _productOrderDao.idGenerator.lastIncrement = oldProductOrderIdsInDb.size
    oldProductOrderIdsInDb.forEach { _productOrderDao.data.add(_productOrder.copy(id = it)) }

    _queueRepository.update(
        _queue.copy(
            id = initialId,
            productOrders = referencedProductOrderIds.map { _productOrder.copy(id = it) }))
    assertThat(_productOrderDao.data)
        .describedAs("Upsert the new or existing product orders and delete the ones removed")
        .isEqualTo(updatedProductOrderIdsInDb.map { _productOrder.copy(id = it) })
  }

  private fun `_update queue result customer updated cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, listOf(111L, 222L), 111L, 111L, listOf(111L to 500L, 222L to 500L)),
          arrayOf(111L, listOf(111L, 222L), 111L, 222L, listOf(111L to 600L, 222L to 400L)),
          arrayOf(111L, listOf(111L, 222L), 111L, null, listOf(111L to 600L, 222L to 500L)),
          arrayOf(111L, listOf(111L, 222L), null, 111L, listOf(111L to 400L, 222L to 500L)),
          arrayOf(111L, listOf(111L, 222L), null, 222L, listOf(111L to 500L, 222L to 400L)),
          arrayOf(111L, listOf(111L, 222L), null, null, listOf(111L to 500L, 222L to 500L)),
          // Queue with the same ID doesn't exist.
          arrayOf(null, listOf(111L, 222L), 111L, 111L, listOf(111L to 500L, 222L to 500L)),
          arrayOf(null, listOf(111L, 222L), 111L, 222L, listOf(111L to 500L, 222L to 500L)))

  @ParameterizedTest
  @MethodSource("_update queue result customer updated cases")
  fun `update queue result customer updated`(
      initialId: Long?,
      customerIdsInDb: List<Long>,
      oldReferencedCustomerId: Long?,
      newReferencedCustomerId: Long?,
      updatedCustomerIdAndBalanceInDb: List<Pair<Long, Long>>
  ) = runTest {
    // Simulate exact amount of the customers within the database.
    _customerDao.data.clear()
    _customerDao.idGenerator.lastIncrement = customerIdsInDb.size
    customerIdsInDb.map { _customerDao.data.add(_customer.copy(id = it)) }
    // Simulate the old queue in the database.
    _localDao.data[0] =
        _queue.copy(
            customerId = oldReferencedCustomerId,
            customer = _customer.copy(id = oldReferencedCustomerId))

    _queueRepository.update(
        _queue.copy(
            id = initialId,
            customerId = newReferencedCustomerId,
            customer = _customer.copy(id = newReferencedCustomerId)))
    assertThat(_customerDao.data)
        .describedAs(
            "Update old customer balance via `CustomerBalance.balanceOnUpdatedPayment()` and " +
                "current referenced customer balance via `CustomerBalance.balanceOnRevertedPayment()`")
        .isEqualTo(
            updatedCustomerIdAndBalanceInDb.map {
              _customer.copy(id = it.first, balance = it.second)
            })
  }

  fun `_delete queue cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, _queue, 1),
          // Queue with the same ID doesn't exist.
          arrayOf(222L, null, 0),
          arrayOf(null, null, 0))

  @ParameterizedTest
  @MethodSource("_delete queue cases")
  fun `delete queue`(initialId: Long?, deletedQueue: QueueModel?, notifyCount: Int) {
    assertSoftly {
      runTest {
        it.assertThat(_queueRepository.delete(initialId))
            .describedAs("Return the number of effected rows")
            .isEqualTo(listOfNotNull(deletedQueue).size)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelDeleted(listOfNotNull(deletedQueue).ifEmpty { any() })
            }
          }
          .describedAs("Notify deleted queue to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
  }

  fun `_delete queue result customer updated cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, _customer, 0),
          // Queue with the same ID doesn't exist.
          arrayOf(null, null, -1))

  @ParameterizedTest
  @MethodSource("_delete queue result customer updated cases")
  fun `delete queue result customer updated`(
      initialId: Long?,
      updatedCustomer: CustomerModel?,
      updatedCustomerIndex: Int
  ) = runTest {
    _queueRepository.delete(initialId)
    assertThat(_customerDao.data.getOrNull(updatedCustomerIndex))
        .describedAs("Revert customer balance via `CustomerModel.balanceOnRevertedPayment()`")
        .isEqualTo(
            updatedCustomer?.copy(
                balance = updatedCustomer.balance + _productOrder.totalPrice.toLong()))
  }
}
