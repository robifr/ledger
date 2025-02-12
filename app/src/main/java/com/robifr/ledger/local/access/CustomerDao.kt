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
import androidx.room.TypeConverters
import androidx.room.Update
import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerDebtInfo
import com.robifr.ledger.data.model.CustomerFtsModel
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.local.BigDecimalConverter
import com.robifr.ledger.local.FtsStringConverter
import java.math.BigDecimal
import org.intellij.lang.annotations.Language

@Dao
abstract class CustomerDao : QueryAccessible<CustomerModel> {
  @Transaction
  override fun insert(customer: CustomerModel): Long {
    val rowId: Long = _insert(customer)
    _insertFts(CustomerFtsModel(rowId, customer))
    return rowId
  }

  @Transaction
  override fun update(customer: CustomerModel): Int {
    val rowId: Long = selectRowIdById(customer.id)
    _deleteFts(rowId)
    val effectedRow: Int = _update(customer)
    _insertFts(CustomerFtsModel(rowId, customer))
    return effectedRow
  }

  @Transaction
  override fun delete(customer: CustomerModel): Int {
    _deleteFts(selectRowIdById(customer.id))
    return _delete(customer)
  }

  @Query("SELECT * FROM customer") abstract override fun selectAll(): List<CustomerModel>

  @Query("SELECT * FROM customer WHERE id = :customerId")
  abstract override fun selectById(customerId: Long?): CustomerModel?

  @Query("SELECT * FROM customer WHERE id IN (:customerIds)")
  abstract override fun selectById(customerIds: List<Long>): List<CustomerModel>

  @Query("SELECT * FROM customer WHERE rowid = :rowId")
  abstract override fun selectByRowId(rowId: Long): CustomerModel?

  @Query("SELECT id FROM customer WHERE rowid = :rowId")
  abstract override fun selectIdByRowId(rowId: Long): Long

  @Query("SELECT rowid FROM customer WHERE id = :customerId")
  abstract override fun selectRowIdById(customerId: Long?): Long

  @Query("SELECT EXISTS(SELECT id FROM customer WHERE id = :customerId)")
  abstract override fun isExistsById(customerId: Long?): Boolean

  @Query("SELECT NOT EXISTS(SELECT 1 FROM customer)") abstract override fun isTableEmpty(): Boolean

  @Query("SELECT id, balance FROM customer WHERE balance > 0")
  abstract fun selectAllInfoWithBalance(): List<CustomerBalanceInfo>

  @Transaction open fun selectAllInfoWithDebt(): List<CustomerDebtInfo> = _selectAllInfoWithDebt()

  @Query(
      """
      ${_CTE_COUNT_DEBT_BY_ID}
      SELECT id, debt FROM total_debt_cte WHERE debt < 0
      """)
  @TypeConverters(BigDecimalConverter::class)
  protected abstract fun _selectAllInfoWithDebt(customerId: Long? = null): List<CustomerDebtInfo>

  @Transaction
  open fun search(query: String): List<CustomerModel> {
    val escapedQuery: String = query.replace("\"".toRegex(), "\"\"")
    return _search("*\"${FtsStringConverter.toFtsSpacedString(escapedQuery)}\"*")
  }

  @Query(
      """
      ${_CTE_COUNT_DEBT_BY_ID}
      SELECT debt FROM total_debt_cte
      """)
  @TypeConverters(BigDecimalConverter::class)
  abstract fun totalDebtById(customerId: Long?): BigDecimal

  @Insert protected abstract fun _insert(customer: CustomerModel): Long

  @Update protected abstract fun _update(customer: CustomerModel): Int

  @Delete protected abstract fun _delete(customer: CustomerModel): Int

  @Query(
      """
      SELECT * FROM customer
      /**
       * Use where-in clause because we don't want the data get overriden
       * from the FTS field, since the string field is spaced.
       */
      WHERE customer.rowid IN (
        SELECT customer_fts.rowid FROM customer_fts
        WHERE customer_fts MATCH :query
      )
      ORDER BY customer.name
      """)
  protected abstract fun _search(query: String): List<CustomerModel>

  /**
   * Delete customer virtual row from FTS table. It should be used before updating or deleting
   * customer from the actual table.
   */
  @Query("DELETE FROM customer_fts WHERE docid = :rowId")
  protected abstract fun _deleteFts(rowId: Long)

  /**
   * Insert customer virtual row into FTS table. It should be used after updating or inserting
   * customer from the actual table.
   *
   * @return Inserted row ID.
   */
  @Insert protected abstract fun _insertFts(customerFts: CustomerFtsModel): Long

  companion object {
    /** Current debt by counting all of product orders total price from unpaid queues. */
    @Language("RoomSql")
    private const val _CTE_COUNT_DEBT_BY_ID: String =
        """
        WITH total_debt_cte AS (
          SELECT queue.customer_id AS id,
              -IFNULL(SUM(ABS(product_order.total_price)), 0) AS debt
          FROM product_order
          JOIN queue ON queue.id = product_order.queue_id
          WHERE (:customerId IS NULL OR queue.customer_id = :customerId)
              AND queue.status = 'UNPAID'
        )
        """
  }
}
