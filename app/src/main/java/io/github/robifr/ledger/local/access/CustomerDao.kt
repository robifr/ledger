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

package io.github.robifr.ledger.local.access

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import io.github.robifr.ledger.data.display.CustomerSortMethod
import io.github.robifr.ledger.data.model.CustomerBalanceInfo
import io.github.robifr.ledger.data.model.CustomerDebtInfo
import io.github.robifr.ledger.data.model.CustomerFtsModel
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.CustomerPaginatedInfo
import io.github.robifr.ledger.local.BigDecimalConverter
import io.github.robifr.ledger.local.FtsStringConverter
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

  @Insert protected abstract fun _insert(customer: CustomerModel): Long

  @Transaction
  override fun update(customer: CustomerModel): Int {
    val rowId: Long = selectRowIdById(customer.id)
    _deleteFts(rowId)
    val effectedRow: Int = _update(customer)
    _insertFts(CustomerFtsModel(rowId, customer))
    return effectedRow
  }

  @Update protected abstract fun _update(customer: CustomerModel): Int

  @Transaction
  override fun delete(customerId: Long?): Int {
    _deleteFts(selectRowIdById(customerId))
    return _delete(customerId)
  }

  @Query("DELETE FROM customer WHERE id = :customerId") abstract fun _delete(customerId: Long?): Int

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
      WITH filtered_customers_cte AS (${_CTE_SELECT_PAGINATED_WITH_FILTER})
      SELECT * FROM filtered_customers_cte
      -- Sorting based on the data from `CustomerSortMethod`.
      ORDER BY
          CASE WHEN :sortBy = 'NAME' AND :isAscending IS TRUE
              -- Sort null to the last.
              THEN CASE WHEN filtered_customers_cte.name IS NULL THEN 1 ELSE 0 END
              END ASC,
          CASE WHEN :sortBy = 'NAME' AND :isAscending IS TRUE
              THEN filtered_customers_cte.name END COLLATE NOCASE ASC,

          CASE WHEN :sortBy = 'NAME' AND :isAscending IS FALSE
              -- Sort null to the first.
              THEN CASE WHEN filtered_customers_cte.name IS NULL THEN 0 ELSE 1 END
              END ASC,
          CASE WHEN :sortBy = 'NAME' AND :isAscending IS FALSE
              THEN filtered_customers_cte.name END COLLATE NOCASE DESC,

          CASE WHEN :sortBy = 'BALANCE' AND :isAscending IS TRUE
              THEN filtered_customers_cte.balance END ASC,
          CASE WHEN :sortBy = 'BALANCE' AND :isAscending IS FALSE
              THEN filtered_customers_cte.balance END DESC
      LIMIT :limit OFFSET (:pageNumber - 1) * :itemPerPage
      """)
  @TypeConverters(BigDecimalConverter::class)
  abstract fun selectPaginatedInfoByOffset(
      pageNumber: Int,
      itemPerPage: Int,
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
      WITH filtered_customers_cte AS (${_CTE_SELECT_PAGINATED_WITH_FILTER})
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
  abstract fun selectAllBalanceInfoWithBalance(): List<CustomerBalanceInfo>

  @Query(
      """
      WITH total_debt_cte AS (${_CTE_COUNT_ALL_DEBT})
      SELECT id, debt FROM total_debt_cte WHERE debt < 0
      """)
  @TypeConverters(BigDecimalConverter::class)
  abstract fun selectAllDebtInfoWithDebt(): List<CustomerDebtInfo>

  @Transaction
  open fun search(query: String): List<CustomerModel> {
    val escapedQuery: String = query.replace("\"".toRegex(), "\"\"")
    return _search("*\"${FtsStringConverter.toFtsSpacedString(escapedQuery)}\"*")
  }

  @Query(
      """
      SELECT * FROM customer
      -- Use where-in clause because we don't want the data get overriden
      -- from the FTS field, since the string field is spaced.
      WHERE customer.rowid IN (
        SELECT customer_fts.rowid FROM customer_fts
        WHERE customer_fts MATCH :query
      )
      ORDER BY customer.name
      """)
  protected abstract fun _search(query: String): List<CustomerModel>

  @Query(
      """
      WITH total_debt_cte AS (${_CTE_COUNT_DEBT_BY_ID})
      SELECT IFNULL((SELECT debt FROM total_debt_cte), 0)
      """)
  @TypeConverters(BigDecimalConverter::class)
  abstract fun totalDebtById(customerId: Long?): BigDecimal

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
        SELECT
            queue.customer_id AS id,
            -IFNULL(SUM(ABS(product_order.total_price)), 0) AS debt
        FROM product_order
        JOIN queue ON queue.id = product_order.queue_id
        WHERE
            :customerId IS NOT NULL
            AND queue.customer_id = :customerId
            AND queue.status = 'UNPAID'
        GROUP BY queue.customer_id
        """

    /** Current debt by counting all of product order's total price from all unpaid queues. */
    @Language("RoomSql")
    private const val _CTE_COUNT_ALL_DEBT: String =
        """
        SELECT
            queue.customer_id AS id,
            -IFNULL(SUM(ABS(product_order.total_price)), 0) AS debt
        FROM product_order
        JOIN queue ON queue.id = product_order.queue_id
        WHERE queue.customer_id IS NOT NULL AND queue.status = 'UNPAID'
        GROUP BY queue.customer_id
        """

    @Language("RoomSql")
    private const val _CTE_SELECT_PAGINATED_WITH_FILTER: String =
        """
        SELECT
            customer.id AS id,
            customer.name AS name,
            customer.balance AS balance,
            IFNULL(total_debt_cte.debt, 0) AS debt
        FROM customer
        LEFT JOIN (${_CTE_COUNT_ALL_DEBT}) AS total_debt_cte ON total_debt_cte.id = customer.id
        -- Condition based on the data from `CustomerFilters`.
        WHERE
            -- Filter by balance range.
            (:filteredMinBalance IS NULL OR balance >= :filteredMinBalance)
            AND (:filteredMaxBalance IS NULL OR balance <= :filteredMaxBalance)

            -- Filter by debt range.
            AND (:filteredMinDebt IS NULL OR ABS(debt) >= ABS(CAST(:filteredMinDebt AS NUMERIC)))
            AND (:filteredMaxDebt IS NULL OR ABS(debt) <= ABS(CAST(:filteredMaxDebt AS NUMERIC)))
        """
  }
}
