/**
 * Copyright (c) 2024 Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
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
import com.robifr.ledger.util.Strings
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
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long?,
    @ColumnInfo(name = "customer_id") val customerId: Long?,
    @ColumnInfo(name = "status") val status: Status,
    @field:TypeConverters(InstantConverter::class) @ColumnInfo(name = "date") val date: Instant,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod,
    @Ignore val customer: CustomerModel?,
    @Ignore val productOrders: List<ProductOrderModel>
) : Model, Parcelable {

  enum class Status(
      @get:JvmName("resourceString") @StringRes val resourceString: Int,
      @get:JvmName("resourceBackgroundColor") @ColorRes val resourceBackgroundColor: Int,
      @get:JvmName("resourceTextColor") @ColorRes val resourceTextColor: Int
  ) {
    IN_QUEUE(R.string.text_in_queue, R.color.light_yellow, R.color.dark_yellow),
    IN_PROCESS(R.string.text_in_process, R.color.light_blue, R.color.dark_blue),
    UNPAID(R.string.text_unpaid, R.color.light_red, R.color.dark_red),
    COMPLETED(R.string.text_completed, R.color.light_gray, R.color.darker_gray)
  }

  enum class PaymentMethod(@get:JvmName("resourceString") @StringRes val resourceString: Int) {
    CASH(R.string.text_cash),
    ACCOUNT_BALANCE(R.string.text_account_balance)
  }

  /** Reserved constructor to be used by Room upon querying. */
  constructor(
      id: Long?,
      customerId: Long?,
      status: Status,
      date: Instant,
      paymentMethod: PaymentMethod
  ) : this(id, customerId, status, date, paymentMethod, null, ArrayList())

  companion object {
    @JvmStatic fun toBuilder(): StatusBuild = NewBuilder()

    @JvmStatic
    fun toBuilder(queue: QueueModel): EditBuild =
        EditBuilder()
            .setId(queue.id)
            .setCustomerId(queue.customerId)
            .setStatus(queue.status)
            .setDate(queue.date)
            .setPaymentMethod(queue.paymentMethod)
            .setCustomer(queue.customer)
            .setProductOrders(queue.productOrders)
  }

  @Ignore override fun modelId(): Long? = this.id

  @Ignore override fun toString(): String = Strings.classToString(this)

  @Ignore fun grandTotalPrice(): BigDecimal = this.productOrders.sumOf { it.totalPrice }

  @Ignore fun totalDiscount(): BigDecimal = this.productOrders.sumOf { it.discount.toBigDecimal() }

  interface StatusBuild {
    fun setStatus(status: Status): DateBuild
  }

  interface DateBuild {
    fun setDate(date: Instant): PaymentMethodBuild
  }

  interface PaymentMethodBuild {
    fun setPaymentMethod(paymentMethod: PaymentMethod): NewBuild
  }

  interface NewBuild {
    fun setId(id: Long?): NewBuild

    fun setCustomerId(customerId: Long?): NewBuild

    fun setCustomer(customer: CustomerModel?): NewBuild

    fun setProductOrders(productOrders: List<ProductOrderModel>): NewBuild

    fun build(): QueueModel
  }

  interface EditBuild : NewBuild {
    override fun setId(id: Long?): EditBuild

    override fun setCustomerId(customerId: Long?): EditBuild

    override fun setCustomer(customer: CustomerModel?): EditBuild

    override fun setProductOrders(productOrders: List<ProductOrderModel>): EditBuild

    fun setStatus(status: Status): EditBuild

    fun setDate(date: Instant): EditBuild

    fun setPaymentMethod(paymentMethod: PaymentMethod): EditBuild
  }

  private abstract class Builder : NewBuild {
    protected lateinit var _status: Status
    protected lateinit var _date: Instant
    protected lateinit var _paymentMethod: PaymentMethod
    protected var _id: Long? = null
    protected var _customerId: Long? = null
    protected var _customer: CustomerModel? = null
    protected var _productOrders: List<ProductOrderModel> = ArrayList()

    override fun build(): QueueModel =
        QueueModel(
            id = this._id,
            customerId = this._customerId,
            status = this._status,
            date = this._date,
            paymentMethod = this._paymentMethod,
            customer = this._customer,
            productOrders = this._productOrders)
  }

  private class NewBuilder : Builder(), StatusBuild, DateBuild, PaymentMethodBuild {
    override fun setStatus(status: Status): DateBuild = this.apply { this._status = status }

    override fun setDate(date: Instant): PaymentMethodBuild = this.apply { this._date = date }

    override fun setPaymentMethod(paymentMethod: PaymentMethod): NewBuild =
        this.apply { this._paymentMethod = paymentMethod }

    override fun setId(id: Long?): NewBuild = this.apply { this._id = id }

    override fun setCustomerId(customerId: Long?): NewBuild =
        this.apply { this._customerId = customerId }

    override fun setCustomer(customer: CustomerModel?): NewBuild =
        this.apply { this._customer = customer }

    override fun setProductOrders(productOrders: List<ProductOrderModel>): NewBuild =
        this.apply { this._productOrders = productOrders }
  }

  private class EditBuilder : Builder(), EditBuild {
    override fun setStatus(status: Status): EditBuild = this.apply { this._status = status }

    override fun setDate(date: Instant): EditBuild = this.apply { this._date = date }

    override fun setPaymentMethod(paymentMethod: PaymentMethod): EditBuild =
        this.apply { this._paymentMethod = paymentMethod }

    override fun setId(id: Long?): EditBuild = this.apply { this._id = id }

    override fun setCustomerId(customerId: Long?): EditBuild =
        this.apply { this._customerId = customerId }

    override fun setCustomer(customer: CustomerModel?): EditBuild =
        this.apply { this._customer = customer }

    override fun setProductOrders(productOrders: List<ProductOrderModel>): EditBuild =
        this.apply { this._productOrders = productOrders }
  }
}
