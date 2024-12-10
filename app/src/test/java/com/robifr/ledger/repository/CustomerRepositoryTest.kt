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
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.local.access.FakeCustomerDao
import io.mockk.clearAllMocks
import io.mockk.spyk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(MainCoroutineExtension::class)
class CustomerRepositoryTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _localDao: FakeCustomerDao
  private lateinit var _modelChangedListener: ModelChangedListener<CustomerModel>

  private val _customer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 0L)
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
          status = QueueModel.Status.UNPAID,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders = listOf(_productOrder))

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _modelChangedListener = spyk()
    _localDao =
        FakeCustomerDao(
            data = mutableListOf(_customer),
            queueData = mutableListOf(_queue),
            productOrderData = mutableListOf(_productOrder))
    _customerRepository = CustomerRepository(_dispatcher, _localDao)
    _customerRepository.addModelChangedListener(_modelChangedListener)
  }

  @Test
  fun `select query result mapped customer`() {
    val mappedCustomer: CustomerModel = _customer.copy(debt = (-100).toBigDecimal())
    assertAll(
        "Map every customer property that doesn't belong to the database table",
        { runTest { assertEquals(listOf(mappedCustomer), _customerRepository.selectAll()) } },
        { runTest { assertEquals(mappedCustomer, _customerRepository.selectById(_customer.id)) } },
        {
          runTest {
            assertEquals(
                listOf(mappedCustomer), _customerRepository.selectById(listOfNotNull(_customer.id)))
          }
        },
        {
          runTest {
            assertEquals(listOf(mappedCustomer), _customerRepository.search(_customer.name))
          }
        })
  }

  private fun `_add customer cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(null, 222L, _customer, 1),
          // Customer with the same ID already exist.
          arrayOf(111L, 0L, null, 0))

  @ParameterizedTest
  @MethodSource("_add customer cases")
  fun `add customer`(
      initialId: Long?,
      insertedId: Long,
      insertedCustomer: CustomerModel?,
      notifyCount: Int
  ) {
    assertAll(
        {
          runTest {
            assertEquals(
                insertedId,
                _customerRepository.add(_customer.copy(id = initialId)),
                "Return the inserted customer ID")
          }
        },
        {
          assertDoesNotThrow("Notify added customer to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelAdded(
                  listOfNotNull(
                          insertedCustomer?.copy(
                              id = insertedId,
                              // Map properties that doesn't belong to the table.
                              debt = _localDao.totalDebtById(insertedId)))
                      .ifEmpty { any() })
            }
          }
        })
  }

  private fun `_update customer cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, _customer, 1),
          // Customer with the same ID doesn't exist.
          arrayOf(222L, null, 0),
          arrayOf(null, null, 0))

  @ParameterizedTest
  @MethodSource("_update customer cases")
  fun `update customer`(initialId: Long?, updatedCustomer: CustomerModel?, notifyCount: Int) {
    assertAll(
        {
          runTest {
            assertEquals(
                listOfNotNull(updatedCustomer).size,
                _customerRepository.update(_customer.copy(id = initialId)),
                "Return the number of effected rows")
          }
        },
        {
          assertDoesNotThrow("Notify updated customer to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelUpdated(
                  listOfNotNull(
                          updatedCustomer?.copy(
                              // Map properties that doesn't belong to the table.
                              debt = _localDao.totalDebtById(_customer.id)))
                      .ifEmpty { any() })
            }
          }
        })
  }

  private fun `_delete customer cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(111L, _customer, 1),
          // Customer with the same ID doesn't exist.
          arrayOf(222L, null, 0),
          arrayOf(null, null, 0))

  @ParameterizedTest
  @MethodSource("_delete customer cases")
  fun `delete customer`(initialId: Long?, deletedCustomer: CustomerModel?, notifyCount: Int) {
    assertAll(
        {
          runTest {
            assertEquals(
                listOfNotNull(deletedCustomer).size,
                _customerRepository.delete(_customer.copy(id = initialId)),
                "Return the number of effected rows")
          }
        },
        {
          assertDoesNotThrow("Notify deleted customer to the `ModelChangedListener`") {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelDeleted(
                  listOfNotNull(
                          deletedCustomer?.copy(
                              // Map properties that doesn't belong to the table.
                              debt = _localDao.totalDebtById(_customer.id)))
                      .ifEmpty { any() })
            }
          }
        })
  }
}
