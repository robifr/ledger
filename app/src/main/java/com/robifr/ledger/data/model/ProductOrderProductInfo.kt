/**
 * Copyright 2025 Robi
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

import androidx.room.ColumnInfo

data class ProductOrderProductInfo(
    @ColumnInfo(name = "id") override val id: Long?,
    @ColumnInfo(name = "queue_id") val queueId: Long?,
    @ColumnInfo(name = "product_id") val productId: Long?,
    @ColumnInfo(name = "product_name") val productName: String?,
    @ColumnInfo(name = "product_price") val productPrice: Long?,
    @ColumnInfo(name = "quantity") val quantity: Double
) : Info {
  constructor(
      productOrder: ProductOrderModel
  ) : this(
      productOrder.id,
      productOrder.queueId,
      productOrder.productId,
      productOrder.productName,
      productOrder.productPrice,
      productOrder.quantity)
}
