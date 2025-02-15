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
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import com.robifr.ledger.data.display.CustomerSortMethod
import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerDebtInfo
import com.robifr.ledger.data.model.CustomerFtsModel
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.CustomerPaginatedInfo
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
  override fun delete(customerId: Long?): Int {
    _deleteFts(selectRowIdById(customerId))
    return _delete(customerId)
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

  @Query(
      """
      ${_CTE_SELECT_PAGINATED_WITH_FILTER}
      SELECT * FROM filtered_customers_cte
      -- Sorting based on the data from `CustomerSortMethod`.
      ORDER BY
          CASE WHEN :sortBy = 'NAME' AND :isAscending IS TRUE
              THEN filtered_customers_cte.name END COLLATE NOCASE ASC,
          CASE WHEN :sortBy = 'NAME' AND :isAscending IS FALSE
              THEN filtered_customers_cte.name END COLLATE NOCASE DESC,
          CASE WHEN :sortBy = 'BALANCE' AND :isAscending IS TRUE
              THEN filtered_customers_cte.balance END ASC,
          CASE WHEN :sortBy = 'BALANCE' AND :isAscending IS FALSE
              THEN filtered_customers_cte.balance END DESC
      LIMIT :limit OFFSET (:pageNumber - 1) * :limit
      """)
  @TypeConverters(BigDecimalConverter::class)
  abstract fun selectByPageOffset(
      pageNumber: Int,
      limit: Int,
      // Sort options from `CustomerSortMethod`.
      sortBy: CustomerSortMethod.SortBy,
      isAscending: Boolean,
      // Filter options from `CustomerFilters`.
      filteredMinBalance: Long?,
      filteredMaxBalance: Long?,
      filteredMinDebt: BigDecimal?,
      filteredMaxDebt: BigDecimal?
  ): List<CustomerPaginatedInfo>

  @Query(
      """
      ${_CTE_SELECT_PAGINATED_WITH_FILTER}
      SELECT COUNT(*) FROM filtered_customers_cte
      """)
  @TypeConverters(BigDecimalConverter::class)
  abstract fun countFilteredCustomers(
      // Filter options from `CustomerFilters`.
      filteredMinBalance: Long?,
      filteredMaxBalance: Long?,
      filteredMinDebt: BigDecimal?,
      filteredMaxDebt: BigDecimal?
  ): Long

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

  @Query("DELETE FROM customer WHERE id = :customerId") abstract fun _delete(customerId: Long?): Int

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
    /** Current debt by counting all of product order's total price from unpaid queues. */
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

    @Language("RoomSql")
    private const val _CTE_SELECT_PAGINATED_WITH_FILTER: String =
        """
        WITH filtered_customers_cte AS (
          SELECT
              customer.id AS id,
              customer.name AS name,
              customer.balance AS balance,
              IFNULL(debt, 0) AS debt
          FROM customer
          LEFT JOIN (
            -- Count customer debt based from product order's total price with unpaid queue status.
            SELECT
                queue.customer_id,
                -IFNULL(SUM(ABS(product_order.total_price)), 0) AS debt
            FROM product_order
            JOIN queue ON queue.id = product_order.queue_id
            WHERE queue.customer_id IS NOT NULL AND queue.status = 'UNPAID'
            GROUP BY queue.customer_id
          ) ON customer_id = customer.id
          -- Condition based on the data from `CustomerFilters`.
          WHERE
              -- Filter by balance range.
              (:filteredMinBalance IS NULL OR balance >= :filteredMinBalance)
              AND (:filteredMaxBalance IS NULL OR balance <= :filteredMaxBalance)

              -- Filter by debt range.
              AND (:filteredMinDebt IS NULL OR ABS(debt) >= ABS(CAST(:filteredMinDebt AS NUMERIC)))
              AND (:filteredMaxDebt IS NULL OR ABS(debt) <= ABS(CAST(:filteredMaxDebt AS NUMERIC)))
        )
        """
  }
}
