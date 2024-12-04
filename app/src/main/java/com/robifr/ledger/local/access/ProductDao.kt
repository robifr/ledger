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
import androidx.room.Transaction
import androidx.room.Update
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.local.FtsStringConverter

@Dao
abstract class ProductDao : QueryAccessible<ProductModel> {
  @Transaction
  override fun insert(product: ProductModel): Long {
    val rowId: Long = _insert(product)
    _insertFts(rowId, FtsStringConverter.toFtsSpacedString(product.name))
    return rowId
  }

  @Transaction
  override fun update(product: ProductModel): Int {
    val rowId: Long = selectRowIdById(product.id)
    _deleteFts(rowId)
    val effectedRow: Int = _update(product)
    _insertFts(rowId, FtsStringConverter.toFtsSpacedString(product.name))
    return effectedRow
  }

  @Transaction
  override fun delete(product: ProductModel): Int {
    _deleteFts(selectRowIdById(product.id))
    return _delete(product)
  }

  @Query("SELECT * FROM product") abstract override fun selectAll(): List<ProductModel>

  @Query("SELECT * FROM product WHERE id = :productId")
  abstract override fun selectById(productId: Long?): ProductModel?

  @Transaction
  override fun selectById(productIds: List<Long>): List<ProductModel> =
      productIds.mapNotNull { selectById(it) }

  @Query("SELECT * FROM product WHERE rowid = :rowId")
  abstract override fun selectByRowId(rowId: Long): ProductModel?

  @Query("SELECT id FROM product WHERE rowid = :rowId")
  abstract override fun selectIdByRowId(rowId: Long): Long

  @Query("SELECT rowid FROM product WHERE id = :productId")
  abstract override fun selectRowIdById(productId: Long?): Long

  @Query("SELECT EXISTS(SELECT id FROM product WHERE id = :productId)")
  abstract override fun isExistsById(productId: Long?): Boolean

  @Transaction
  open fun search(query: String): List<ProductModel> {
    val escapedQuery: String = query.replace("\"".toRegex(), "\"\"")
    return _search("*\"${FtsStringConverter.toFtsSpacedString(escapedQuery)}\"*")
  }

  @Insert protected abstract fun _insert(product: ProductModel): Long

  @Update protected abstract fun _update(product: ProductModel): Int

  @Delete protected abstract fun _delete(product: ProductModel): Int

  @Query(
      """
      SELECT * FROM product
      /**
       * Use where-in clause because we don't want the data get override from the FTS field,
       * since the string field is spaced.
       */
      WHERE product.rowid IN (
        SELECT product_fts.rowid FROM product_fts
        WHERE product_fts MATCH :query
      )
      ORDER BY product.name
      """)
  protected abstract fun _search(query: String): List<ProductModel>

  /**
   * Delete product virtual row from FTS table. It should be used before updating or deleting
   * product from the actual table.
   */
  @Query("DELETE FROM product_fts WHERE docid = :rowId")
  protected abstract fun _deleteFts(rowId: Long)

  /**
   * Insert product virtual row into FTS table. It should be used after updating or inserting
   * product from the actual table.
   *
   * @return Inserted row ID.
   */
  @Query("INSERT INTO product_fts(docid, name) VALUES (:rowId, :productName)")
  protected abstract fun _insertFts(rowId: Long, productName: String): Long
}
