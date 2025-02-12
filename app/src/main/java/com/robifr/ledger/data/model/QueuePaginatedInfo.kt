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
import androidx.room.Ignore
import androidx.room.TypeConverters
import com.robifr.ledger.data.model.QueueModel.Status
import com.robifr.ledger.local.BigDecimalConverter
import com.robifr.ledger.local.InstantConverter
import java.math.BigDecimal
import java.time.Instant

data class QueuePaginatedInfo(
    @ColumnInfo(name = "id") override val id: Long?,
    @ColumnInfo(name = "customer_id") val customerId: Long?,
    @ColumnInfo(name = "customer_name") val customerName: String?,
    @ColumnInfo(name = "status") val status: Status,
    @ColumnInfo(name = "date") @field:TypeConverters(InstantConverter::class) val date: Instant,
    @ColumnInfo(name = "grand_total_price")
    @field:TypeConverters(BigDecimalConverter::class)
    val grandTotalPrice: BigDecimal,
    @Ignore val fullModel: QueueModel? = null
) : Info {
  /** Reserved constructor to be used by Room upon querying. */
  constructor(
      id: Long?,
      customerId: Long?,
      customerName: String?,
      status: Status,
      date: Instant,
      grandTotalPrice: BigDecimal
  ) : this(id, customerId, customerName, status, date, grandTotalPrice, null)

  constructor(
      queue: QueueModel
  ) : this(
      queue.id,
      queue.customerId,
      queue.customer?.name,
      queue.status,
      queue.date,
      queue.grandTotalPrice(),
      queue)
}
