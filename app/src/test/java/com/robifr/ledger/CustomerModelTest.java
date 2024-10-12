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

package com.robifr.ledger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import androidx.annotation.NonNull;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.data.model.ProductOrderModel;
import com.robifr.ledger.data.model.QueueModel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class CustomerModelTest {
  @NonNull
  private final CustomerModel _customer =
      CustomerModel.toBuilder()
          .withName("Amy")
          .withId(1L)
          .withBalance(10_000L)
          .withDebt(BigDecimal.ZERO);

  @NonNull
  private final ProductOrderModel _order =
      ProductOrderModel.toBuilder()
          .withId(1L)
          .withQueueId(1L)
          .withProductId(1L)
          .withProductName("Apple")
          .withProductPrice(1000L)
          .withQuantity(1.0)
          .withDiscount(0L)
          .withTotalPrice(BigDecimal.valueOf(1000L));

  @NonNull
  private final QueueModel _queue =
      QueueModel.toBuilder()
          .withStatus(QueueModel.Status.IN_QUEUE)
          .withDate(Instant.now())
          .withPaymentMethod(QueueModel.PaymentMethod.CASH)
          .withId(1L)
          .withCustomerId(1L)
          .withCustomer(this._customer)
          .withProductOrders(List.of(this._order));

  @Test
  public void balanceSufficient() {
    final CustomerModel secondCustomer = this._customer.withId(2L).withName("Ben");
    final CustomerModel noBalanceCustomer = this._customer.withBalance(0L);

    final QueueModel queue_noBalance_totalPriceLessThanOriginalBalance =
        this._queue
            .withCustomer(noBalanceCustomer)
            .withProductOrders(Collections.nCopies(9, this._order));
    final QueueModel queue_noBalance_totalPriceEqualsOriginalBalance =
        this._queue
            .withCustomer(noBalanceCustomer)
            .withProductOrders(Collections.nCopies(10, this._order));
    final QueueModel queue_noBalance_totalPriceMoreThanOriginalBalance =
        this._queue
            .withCustomer(noBalanceCustomer)
            .withProductOrders(Collections.nCopies(11, this._order));

    assertAll( // spotless:off
        // Before payment is made. Ensure the actual shown balance — the one
        // visible by user — is sufficient.
        () -> assertTrue(this._customer.isBalanceSufficient(this._queue, this._queue), "Sufficient balance when both customer equals"),
        () -> assertTrue(this._customer.isBalanceSufficient(null, this._queue), "Sufficient balance when old queue customer is null"),
        () -> assertFalse(secondCustomer.isBalanceSufficient(null, this._queue), "Insufficient balance when both customer differs"),

        // After payment is made. Ensure the balance — from
        // both current and deducted balance — is sufficient.
        () -> assertTrue(noBalanceCustomer.isBalanceSufficient(queue_noBalance_totalPriceEqualsOriginalBalance, queue_noBalance_totalPriceLessThanOriginalBalance), "Sufficient balance for customer with no balance when new queue has less total price"),
        () -> assertTrue(noBalanceCustomer.isBalanceSufficient(queue_noBalance_totalPriceEqualsOriginalBalance, queue_noBalance_totalPriceEqualsOriginalBalance), "Sufficient balance for customer with no balance when new queue has equals total price"),
        () -> assertFalse(noBalanceCustomer.isBalanceSufficient(queue_noBalance_totalPriceEqualsOriginalBalance, queue_noBalance_totalPriceMoreThanOriginalBalance), "Insufficient balance for customer with no balance when new queue has more total price")
    ); // spotless:on
  }

  @Test
  public void balanceOnPayment() {
    final QueueModel completedQueue_accountBalance =
        this._queue
            .withStatus(QueueModel.Status.COMPLETED)
            .withPaymentMethod(QueueModel.PaymentMethod.ACCOUNT_BALANCE);
    final QueueModel completedQueue_cash =
        this._queue
            .withStatus(QueueModel.Status.COMPLETED)
            .withPaymentMethod(QueueModel.PaymentMethod.CASH);
    final QueueModel uncompletedQueue_accountBalance =
        this._queue
            .withStatus(QueueModel.Status.IN_QUEUE)
            .withPaymentMethod(QueueModel.PaymentMethod.ACCOUNT_BALANCE);
    final QueueModel uncompletedQueue_cash =
        this._queue
            .withStatus(QueueModel.Status.IN_QUEUE)
            .withPaymentMethod(QueueModel.PaymentMethod.CASH);

    final CustomerModel lowBalanceCustomer = this._customer.withBalance(500L);
    final CustomerModel secondCustomer = this._customer.withId(2L).withName("Ben");

    final QueueModel completedQueue_accountBalance_noCustomer =
        completedQueue_accountBalance.withCustomerId(null).withCustomer(null);
    final QueueModel completedQueue_accountBalance_secondCustomer =
        completedQueue_accountBalance
            .withCustomerId(secondCustomer.id())
            .withCustomer(secondCustomer);
    final QueueModel completedQueue_accountBalance_doubleOrder =
        completedQueue_accountBalance.withProductOrders(List.of(this._order, this._order));
    final QueueModel completedQueue_accountBalance_noOrder =
        completedQueue_accountBalance.withProductOrders(Collections.emptyList());

    assertAll( // spotless:off
        () -> assertEquals(9000L, this._customer.balanceOnMadePayment(completedQueue_accountBalance), "Deduct balance when queue completed with account balance"),
        () -> assertEquals(10_000L, this._customer.balanceOnMadePayment(completedQueue_cash), "Keep balance when queue completed with cash"),
        () -> assertEquals(10_000L, this._customer.balanceOnMadePayment(uncompletedQueue_cash), "Keep balance when queue uncompleted"),
        () -> assertEquals(10_000L, this._customer.balanceOnMadePayment(uncompletedQueue_accountBalance), "Keep balance when queue uncompleted"),
        () -> assertEquals(500L, lowBalanceCustomer.balanceOnMadePayment(completedQueue_accountBalance), "Keep balance when the balance low"),
        () -> assertEquals(10_000L, secondCustomer.balanceOnMadePayment(completedQueue_accountBalance), "Keep balance when the customer differs with the one in the queue"),

        () -> assertEquals(11_000L, this._customer.balanceOnRevertedPayment(completedQueue_accountBalance), "Revert balance when queue completed with account balance"),
        () -> assertEquals(10_000L, this._customer.balanceOnRevertedPayment(completedQueue_cash), "Keep balance when queue completed with cash"),
        () -> assertEquals(10_000L, this._customer.balanceOnRevertedPayment(uncompletedQueue_cash), "Keep balance when queue uncompleted"),
        () -> assertEquals(10_000L, this._customer.balanceOnRevertedPayment(uncompletedQueue_accountBalance), "Keep balance when queue uncompleted"),
        () -> assertEquals(10_000L, secondCustomer.balanceOnRevertedPayment(completedQueue_accountBalance), "Keep balance when the customer differs with the one in the queue"),

        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance, completedQueue_accountBalance), "Keep balance when queue unchanged"),
        () -> assertEquals(11_000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance, completedQueue_cash), "Revert balance when queue changed to cash"),
        () -> assertEquals(11_000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance, uncompletedQueue_accountBalance), "Revert balance when old queue with account balance changed to uncompleted"),
        () -> assertEquals(11_000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance, uncompletedQueue_cash), "Revert balance when old queue with account balance changed to uncompleted"),

        () -> assertEquals(9000L, this._customer.balanceOnUpdatedPayment(completedQueue_cash, completedQueue_accountBalance), "Deduct balance when queue changed to account balance"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(completedQueue_cash, completedQueue_cash), "Keep balance when queue unchanged"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(completedQueue_cash, uncompletedQueue_accountBalance), "Keep balance when old queue with cash changed to uncompleted"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(completedQueue_cash, uncompletedQueue_cash), "Keep balance when old queue with cash changed to uncompleted"),
        () -> assertEquals(10_000L, secondCustomer.balanceOnUpdatedPayment(completedQueue_cash, completedQueue_accountBalance), "Keep balance when both customer differs"),

        () -> assertEquals(9000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_accountBalance, completedQueue_accountBalance), "Deduct balance when queue changed to completed with account balance"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_accountBalance, completedQueue_cash), "Keep balance when queue changed to completed with cash"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_accountBalance, uncompletedQueue_accountBalance), "Keep balance when queue stays uncompleted"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_accountBalance, uncompletedQueue_cash), "Keep balance when queue stays uncompleted"),

        () -> assertEquals(9000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_cash, completedQueue_accountBalance), "Deduct balance when queue changed to completed with account balance"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_cash, completedQueue_cash), "Keep balance when queue stays with cash"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_cash, uncompletedQueue_accountBalance), "Keep balance when queue stays uncompleted"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_cash, uncompletedQueue_cash), "Keep balance when queue stays uncompleted"),

        // When the old queue doesn't have customer beforehand.
        // Though, it should be impossible in first place to have
        // a completed queue with account balance and no customer.
        () -> assertEquals(9000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance_noCustomer, completedQueue_accountBalance), "Deduct balance when queue changed to have a customer"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance_noCustomer, completedQueue_cash), "Keep balance when old queue has no customer"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance_noCustomer, uncompletedQueue_accountBalance), "Keep balance when old queue has no customer"),
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance_noCustomer, uncompletedQueue_cash), "Keep balance when old queue has no customer"),

        // When the queue has different customer.
        () -> assertEquals(9000L, secondCustomer.balanceOnUpdatedPayment(completedQueue_accountBalance, completedQueue_accountBalance_secondCustomer), "Deduct balance when queue completed with different customer"),
        () -> assertEquals(9000L, secondCustomer.balanceOnUpdatedPayment(completedQueue_cash, completedQueue_accountBalance_secondCustomer), "Deduct balance when queue completed with different customer"),
        () -> assertEquals(9000L, secondCustomer.balanceOnUpdatedPayment(uncompletedQueue_accountBalance, completedQueue_accountBalance_secondCustomer), "Deduct balance when queue completed with different customer"),
        () -> assertEquals(9000L, secondCustomer.balanceOnUpdatedPayment(uncompletedQueue_cash, completedQueue_accountBalance_secondCustomer), "Deduct balance when queue completed with different customer"),

        // When the queue has more total price of ordered products.
        () -> assertEquals(9000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance, completedQueue_accountBalance_doubleOrder), "Deduct balance by total price difference when queue completed with more total price"),
        () -> assertEquals(8000L, this._customer.balanceOnUpdatedPayment(completedQueue_cash, completedQueue_accountBalance_doubleOrder), "Deduct balance when queue completed with more total price"),
        () -> assertEquals(8000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_accountBalance, completedQueue_accountBalance_doubleOrder), "Deduct balance when queue completed with more total price"),
        () -> assertEquals(8000L, this._customer.balanceOnUpdatedPayment(uncompletedQueue_cash, completedQueue_accountBalance_doubleOrder), "Deduct balance when queue completed with more total price"),

        // When the queue has less total price
        // of ordered products — reduced of product, quantity, etc.
        () -> assertEquals(11_000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance, completedQueue_accountBalance_noOrder), "Revert balance when queue completed with lesser total price"),
        // It should be impossible in first place.
        () -> assertEquals(10_000L, this._customer.balanceOnUpdatedPayment(completedQueue_accountBalance_noCustomer, completedQueue_accountBalance_noOrder), "Revert balance up to original when old queue with no customer completed with lesser total price")
    ); // spotless:on
  }

  @Test
  public void debtOnPayment() {
    final List<QueueModel> notUnpaidQueues =
        List.of(
            this._queue.withStatus(QueueModel.Status.IN_QUEUE),
            this._queue.withStatus(QueueModel.Status.IN_PROCESS),
            this._queue.withStatus(QueueModel.Status.COMPLETED));
    final QueueModel unpaidQueue = this._queue.withStatus(QueueModel.Status.UNPAID);
    final QueueModel unpaidQueue_noCustomer = unpaidQueue.withCustomerId(null).withCustomer(null);

    final CustomerModel secondCustomer = this._customer.withId(2L).withName("Ben");
    final QueueModel unpaidQueue_secondCustomer =
        unpaidQueue.withCustomerId(secondCustomer.id()).withCustomer(secondCustomer);
    final Function<QueueModel, QueueModel> notUnpaidQueue_secondCustomer =
        (notUnpaidQueue) ->
            notUnpaidQueue.withCustomerId(secondCustomer.id()).withCustomer(secondCustomer);
    final QueueModel unpaidQueue_doubleOrder =
        unpaidQueue.withProductOrders(List.of(this._order, this._order));
    final Function<QueueModel, QueueModel> notUnpaidQueue_doubleOrder =
        (notUnpaidQueue) -> notUnpaidQueue.withProductOrders(List.of(this._order, this._order));

    assertAll( // spotless:off
        () -> assertEquals(BigDecimal.valueOf(-1000), this._customer.debtOnMadePayment(unpaidQueue), "Add debt when queue unpaid"),
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.ZERO, this._customer.debtOnMadePayment(queue), "Keep debt when queue is not unpaid")),
        () -> assertEquals(BigDecimal.ZERO, secondCustomer.debtOnMadePayment(unpaidQueue), "Keep debt when the customer differs with the one in the queue"),

        () -> assertEquals(BigDecimal.valueOf(1000), this._customer.debtOnRevertedPayment(unpaidQueue), "Revert debt when queue unpaid"),
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.ZERO, this._customer.debtOnRevertedPayment(queue), "Keep debt when queue is not unpaid")),
        () -> assertEquals(BigDecimal.ZERO, secondCustomer.debtOnRevertedPayment(unpaidQueue), "Keep debt when the customer differs with the one in the queue"),

        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.ZERO, this._customer.debtOnUpdatedPayment(queue, queue), "Keep debt when queue unchanged")),
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.valueOf(-1000), this._customer.debtOnUpdatedPayment(queue, unpaidQueue), "Add debt when queue changed to unpaid")),
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.valueOf(1000), this._customer.debtOnUpdatedPayment(unpaidQueue, queue), "Revert debt when queue changed to not unpaid")),
        () -> assertEquals(BigDecimal.ZERO, this._customer.debtOnUpdatedPayment(unpaidQueue, unpaidQueue), "Keep debt when queue unchanged"),
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.ZERO, secondCustomer.debtOnUpdatedPayment(queue, unpaidQueue), "Keep debt when both customer differs")),

        // When the old queue doesn't have customer beforehand.
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.ZERO, this._customer.debtOnUpdatedPayment(unpaidQueue_noCustomer, queue), "Keep debt when old queue with no customer changed to not unpaid")),
        () -> assertEquals(BigDecimal.valueOf(-1000), this._customer.debtOnUpdatedPayment(unpaidQueue_noCustomer, unpaidQueue), "Add debt when old queue with no customer changed to unpaid"),

        // When the queue has different customer.
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.ZERO, secondCustomer.debtOnUpdatedPayment(queue, notUnpaidQueue_secondCustomer.apply(queue)), "Keep debt when queue stays not unpaid with different customer")),
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.valueOf(-1000), secondCustomer.debtOnUpdatedPayment(queue, unpaidQueue_secondCustomer), "Add debt when queue changed to unpaid with different customer")),
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.ZERO, secondCustomer.debtOnUpdatedPayment(unpaidQueue, notUnpaidQueue_secondCustomer.apply(queue)), "Revert debt when queue changed to not unpaid with different customer")),
        () -> assertEquals(BigDecimal.valueOf(-1000), secondCustomer.debtOnUpdatedPayment(unpaidQueue, unpaidQueue_secondCustomer), "Add debt when queue stays unpaid with different customer"),

        // When the queue has more total price of ordered products.
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.ZERO, this._customer.debtOnUpdatedPayment(queue, notUnpaidQueue_doubleOrder.apply(queue)), "Keep debt when queue stays not unpaid with different total price")),
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.valueOf(-2000), this._customer.debtOnUpdatedPayment(queue, unpaidQueue_doubleOrder), "Add debt when queue changed to unpaid with different total price")),
        () -> notUnpaidQueues.forEach(queue -> assertEquals(BigDecimal.valueOf(1000), this._customer.debtOnUpdatedPayment(unpaidQueue, notUnpaidQueue_doubleOrder.apply(queue)), "Revert debt when queue changed to not unpaid with different total price")),
        () -> assertEquals(BigDecimal.valueOf(-1000), this._customer.debtOnUpdatedPayment(unpaidQueue, unpaidQueue_doubleOrder), "Add debt when queue stays unpaid with different total price")
    ); // spotless:on
  }
}
