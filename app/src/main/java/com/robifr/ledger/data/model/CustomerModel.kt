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

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.robifr.ledger.local.access.CustomerDao
import java.math.BigDecimal
import kotlinx.parcelize.Parcelize

/**
 * @property id Product unique ID. Set to null for the value to be auto-generated by Room.
 * @property name Product name.
 * @property balance Product balance.
 * @property debt Customer debt is stored as a negative number, by counting
 *   [ProductOrderModel.totalPrice] from queues whose status is [QueueModel.Status.UNPAID]. Use
 *   [CustomerDao.totalDebtById] to count current total debt.
 * @see Model.id
 */
@Parcelize
@Entity(tableName = "customer")
data class CustomerModel(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") override val id: Long? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "balance") val balance: Long = 0L,
    @Ignore val debt: BigDecimal = 0.toBigDecimal()
) : Model, Parcelable {
  /** Reserved constructor to be used by Room upon querying. */
  constructor(id: Long, name: String, balance: Long) : this(id, name, balance, 0.toBigDecimal())

  /**
   * Check whether balance is sufficient before making a payment. Where current customer belongs to
   * the [newQueue].
   */
  @Ignore
  fun isBalanceSufficient(oldQueue: QueueModel?, newQueue: QueueModel): Boolean {
    if (id != newQueue.customer?.id) return false
    val oldTotalPrice: BigDecimal = oldQueue?.grandTotalPrice() ?: 0.toBigDecimal()
    val originalBalance: BigDecimal =
        // Ensure customer is unchanged when they both exists.
        if (oldQueue?.customerId != null &&
            oldQueue.customerId == id &&
            newQueue.customerId != null &&
            newQueue.customerId == id &&
            oldQueue.status == QueueModel.Status.COMPLETED &&
            oldQueue.paymentMethod == QueueModel.PaymentMethod.ACCOUNT_BALANCE) {
          // Revert balance from old queue to obtain the old balance.
          balance.toBigDecimal() + oldTotalPrice
        } else {
          balance.toBigDecimal()
        }
    return (originalBalance - newQueue.grandTotalPrice()).compareTo(0.toBigDecimal()) >= 0
  }

  /** Calculate balance when customer is assigned to pay a queue. */
  @Ignore
  fun balanceOnMadePayment(queue: QueueModel): Long =
      if (id == queue.customer?.id &&
          queue.status == QueueModel.Status.COMPLETED &&
          queue.paymentMethod == QueueModel.PaymentMethod.ACCOUNT_BALANCE &&
          balance.toBigDecimal().compareTo(queue.grandTotalPrice()) >= 0) {
        (balance.toBigDecimal() - queue.grandTotalPrice()).toLong()
      } else {
        balance
      }

  /**
   * Calculate balance when the queue was changed. Where current customer belongs to the [newQueue].
   */
  @Ignore
  fun balanceOnUpdatedPayment(oldQueue: QueueModel, newQueue: QueueModel): Long {
    // WARNING: If I were you, I would just stay away from this method.
    //    It's critical to ensure the balance always correctly calculated.
    if (id != newQueue.customer?.id) return balance
    val isStatusCompleted: Boolean = newQueue.status == QueueModel.Status.COMPLETED
    val isPaymentCash: Boolean = newQueue.paymentMethod == QueueModel.PaymentMethod.CASH
    val isPaymentAccountBalance: Boolean =
        newQueue.paymentMethod == QueueModel.PaymentMethod.ACCOUNT_BALANCE
    val isTotalPriceChanged: Boolean =
        oldQueue.grandTotalPrice().compareTo(newQueue.grandTotalPrice()) != 0

    val isStatusWasCompleted: Boolean = oldQueue.status == QueueModel.Status.COMPLETED
    val isPaymentWasAccountBalance: Boolean =
        oldQueue.paymentMethod == QueueModel.PaymentMethod.ACCOUNT_BALANCE

    val isOldQueueHaveCustomer = oldQueue.customerId != null && oldQueue.customer != null
    // Customer is SWITCHED when its from non-null to non-null ID only.
    // Don't accept for null to null nor null to non-null ID.
    val isCustomerSwitched: Boolean =
        id != null && oldQueue.customerId != null && id != oldQueue.customerId

    // Deduct balance.
    if (isStatusCompleted && isPaymentAccountBalance) {
      // Case when total price changed while payment method still saved as account balance.
      // We only deduct based on difference between two total prices.
      if (isTotalPriceChanged &&
          isPaymentWasAccountBalance &&
          isStatusWasCompleted &&
          isOldQueueHaveCustomer &&
          !isCustomerSwitched) {
        return (balance.toBigDecimal() +
                oldQueue.grandTotalPrice() - // Revert balance from old queue to obtain old balance.
                newQueue.grandTotalPrice()) // Then subtract to deduct it again with the new one.
            .toLong()
        // Case when status simply switched from uncompleted to completed
        // or when old payment is non account balance,
        // with the new queue payment method marked as account balance.
      } else if (isCustomerSwitched ||
          isTotalPriceChanged ||
          !isOldQueueHaveCustomer ||
          !isStatusWasCompleted ||
          !isPaymentWasAccountBalance) {
        return (balance.toBigDecimal() - newQueue.grandTotalPrice()).toLong()
      }
      // Revert balance.
    } else if (isOldQueueHaveCustomer &&
        isPaymentWasAccountBalance &&
        isStatusWasCompleted &&
        !isCustomerSwitched &&
        // Case when status changed from completed to uncompleted,
        // while payment method still saved as account balance.
        // Or when payment simply switched to cash.
        ((isPaymentAccountBalance && !isStatusCompleted) || isPaymentCash)) {
      return (balance.toBigDecimal() + oldQueue.grandTotalPrice()).toLong()
    }
    return balance
  }

  /** Calculate balance when going to revert the payment, like when deleting queue. */
  @Ignore
  fun balanceOnRevertedPayment(queue: QueueModel): Long =
      if (id == queue.customer?.id &&
          queue.status == QueueModel.Status.COMPLETED &&
          queue.paymentMethod == QueueModel.PaymentMethod.ACCOUNT_BALANCE) {
        (balance.toBigDecimal() + queue.grandTotalPrice()).toLong()
      } else {
        balance
      }

  /** Calculate debt when customer is assigned to pay a queue. */
  @Ignore
  fun debtOnMadePayment(queue: QueueModel): BigDecimal =
      if (id == queue.customer?.id && queue.status == QueueModel.Status.UNPAID) {
        debt - queue.grandTotalPrice()
      } else {
        debt
      }

  /**
   * Calculate debt when the queue was changed. Where current customer belongs to the [newQueue].
   */
  @Ignore
  fun debtOnUpdatedPayment(oldQueue: QueueModel, newQueue: QueueModel): BigDecimal {
    // WARNING: Although debt will always be calculated based on total price of unpaid queues.
    //    It does still important to calculate post-transaction debt for UI stuff.
    //    Just think twice before you do something here. You have been warned.
    if (id != newQueue.customer?.id) return debt
    val isStatusUnpaid: Boolean = newQueue.status == QueueModel.Status.UNPAID
    val isStatusWasUnpaid: Boolean = oldQueue.status == QueueModel.Status.UNPAID
    val isTotalPriceChanged: Boolean =
        oldQueue.grandTotalPrice().compareTo(newQueue.grandTotalPrice()) != 0
    // Customer is CHANGED when its from non-null to non-null or null to non-null ID.
    // Don't accept for null to null ID.
    val isCustomerChanged: Boolean = id != null && id != oldQueue.customerId

    // Revert debt when changing queue status from unpaid to others.
    if (!isStatusUnpaid && isStatusWasUnpaid && (!isCustomerChanged || isTotalPriceChanged)) {
      return debt + oldQueue.grandTotalPrice()
      // Add more debt when changing queue status from others to unpaid.
    } else if (isStatusUnpaid && (!isStatusWasUnpaid || isCustomerChanged)) {
      return debt - newQueue.grandTotalPrice()
      // Add more debt when queue total price changed by
      // calculating difference between old and new total price.
    } else if (isStatusUnpaid && isTotalPriceChanged) {
      return debt + oldQueue.grandTotalPrice() - newQueue.grandTotalPrice()
    }
    return debt
  }

  /** Calculate debt when going to revert the payment, like when deleting queue. */
  @Ignore
  fun debtOnRevertedPayment(queue: QueueModel): BigDecimal =
      if (id == queue.customer?.id && queue.status == QueueModel.Status.UNPAID) {
        debt + queue.grandTotalPrice()
      } else {
        debt
      }
}
