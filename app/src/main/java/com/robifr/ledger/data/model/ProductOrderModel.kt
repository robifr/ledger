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
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.robifr.ledger.local.ColumnConverter.BigDecimalConverter
import com.robifr.ledger.util.Strings
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.parcelize.Parcelize

/**
 * @param id Product order unique ID. Set to null for the value to be auto-generated by Room. See
 *   [Model.modelId] For the reason of why it's boxed type.
 * @param queueId Referenced queue ID from [QueueModel.id].
 * @param productId Referenced product ID from [ProductModel.id].
 * @param productName Referenced product name if [productId] exists from [ProductModel.name].
 * @param productPrice Referenced product price if [productId] exists from [ProductModel.price].
 * @param quantity Product order quantity.
 * @param discount Product order discount.
 * @param totalPrice Product order total price. Use [ProductOrderModel.calculateTotalPrice] to do
 *   the calculation.
 */
@JvmRecord
@Parcelize
@Entity(
    tableName = "product_order",
    foreignKeys =
        [
            ForeignKey(
                entity = QueueModel::class,
                parentColumns = ["id"],
                childColumns = ["queue_id"],
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.CASCADE),
            ForeignKey(
                entity = ProductModel::class,
                parentColumns = ["id"],
                childColumns = ["product_id"],
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["queue_id"]), Index(value = ["product_id"])])
data class ProductOrderModel(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long?,
    @ColumnInfo(name = "queue_id") val queueId: Long?,
    @ColumnInfo(name = "product_id") val productId: Long?,
    @ColumnInfo(name = "product_name") val productName: String?,
    @ColumnInfo(name = "product_price") val productPrice: Long?,
    @ColumnInfo(name = "quantity") val quantity: Double,
    @ColumnInfo(name = "discount") val discount: Long,
    @field:TypeConverters(BigDecimalConverter::class)
    @ColumnInfo(name = "total_price")
    val totalPrice: BigDecimal
) : Model, Parcelable {
  companion object {
    @JvmStatic fun toBuilder(): NewBuild = NewBuilder()

    @JvmStatic
    fun toBuilder(productOrder: ProductOrderModel): EditBuild =
        EditBuilder()
            .setId(productOrder.id)
            .setQueueId(productOrder.queueId)
            .setProductId(productOrder.productId)
            .setProductName(productOrder.productName)
            .setProductPrice(productOrder.productPrice)
            .setQuantity(productOrder.quantity)
            .setDiscount(productOrder.discount)
            .setTotalPrice(productOrder.totalPrice)
  }

  @Ignore override fun modelId(): Long? = this.id

  @Ignore override fun toString(): String = Strings.classToString(this)

  /**
   * @return Referenced product associated with the current product order. [productId] will remains
   *   null if its null, which can occur when the actual product has been deleted from the database.
   *   However, other fields such as [productName] and [productPrice] are retained to preserve
   *   transaction history.
   */
  @Ignore
  fun referencedProduct(): ProductModel? {
    return this.productName?.let {
      this.productPrice?.let { ProductModel(this.productId, this.productName, this.productPrice) }
    }
  }

  @Ignore
  fun discountPercent(): BigDecimal {
    val totalPriceWithoutDiscount: BigDecimal = this.totalPrice + this.discount.toBigDecimal()

    return if (totalPriceWithoutDiscount.compareTo(0.toBigDecimal()) == 0)
        0.toBigDecimal() // Prevent zero division.
    else
        (this.discount.toBigDecimal() * 100.toBigDecimal())
            .divide(totalPriceWithoutDiscount, 2, RoundingMode.UP)
            .stripTrailingZeros()
  }

  @Ignore
  fun calculateTotalPrice(): BigDecimal {
    val totalPrice: BigDecimal =
        this.productPrice
            ?.toBigDecimal()
            ?.multiply(this.quantity.toBigDecimal())
            ?.subtract(this.discount.toBigDecimal()) ?: 0.toBigDecimal()
    return maxOf(0.toBigDecimal(), totalPrice) // Disallow negative total price.
  }

  interface NewBuild {
    fun setId(id: Long?): NewBuild

    fun setQueueId(queueId: Long?): NewBuild

    fun setProductId(productId: Long?): NewBuild

    fun setProductName(productName: String?): NewBuild

    fun setProductPrice(productPrice: Long?): NewBuild

    fun setQuantity(quantity: Double): NewBuild

    fun setDiscount(discount: Long): NewBuild

    fun setTotalPrice(totalPrice: BigDecimal?): NewBuild

    fun build(): ProductOrderModel
  }

  interface EditBuild : NewBuild {
    override fun setId(id: Long?): EditBuild

    override fun setQueueId(queueId: Long?): EditBuild

    override fun setProductId(productId: Long?): EditBuild

    override fun setProductName(productName: String?): EditBuild

    override fun setProductPrice(productPrice: Long?): EditBuild

    override fun setQuantity(quantity: Double): EditBuild

    override fun setDiscount(discount: Long): EditBuild

    override fun setTotalPrice(totalPrice: BigDecimal?): EditBuild
  }

  private abstract class Builder : NewBuild {
    protected var _id: Long? = null
    protected var _queueId: Long? = null
    protected var _productId: Long? = null
    protected var _productName: String? = null
    protected var _productPrice: Long? = null
    protected var _quantity: Double = 0.0
    protected var _discount: Long = 0L
    protected var _totalPrice: BigDecimal? = null

    override fun build(): ProductOrderModel {
      val productOrder: ProductOrderModel =
          ProductOrderModel(
              id = this._id,
              queueId = this._queueId,
              productId = this._productId,
              productName = this._productName,
              productPrice = this._productPrice,
              quantity = this._quantity,
              discount = this._discount,
              totalPrice = 0.toBigDecimal())
      // When total price isn't set, calculate it via `ProductOrderModel#calculateTotalPrice()`.
      val totalPrice: BigDecimal = this._totalPrice ?: productOrder.calculateTotalPrice()

      return productOrder.copy(totalPrice = totalPrice)
    }
  }

  private class NewBuilder : Builder() {
    override fun setId(id: Long?): NewBuild = this.apply { this._id = id }

    override fun setQueueId(queueId: Long?): NewBuild = this.apply { this._queueId = queueId }

    override fun setProductId(productId: Long?): NewBuild =
        this.apply { this._productId = productId }

    override fun setProductName(productName: String?): NewBuild =
        this.apply { this._productName = productName }

    override fun setProductPrice(productPrice: Long?): NewBuild =
        this.apply { this._productPrice = productPrice }

    override fun setQuantity(quantity: Double): NewBuild = this.apply { this._quantity = quantity }

    override fun setDiscount(discount: Long): NewBuild = this.apply { this._discount = discount }

    override fun setTotalPrice(totalPrice: BigDecimal?): NewBuild =
        this.apply { this._totalPrice = totalPrice }
  }

  private class EditBuilder : Builder(), EditBuild {
    override fun setId(id: Long?): EditBuild = this.apply { this._id = id }

    override fun setQueueId(queueId: Long?): EditBuild = this.apply { this._queueId = queueId }

    override fun setProductId(productId: Long?): EditBuild =
        this.apply { this._productId = productId }

    override fun setProductName(productName: String?): EditBuild =
        this.apply { this._productName = productName }

    override fun setProductPrice(productPrice: Long?): EditBuild =
        this.apply { this._productPrice = productPrice }

    override fun setQuantity(quantity: Double): EditBuild = this.apply { this._quantity = quantity }

    override fun setDiscount(discount: Long): EditBuild = this.apply { this._discount = discount }

    override fun setTotalPrice(totalPrice: BigDecimal?): EditBuild =
        this.apply { this._totalPrice = totalPrice }
  }
}
