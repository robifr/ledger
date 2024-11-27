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

package com.robifr.ledger.data.model

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerModelTest {
  private val _customer: CustomerModel = CustomerModel(id = 111L, name = "Amy", balance = 500L)
  private val _product: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)
  private val _productOrder: ProductOrderModel =
      ProductOrderModel(
          id = 111L,
          queueId = 111L,
          productId = _product.id,
          productName = _product.name,
          productPrice = _product.price,
          quantity = 1.0)
  private val _queue: QueueModel =
      QueueModel(
          id = 111L,
          customerId = _customer.id,
          customer = _customer,
          status = QueueModel.Status.IN_QUEUE,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders = listOf(_productOrder))

  private val _uncompletedQueueWithCash: QueueModel = _queue
  private val _uncompletedQueueWithAccountBalance: QueueModel =
      _queue.copy(paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE)
  private val _completedQueueWithCash: QueueModel =
      _queue.copy(status = QueueModel.Status.COMPLETED)
  private val _completedQueueWithAccountBalance: QueueModel =
      _queue.copy(
          status = QueueModel.Status.COMPLETED,
          paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE)

  private val _unpaidQueue: QueueModel = _queue.copy(status = QueueModel.Status.UNPAID)

  @Test
  fun `is balance sufficient with different customer in queue`() {
    assertAll({
      // Before payment is made, ensure the actual shown
      // balance — the one visible by user — is sufficient.
      assertTrue(
          _customer.isBalanceSufficient(_queue, _queue),
          "Balance is sufficient when both customer equals")
      assertTrue(
          _customer.isBalanceSufficient(null, _queue),
          "Balance is sufficient when the customer in the old queue is null")
      assertFalse(
          _customer.copy(id = 222L).isBalanceSufficient(null, _queue),
          "Balance is insufficient when both customer differs")
    })
  }

  @Test
  fun `is balance sufficient with less balance customer`() {
    val lessBalanceCustomer: CustomerModel = _customer.copy(balance = 0L)
    val queueWithGrandTotalPriceLessThanCustomerBalance =
        _queue.copy(
            customer = lessBalanceCustomer,
            productOrders = _queue.productOrders.map { it.copy(totalPrice = 400.toBigDecimal()) })
    val queueWithGrandTotalPriceEqualsCustomerBalance =
        _queue.copy(
            customer = lessBalanceCustomer,
            productOrders = _queue.productOrders.map { it.copy(totalPrice = 500.toBigDecimal()) })
    val queueWithGrandTotalPriceMoreThanCustomerBalance =
        _queue.copy(
            customer = lessBalanceCustomer,
            productOrders = _queue.productOrders.map { it.copy(totalPrice = 600.toBigDecimal()) })
    assertAll({
      // After payment is made. Ensure the balance — from both current and deducted balance —
      // is sufficient. This is a case where all the customer balance has already been used,
      // making it appear to be less than the queue's grand total price.
      assertTrue(
          lessBalanceCustomer.isBalanceSufficient(
              queueWithGrandTotalPriceEqualsCustomerBalance,
              queueWithGrandTotalPriceLessThanCustomerBalance),
          "Balance is sufficient when the new queue has lesser grand total price")
      assertTrue(
          lessBalanceCustomer.isBalanceSufficient(
              queueWithGrandTotalPriceEqualsCustomerBalance,
              queueWithGrandTotalPriceEqualsCustomerBalance),
          "Balance is sufficient when the new queue has equals grand total price")
      assertFalse(
          lessBalanceCustomer.isBalanceSufficient(
              queueWithGrandTotalPriceEqualsCustomerBalance,
              queueWithGrandTotalPriceMoreThanCustomerBalance),
          "Balance is insufficient when the new queue has more grand total price")
    })
  }

  private fun `_balance on made payment with valid status and payment methods cases`():
      Array<Array<Any>> =
      arrayOf(
          arrayOf(_uncompletedQueueWithCash, 500L),
          arrayOf(_uncompletedQueueWithAccountBalance, 500L),
          arrayOf(_completedQueueWithCash, 500L),
          arrayOf(_completedQueueWithAccountBalance, 400L))

  @ParameterizedTest
  @MethodSource("_balance on made payment with valid status and payment methods cases")
  fun `balance on made payment with valid status and payment methods`(
      queue: QueueModel,
      calculatedBalance: Long
  ) {
    assertEquals(
        calculatedBalance,
        _customer.balanceOnMadePayment(queue),
        "Correctly calculate balance by deducting or keeping it")
  }

  @Test
  fun `balance on made payment with low balance`() {
    assertEquals(
        50L,
        _customer.copy(balance = 50L).balanceOnMadePayment(_completedQueueWithAccountBalance),
        "Keep balance when the balance is low")
  }

  @Test
  fun `balance on made payment with different customer`() {
    assertEquals(
        500L,
        _customer.copy(id = 222L).balanceOnMadePayment(_completedQueueWithAccountBalance),
        "Keep balance when the customer differs with the one in queue")
  }

  private fun `_balance on reverted payment with valid status and payment methods cases`():
      Array<Any> =
      arrayOf(
          arrayOf(_uncompletedQueueWithCash, 500L),
          arrayOf(_uncompletedQueueWithAccountBalance, 500L),
          arrayOf(_completedQueueWithCash, 500L),
          arrayOf(_completedQueueWithAccountBalance, 600L))

  @ParameterizedTest
  @MethodSource("_balance on reverted payment with valid status and payment methods cases")
  fun `balance on reverted payment with valid status and payment methods`(
      queue: QueueModel,
      calculatedBalance: Long
  ) {
    assertEquals(
        calculatedBalance,
        _customer.balanceOnRevertedPayment(queue),
        "Correctly calculate balance by reverting or keeping it")
  }

  @Test
  fun `balance on reverted payment with different customer`() {
    assertEquals(
        500L,
        _customer.copy(id = 222L).balanceOnRevertedPayment(_completedQueueWithAccountBalance),
        "Keep balance when the customer differs with the one in queue")
  }

  private fun `_balance on updated payment with valid status and payment methods cases`():
      Array<Any> =
      arrayOf(
          arrayOf(_uncompletedQueueWithCash, _uncompletedQueueWithCash, 500L),
          arrayOf(_uncompletedQueueWithCash, _uncompletedQueueWithAccountBalance, 500L),
          arrayOf(_uncompletedQueueWithCash, _completedQueueWithCash, 500L),
          arrayOf(_uncompletedQueueWithCash, _completedQueueWithAccountBalance, 400L),
          arrayOf(_uncompletedQueueWithAccountBalance, _uncompletedQueueWithCash, 500L),
          arrayOf(_uncompletedQueueWithAccountBalance, _uncompletedQueueWithAccountBalance, 500L),
          arrayOf(_uncompletedQueueWithAccountBalance, _completedQueueWithCash, 500L),
          arrayOf(_uncompletedQueueWithAccountBalance, _completedQueueWithAccountBalance, 400L),
          arrayOf(_completedQueueWithCash, _uncompletedQueueWithCash, 500L),
          arrayOf(_completedQueueWithCash, _uncompletedQueueWithAccountBalance, 500L),
          arrayOf(_completedQueueWithCash, _completedQueueWithCash, 500L),
          arrayOf(_completedQueueWithCash, _completedQueueWithAccountBalance, 400L),
          arrayOf(_completedQueueWithAccountBalance, _uncompletedQueueWithCash, 600L),
          arrayOf(_completedQueueWithAccountBalance, _uncompletedQueueWithAccountBalance, 600L),
          arrayOf(_completedQueueWithAccountBalance, _completedQueueWithCash, 600L),
          arrayOf(_completedQueueWithAccountBalance, _completedQueueWithAccountBalance, 500L))

  @ParameterizedTest
  @MethodSource("_balance on updated payment with valid status and payment methods cases")
  fun `balance on updated payment with valid status and payment methods`(
      oldQueue: QueueModel,
      newQueue: QueueModel,
      calculatedBalance: Long
  ) {
    assertEquals(
        calculatedBalance,
        _customer.balanceOnUpdatedPayment(oldQueue, newQueue),
        "Correctly calculate balance when payment is updated")
  }

  /**
   * Every possible scenario from
   * [_balance on updated payment with valid status and payment methods cases] where the balance
   * changes.
   */
  private fun `_balance on updated payment with different customer cases`(): Array<Array<Any>> =
      arrayOf(
          // spotless:off
          // Customer is equal to the one in the new queue.
          arrayOf(_uncompletedQueueWithCash, _completedQueueWithAccountBalance, true, 400L),
          arrayOf(_uncompletedQueueWithAccountBalance, _completedQueueWithAccountBalance, true, 400L),
          arrayOf(_completedQueueWithCash, _completedQueueWithAccountBalance, true, 400L),
          arrayOf(_completedQueueWithAccountBalance, _completedQueueWithAccountBalance, true, 400L),
          // Customer differs from the one in the new queue.
          arrayOf(_uncompletedQueueWithCash, _completedQueueWithAccountBalance, false, 500L),
          arrayOf(_uncompletedQueueWithAccountBalance, _completedQueueWithAccountBalance, false, 500L),
          arrayOf(_completedQueueWithCash, _completedQueueWithAccountBalance, false, 500L),
          arrayOf(_completedQueueWithAccountBalance, _completedQueueWithAccountBalance, false, 500L))
          // spotless:on

  @ParameterizedTest
  @MethodSource("_balance on updated payment with different customer cases")
  fun `balance on updated payment with different customer`(
      oldQueue: QueueModel,
      newQueue: QueueModel,
      isCustomerInNewQueueEquals: Boolean,
      calculatedBalance: Long
  ) {
    val secondCustomer: CustomerModel = _customer.copy(id = 222L)
    assertEquals(
        calculatedBalance,
        secondCustomer.balanceOnUpdatedPayment(
            oldQueue,
            if (isCustomerInNewQueueEquals) {
              newQueue.copy(customerId = secondCustomer.id, customer = secondCustomer)
            } else {
              newQueue
            }),
        "Correctly calculate balance based on customer differences between queues")
  }

  private fun `_balance on updated payment with old queue has account balance payment and no customer cases`():
      Array<Any> =
      arrayOf(
          arrayOf(_uncompletedQueueWithCash, 500L),
          arrayOf(_uncompletedQueueWithAccountBalance, 500L),
          arrayOf(_completedQueueWithCash, 500L),
          arrayOf(_completedQueueWithAccountBalance, 400L))

  @ParameterizedTest
  @MethodSource(
      "_balance on updated payment with old queue has account balance payment and no customer cases")
  fun `balance on updated payment with old queue has account balance payment and no customer`(
      newQueue: QueueModel,
      calculatedBalance: Long
  ) {
    // When the old queue doesn't have customer beforehand. Though, it should be impossible
    // in first place to have a completed queue with account balance and no customer.
    assertEquals(
        calculatedBalance,
        _customer.balanceOnUpdatedPayment(
            _completedQueueWithAccountBalance.copy(customerId = null, customer = null), newQueue),
        "Correctly calculate balance even when it seems impossible")
  }

  private fun `_balance on updated payment with different grand total price cases`():
      Array<Array<Any>> =
      arrayOf(
          // Grand total price in the new queue is greater than in the old queue.
          arrayOf(_uncompletedQueueWithCash, 200, 300L),
          arrayOf(_uncompletedQueueWithAccountBalance, 200, 300L),
          arrayOf(_completedQueueWithCash, 200, 300L),
          arrayOf(_completedQueueWithAccountBalance, 200, 400L),
          // Grand total price in the new queue is less than in the old queue.
          arrayOf(_uncompletedQueueWithCash, 50, 450L),
          arrayOf(_uncompletedQueueWithAccountBalance, 50, 450L),
          arrayOf(_completedQueueWithCash, 50, 450L),
          arrayOf(_completedQueueWithAccountBalance, 50, 550L))

  @ParameterizedTest
  @MethodSource("_balance on updated payment with different grand total price cases")
  fun `balance on updated payment with different grand total price`(
      oldQueue: QueueModel,
      newQueueGrandTotalPrice: Int,
      calculatedBalance: Long
  ) {
    assertEquals(
        calculatedBalance,
        _customer.balanceOnUpdatedPayment(
            oldQueue,
            _completedQueueWithAccountBalance.copy(
                productOrders =
                    _completedQueueWithAccountBalance.productOrders.map {
                      it.copy(totalPrice = newQueueGrandTotalPrice.toBigDecimal())
                    })),
        "Correctly calculate balance when grand total price differs")
  }

  @ParameterizedTest
  @EnumSource(QueueModel.Status::class)
  fun `debt on made payment with valid status`(status: QueueModel.Status) {
    assertEquals(
        if (status == QueueModel.Status.UNPAID) (-100).toBigDecimal() else 0.toBigDecimal(),
        _customer.debtOnMadePayment(_queue.copy(status = status)),
        "Correctly calculate debt by adding or keeping it")
  }

  @Test
  fun `debt on made payment with different customer`() {
    assertEquals(
        0.toBigDecimal(),
        _customer.copy(id = 222L).debtOnMadePayment(_unpaidQueue),
        "Keep debt when customer differs with the one in queue")
  }

  @ParameterizedTest
  @EnumSource(QueueModel.Status::class)
  fun `debt on reverted payment with valid status`(status: QueueModel.Status) {
    assertEquals(
        if (status == QueueModel.Status.UNPAID) 100.toBigDecimal() else 0.toBigDecimal(),
        _customer.debtOnRevertedPayment(_queue.copy(status = status)),
        "Correctly calculate debt by reverting or keeping it")
  }

  @Test
  fun `debt on reverted payment with different customer`() {
    assertEquals(
        0.toBigDecimal(),
        _customer.copy(id = 222L).debtOnRevertedPayment(_unpaidQueue),
        "Keep debt when customer differs with the one in queue")
  }

  private fun `_debt on updated payment with valid status cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf(_queue, _queue, 0),
          arrayOf(_queue, _unpaidQueue, -100),
          arrayOf(_unpaidQueue, _queue, 100),
          arrayOf(_unpaidQueue, _unpaidQueue, 0))

  @ParameterizedTest
  @MethodSource("_debt on updated payment with valid status cases")
  fun `debt on updated payment with valid status`(
      oldQueue: QueueModel,
      newQueue: QueueModel,
      calculatedDebt: Int
  ) {
    assertEquals(
        calculatedDebt.toBigDecimal(),
        _customer.debtOnUpdatedPayment(oldQueue, newQueue),
        "Correctly calculate debt when payment is updated")
  }

  private fun `_debt on updated payment with different customer cases`(): Array<Array<Any>> =
      arrayOf(
          // Customer is equal to the one in the new queue.
          arrayOf(_queue, _queue, true, 0),
          arrayOf(_queue, _unpaidQueue, true, -100),
          arrayOf(_unpaidQueue, _queue, true, 0),
          arrayOf(_unpaidQueue, _unpaidQueue, true, -100),
          // Customer differs from the one in the new queue.
          arrayOf(_queue, _queue, false, 0),
          arrayOf(_queue, _unpaidQueue, false, 0),
          arrayOf(_unpaidQueue, _queue, false, 0),
          arrayOf(_unpaidQueue, _unpaidQueue, false, 0))

  @ParameterizedTest
  @MethodSource("_debt on updated payment with different customer cases")
  fun `debt on updated payment with different customer`(
      oldQueue: QueueModel,
      newQueue: QueueModel,
      isCustomerInNewQueueEquals: Boolean,
      calculatedDebt: Int
  ) {
    val secondCustomer: CustomerModel = _customer.copy(id = 222L)
    assertEquals(
        calculatedDebt.toBigDecimal(),
        secondCustomer.debtOnUpdatedPayment(
            oldQueue,
            if (isCustomerInNewQueueEquals) {
              newQueue.copy(customerId = secondCustomer.id, customer = secondCustomer)
            } else {
              newQueue
            }),
        "Correctly calculate debt based on customer differences between queues")
  }

  private fun `_debt on updated payment with old queue has unpaid status and no customer cases`():
      Array<Any> = arrayOf(arrayOf(_queue, 0), arrayOf(_unpaidQueue, -100))

  @ParameterizedTest
  @MethodSource("_debt on updated payment with old queue has unpaid status and no customer cases")
  fun `debt on updated payment with old queue has unpaid status and no customer`(
      newQueue: QueueModel,
      calculatedBalance: Long
  ) {
    assertEquals(
        calculatedBalance.toBigDecimal(),
        _customer.debtOnUpdatedPayment(
            _unpaidQueue.copy(customerId = null, customer = null), newQueue),
        "Correctly calculate debt when the old queue has unpaid status and no customer")
  }

  private fun `_debt on updated payment with different grand total price cases`():
      Array<Array<Any>> =
      arrayOf(
          // Grand total price in the new queue is greater than in the old queue.
          arrayOf(_queue, _queue, 200, 0),
          arrayOf(_queue, _unpaidQueue, 200, -200),
          arrayOf(_unpaidQueue, _queue, 200, 100), // Revert to original.
          arrayOf(_unpaidQueue, _unpaidQueue, 200, -100),
          // Grand total price in the new queue is less than in the old queue.
          arrayOf(_queue, _queue, 50, 0),
          arrayOf(_queue, _unpaidQueue, 50, -50),
          arrayOf(_unpaidQueue, _queue, 50, 100), // Revert to original.
          arrayOf(_unpaidQueue, _unpaidQueue, 50, 50))

  @ParameterizedTest
  @MethodSource("_debt on updated payment with different grand total price cases")
  fun `debt on updated payment with different grand total price`(
      oldQueue: QueueModel,
      newQueue: QueueModel,
      newQueueGrandTotalPrice: Int,
      calculatedDebt: Int
  ) {
    assertEquals(
        calculatedDebt.toBigDecimal(),
        _customer.debtOnUpdatedPayment(
            oldQueue,
            newQueue.copy(
                productOrders =
                    newQueue.productOrders.map {
                      it.copy(totalPrice = newQueueGrandTotalPrice.toBigDecimal())
                    })),
        "Correctly calculate debt when grand total price differs")
  }
}
