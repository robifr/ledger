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
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.robifr.ledger.R
import com.robifr.ledger.local.ColumnConverter.InstantConverter
import java.math.BigDecimal
import java.time.Instant
import kotlinx.parcelize.Parcelize

/**
 * @param id Queue unique ID. Set to null for the value to be auto-generated by Room. See
 *   [Model.modelId] For the reason of why it's boxed type.
 * @param customerId Referenced customer ID from [CustomerModel.id].
 * @param status Queue status.
 * @param date Queue date.
 * @param paymentMethod Queue payment method.
 * @param customer Referenced customer instance if [customerId] available.
 * @param productOrders List of referenced ordered products.
 */
@JvmRecord
@Parcelize
@Entity(
    tableName = "queue",
    foreignKeys =
        [
            ForeignKey(
                entity = CustomerModel::class,
                parentColumns = ["id"],
                childColumns = ["customer_id"],
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["customer_id"])])
data class QueueModel(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") override val id: Long? = null,
    @ColumnInfo(name = "customer_id") val customerId: Long? = null,
    @ColumnInfo(name = "status") val status: Status,
    @field:TypeConverters(InstantConverter::class) @ColumnInfo(name = "date") val date: Instant,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod,
    @Ignore val customer: CustomerModel? = null,
    @Ignore val productOrders: List<ProductOrderModel> = listOf()
) : Model, Parcelable {
  /** Reserved constructor to be used by Room upon querying. */
  constructor(
      id: Long?,
      customerId: Long?,
      status: Status,
      date: Instant,
      paymentMethod: PaymentMethod
  ) : this(id, customerId, status, date, paymentMethod, null, listOf())

  @Ignore fun withId(id: Long?): QueueModel = copy(id = id)

  @Ignore fun withCustomerId(customerId: Long?): QueueModel = copy(customerId = customerId)

  @Ignore fun withStatus(status: Status): QueueModel = copy(status = status)

  @Ignore fun withDate(date: Instant): QueueModel = copy(date = date)

  @Ignore
  fun withPaymentMethod(paymentMethod: PaymentMethod): QueueModel =
      copy(paymentMethod = paymentMethod)

  @Ignore fun withCustomer(customer: CustomerModel?): QueueModel = copy(customer = customer)

  @Ignore
  fun withProductOrders(productOrders: List<ProductOrderModel>): QueueModel =
      copy(productOrders = productOrders)

  @Ignore fun grandTotalPrice(): BigDecimal = productOrders.sumOf { it.totalPrice }

  @Ignore fun totalDiscount(): BigDecimal = productOrders.sumOf { it.discount.toBigDecimal() }

  companion object {
    @JvmStatic fun toBuilder(): StatusBuild = Builder()
  }

  enum class Status(
      @StringRes val resourceString: Int,
      @ColorRes val resourceBackgroundColor: Int,
      @ColorRes val resourceTextColor: Int
  ) {
    IN_QUEUE(R.string.enum_queueStatus_inQueue, R.color.light_yellow, R.color.dark_yellow),
    IN_PROCESS(R.string.enum_queueStatus_inProcess, R.color.light_blue, R.color.dark_blue),
    UNPAID(R.string.enum_queueStatus_unpaid, R.color.light_red, R.color.dark_red),
    COMPLETED(R.string.enum_queueStatus_completed, R.color.light_gray, R.color.darker_gray)
  }

  enum class PaymentMethod(@StringRes val resourceString: Int) {
    CASH(R.string.enum_queuePaymentMethod_cash),
    ACCOUNT_BALANCE(R.string.enum_queuePaymentMethod_accountBalance)
  }

  interface StatusBuild {
    fun withStatus(status: Status): DateBuild
  }

  interface DateBuild {
    fun withDate(date: Instant): PaymentMethodBuild
  }

  interface PaymentMethodBuild {
    fun withPaymentMethod(paymentMethod: PaymentMethod): QueueModel
  }

  private class Builder : StatusBuild, DateBuild, PaymentMethodBuild {
    private lateinit var _status: Status
    private lateinit var _date: Instant

    override fun withStatus(status: Status): DateBuild = apply { _status = status }

    override fun withDate(date: Instant): PaymentMethodBuild = apply { _date = date }

    override fun withPaymentMethod(paymentMethod: PaymentMethod): QueueModel =
        QueueModel(status = _status, date = _date, paymentMethod = paymentMethod)
  }
}
