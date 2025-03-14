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

package io.github.robifr.ledger.data.model

import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    // Before payment is made, ensure the actual shown
    // balance — the one visible by user — is sufficient.
    assertSoftly {
      it.assertThat(_customer.isBalanceSufficient(_queue, _queue))
          .describedAs("Balance is sufficient when both customer equals")
          .isTrue()
      it.assertThat(_customer.isBalanceSufficient(null, _queue))
          .describedAs("Balance is sufficient when the customer in the old queue is null")
          .isTrue()
      it.assertThat(_customer.copy(id = 222L).isBalanceSufficient(null, _queue))
          .describedAs("Balance is insufficient when both customer differs")
          .isFalse()
    }
  }

  private fun `_is balance sufficient with less balance customer cases`(): Array<Array<Any>> =
      arrayOf(
          // It's sufficient when the new queue's grand total price is lesser or equal to the old
          // one. Because the old balance (500, which is the current balance + old queue's grand
          // total price) is greater than or equal to the new queue's grand total price.
          arrayOf(0L, _completedQueueWithAccountBalance, 500L, 400L, true),
          arrayOf(0L, _completedQueueWithAccountBalance, 500L, 500L, true),
          arrayOf(0L, _completedQueueWithAccountBalance, 500L, 600L, false),
          // It's insufficient when the old queue's status isn't completed and the payment method
          // isn't account balance. Because the old balance can't be reverted in that way.
          arrayOf(0L, _queue, 500L, 400L, false),
          arrayOf(0L, _queue, 500L, 500L, false),
          arrayOf(0L, _queue, 500L, 600L, false))

  @ParameterizedTest
  @MethodSource("_is balance sufficient with less balance customer cases")
  fun `is balance sufficient with less balance customer`(
      balance: Long,
      oldQueue: QueueModel,
      oldQueueGrandTotalPrice: Long,
      newQueueGrandTotalPrice: Long,
      isSufficient: Boolean
  ) {
    val lessBalanceCustomer: CustomerModel = _customer.copy(balance = balance)
    val oldQueue =
        oldQueue.copy(
            customer = lessBalanceCustomer,
            productOrders =
                _completedQueueWithAccountBalance.productOrders.map {
                  it.copy(totalPrice = oldQueueGrandTotalPrice.toBigDecimal())
                })
    val newQueue =
        _completedQueueWithAccountBalance.copy(
            customer = lessBalanceCustomer,
            productOrders =
                _completedQueueWithAccountBalance.productOrders.map {
                  it.copy(totalPrice = newQueueGrandTotalPrice.toBigDecimal())
                })
    // After payment is made. Ensure the balance — from both current and deducted balance —
    // is sufficient. This is a case where all the customer balance has already been used,
    // making it appear to be less than the queue's grand total price.
    assertThat(lessBalanceCustomer.isBalanceSufficient(oldQueue, newQueue))
        .describedAs(
            "Balance is sufficient when the new queue's total price is lesser or equal to the old")
        .isEqualTo(isSufficient)
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
    assertThat(_customer.balanceOnMadePayment(queue))
        .describedAs("Correctly calculate balance by deducting or keeping it")
        .isEqualTo(calculatedBalance)
  }

  @Test
  fun `balance on made payment with low balance`() {
    assertThat(
            _customer.copy(balance = 50L).balanceOnMadePayment(_completedQueueWithAccountBalance))
        .describedAs("Keep balance when the balance is low")
        .isEqualTo(50L)
  }

  @Test
  fun `balance on made payment with different customer`() {
    assertThat(_customer.copy(id = 222L).balanceOnMadePayment(_completedQueueWithAccountBalance))
        .describedAs("Keep balance when the customer differs with the one in queue")
        .isEqualTo(500L)
  }

  private fun `_balance on reverted payment with valid status and payment methods cases`():
      Array<Array<Any>> =
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
    assertThat(_customer.balanceOnRevertedPayment(queue))
        .describedAs("Correctly calculate balance by reverting or keeping it")
        .isEqualTo(calculatedBalance)
  }

  @Test
  fun `balance on reverted payment with different customer`() {
    assertThat(
            _customer.copy(id = 222L).balanceOnRevertedPayment(_completedQueueWithAccountBalance))
        .describedAs("Keep balance when the customer differs with the one in queue")
        .isEqualTo(500L)
  }

  private fun `_balance on updated payment with valid status and payment methods cases`():
      Array<Array<Any>> =
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
    assertThat(_customer.balanceOnUpdatedPayment(oldQueue, newQueue))
        .describedAs("Correctly calculate balance when payment is updated")
        .isEqualTo(calculatedBalance)
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
    assertThat(
            secondCustomer.balanceOnUpdatedPayment(
                oldQueue,
                if (isCustomerInNewQueueEquals) {
                  newQueue.copy(customerId = secondCustomer.id, customer = secondCustomer)
                } else {
                  newQueue
                }))
        .describedAs("Correctly calculate balance based on customer differences between queues")
        .isEqualTo(calculatedBalance)
  }

  private fun `_balance on updated payment with old queue has account balance payment and no customer cases`():
      Array<Array<Any>> =
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
    assertThat(
            _customer.balanceOnUpdatedPayment(
                _completedQueueWithAccountBalance.copy(customerId = null, customer = null),
                newQueue))
        .describedAs("Correctly calculate balance even when it seems impossible")
        .isEqualTo(calculatedBalance)
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
    assertThat(
            _customer.balanceOnUpdatedPayment(
                oldQueue,
                _completedQueueWithAccountBalance.copy(
                    productOrders =
                        _completedQueueWithAccountBalance.productOrders.map {
                          it.copy(totalPrice = newQueueGrandTotalPrice.toBigDecimal())
                        })))
        .describedAs("Correctly calculate balance when grand total price differs")
        .isEqualTo(calculatedBalance)
  }

  @ParameterizedTest
  @EnumSource(QueueModel.Status::class)
  fun `debt on made payment with valid status`(status: QueueModel.Status) {
    assertThat(_customer.debtOnMadePayment(_queue.copy(status = status)))
        .describedAs("Correctly calculate debt by adding or keeping it")
        .isEqualTo(
            if (status == QueueModel.Status.UNPAID) (-100).toBigDecimal() else 0.toBigDecimal())
  }

  @Test
  fun `debt on made payment with different customer`() {
    assertThat(_customer.copy(id = 222L).debtOnMadePayment(_unpaidQueue))
        .describedAs("Keep debt when customer differs with the one in queue")
        .isEqualTo(0.toBigDecimal())
  }

  @ParameterizedTest
  @EnumSource(QueueModel.Status::class)
  fun `debt on reverted payment with valid status`(status: QueueModel.Status) {
    assertThat(_customer.debtOnRevertedPayment(_queue.copy(status = status)))
        .describedAs("Correctly calculate debt by reverting or keeping it")
        .isEqualTo(if (status == QueueModel.Status.UNPAID) 100.toBigDecimal() else 0.toBigDecimal())
  }

  @Test
  fun `debt on reverted payment with different customer`() {
    assertThat(_customer.copy(id = 222L).debtOnRevertedPayment(_unpaidQueue))
        .describedAs("Keep debt when customer differs with the one in queue")
        .isEqualTo(0.toBigDecimal())
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
    assertThat(_customer.debtOnUpdatedPayment(oldQueue, newQueue))
        .describedAs("Correctly calculate debt when payment is updated")
        .isEqualTo(calculatedDebt.toBigDecimal())
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
    assertThat(
            secondCustomer.debtOnUpdatedPayment(
                oldQueue,
                if (isCustomerInNewQueueEquals) {
                  newQueue.copy(customerId = secondCustomer.id, customer = secondCustomer)
                } else {
                  newQueue
                }))
        .describedAs("Correctly calculate debt based on customer differences between queues")
        .isEqualTo(calculatedDebt.toBigDecimal())
  }

  private fun `_debt on updated payment with old queue has unpaid status and no customer cases`():
      Array<Array<Any>> = arrayOf(arrayOf(_queue, 0), arrayOf(_unpaidQueue, -100))

  @ParameterizedTest
  @MethodSource("_debt on updated payment with old queue has unpaid status and no customer cases")
  fun `debt on updated payment with old queue has unpaid status and no customer`(
      newQueue: QueueModel,
      calculatedBalance: Long
  ) {
    assertThat(
            _customer.debtOnUpdatedPayment(
                _unpaidQueue.copy(customerId = null, customer = null), newQueue))
        .describedAs(
            "Correctly calculate debt when the old queue has unpaid status and no customer")
        .isEqualTo(calculatedBalance.toBigDecimal())
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
    assertThat(
            _customer.debtOnUpdatedPayment(
                oldQueue,
                newQueue.copy(
                    productOrders =
                        newQueue.productOrders.map {
                          it.copy(totalPrice = newQueueGrandTotalPrice.toBigDecimal())
                        })))
        .describedAs("Correctly calculate debt when grand total price differs")
        .isEqualTo(calculatedDebt.toBigDecimal())
  }
}
