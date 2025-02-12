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

package com.robifr.ledger.local.access

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.robifr.ledger.data.model.ProductOrderModel

@Dao
abstract class ProductOrderDao : QueryAccessible<ProductOrderModel> {
  @Insert abstract override fun insert(productOrder: ProductOrderModel): Long

  /** @return List of inserted row ID. Empty list for a failed operation. */
  @Insert abstract fun insert(productOrders: List<ProductOrderModel>): List<Long>

  @Update abstract override fun update(productOrder: ProductOrderModel): Int

  /** @return Number of row effected. */
  @Update abstract fun update(productOrders: List<ProductOrderModel>): Int

  @Delete abstract override fun delete(productOrderId: Long?): Int

  /** @return Number of row effected. */
  @Delete abstract fun delete(productOrders: List<ProductOrderModel>): Int

  @Query("SELECT * FROM product_order") abstract override fun selectAll(): List<ProductOrderModel>

  @Query("SELECT * FROM product_order WHERE id = :productOrderId")
  abstract override fun selectById(productOrderId: Long?): ProductOrderModel?

  @Query("SELECT * FROM product_order WHERE id IN (:productOrderIds)")
  abstract override fun selectById(productOrderIds: List<Long>): List<ProductOrderModel>

  @Query("SELECT * FROM product_order WHERE rowid = :rowId")
  abstract override fun selectByRowId(rowId: Long): ProductOrderModel?

  @Query("SELECT * FROM product_order WHERE rowid IN (:rowIds)")
  abstract fun selectByRowId(rowIds: List<Long>): List<ProductOrderModel>

  @Query("SELECT id FROM product_order WHERE rowid = :rowId")
  abstract override fun selectIdByRowId(rowId: Long): Long

  @Query("SELECT id FROM product_order WHERE rowid IN (:rowIds)")
  abstract fun selectIdByRowId(rowIds: List<Long>): List<Long>

  @Query("SELECT rowid FROM product_order WHERE id = :productOrderId")
  abstract override fun selectRowIdById(productOrderId: Long?): Long

  @Query("SELECT EXISTS(SELECT id FROM product_order WHERE id = :productOrderId)")
  abstract override fun isExistsById(productOrderId: Long?): Boolean

  @Query("SELECT NOT EXISTS(SELECT 1 FROM product_order)")
  abstract override fun isTableEmpty(): Boolean

  /** @return Upserted row ID. -1 for a failed insert operation. */
  @Upsert abstract fun upsert(productOrder: ProductOrderModel): Long

  /** @return Upserted row IDs. Empty list for a failed insert operation. */
  @Upsert abstract fun upsert(productOrders: List<ProductOrderModel>): List<Long>

  @Query("SELECT * FROM product_order WHERE queue_id = :queueId")
  abstract fun selectAllByQueueId(queueId: Long?): List<ProductOrderModel>
}
