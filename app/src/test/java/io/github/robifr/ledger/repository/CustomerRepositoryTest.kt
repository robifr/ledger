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
import io.github.robifr.ledger.local.access.FakeCustomerDao
import io.mockk.clearAllMocks
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
class CustomerRepositoryTest {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _localDao: FakeCustomerDao
  private lateinit var _modelChangedListener: ModelChangedListener<CustomerModel>

  private val _customer: CustomerModel =
      CustomerModel(id = 111L, name = "Amy", balance = 0L, debt = (-100).toBigDecimal())
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
    _customerRepository = CustomerRepository(_localDao)
    _customerRepository.addModelChangedListener(_modelChangedListener)
  }

  @Test
  fun `select query result mapped customer`() {
    // Simulate current customer in the database with unmapped property.
    _localDao.data[0] = _customer.copy(debt = 0.toBigDecimal())
    assertSoftly {
      val message: String = "Map every customer property that doesn't belong to the database table"
      runTest {
        it.assertThat(_customerRepository.selectAll())
            .describedAs(message)
            .isEqualTo(listOf(_customer))
        it.assertThat(_customerRepository.selectById(_customer.id))
            .describedAs(message)
            .isEqualTo(_customer)
        it.assertThat(_customerRepository.selectById(listOfNotNull(_customer.id)))
            .describedAs(message)
            .isEqualTo(listOf(_customer))
        it.assertThat(_customerRepository.search(_customer.name))
            .describedAs(message)
            .isEqualTo(listOf(_customer))
      }
    }
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
    assertSoftly {
      runTest {
        it.assertThat(
                // A new customer doesn't have any debt.
                _customerRepository.add(_customer.copy(id = initialId, debt = 0.toBigDecimal())))
            .describedAs("Return the inserted customer ID")
            .isEqualTo(insertedId)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelAdded(
                  listOfNotNull(insertedCustomer?.copy(id = insertedId, debt = 0.toBigDecimal()))
                      .ifEmpty { any() })
            }
          }
          .describedAs("Notify added customer to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
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
    assertSoftly {
      runTest {
        it.assertThat(_customerRepository.update(_customer.copy(id = initialId)))
            .describedAs("Return the number of effected rows")
            .isEqualTo(listOfNotNull(updatedCustomer).size)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelUpdated(listOfNotNull(updatedCustomer).ifEmpty { any() })
            }
          }
          .describedAs("Notify updated customer to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
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
    assertSoftly {
      runTest {
        it.assertThat(_customerRepository.delete(initialId))
            .describedAs("Return the number of effected rows")
            .isEqualTo(listOfNotNull(deletedCustomer).size)
      }
      it.assertThatCode {
            verify(exactly = notifyCount) {
              _modelChangedListener.onModelDeleted(listOfNotNull(deletedCustomer).ifEmpty { any() })
            }
          }
          .describedAs("Notify deleted customer to the `ModelChangedListener`")
          .doesNotThrowAnyException()
    }
  }

  private fun `_search customer cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(listOf("Amy", "Cal"), "A", listOf("Amy", "Cal")),
          arrayOf(listOf("Amy", "Cal"), "a", listOf("Amy", "Cal")),
          arrayOf(listOf("Amy", "Cal"), "Amy", listOf("Amy")),
          arrayOf(listOf("Amy", "Cal"), " ", listOf<String>()))

  @ParameterizedTest
  @MethodSource("_search customer cases")
  fun `search customer`(
      customerNamesInDb: List<String>,
      query: String,
      customerNamesFound: List<String>
  ) = runTest {
    // Simulate exact amount of the customers within the database.
    _localDao.data.clear()
    _localDao.idGenerator.lastIncrement = 0
    customerNamesInDb.map { _localDao.data.add(_customer.copy(name = it)) }

    assertThat(_customerRepository.search(query).map { it.name })
        .describedAs("Search for customers whose names contain the query")
        .isEqualTo(customerNamesFound)
  }
}
