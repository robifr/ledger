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
import androidx.room.TypeConverters
import androidx.room.Update
import io.github.robifr.ledger.data.display.QueueSortMethod
import io.github.robifr.ledger.data.model.QueueDateInfo
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.data.model.QueuePaginatedInfo
import io.github.robifr.ledger.local.BigDecimalConverter
import io.github.robifr.ledger.local.InstantConverter
import java.math.BigDecimal
import java.time.Instant
import org.intellij.lang.annotations.Language

@Dao
abstract class QueueDao : QueryAccessible<QueueModel> {
  @Insert abstract override fun insert(queue: QueueModel): Long

  @Update abstract override fun update(queue: QueueModel): Int

  @Query("DELETE FROM queue WHERE id = :queueId") abstract override fun delete(queueId: Long?): Int

  @Query("SELECT * FROM queue") abstract override fun selectAll(): List<QueueModel>

  @Query("SELECT * FROM queue WHERE id = :queueId")
  abstract override fun selectById(queueId: Long?): QueueModel?

  @Query("SELECT * FROM queue WHERE id IN (:queueIds)")
  abstract override fun selectById(queueIds: List<Long>): List<QueueModel>

  @Query("SELECT * FROM queue WHERE rowid = :rowId")
  abstract override fun selectByRowId(rowId: Long): QueueModel?

  @Query("SELECT id FROM queue WHERE rowid = :rowId")
  abstract override fun selectIdByRowId(rowId: Long): Long

  @Query("SELECT rowid FROM queue WHERE id = :queueId")
  abstract override fun selectRowIdById(queueId: Long?): Long

  @Query("SELECT EXISTS(SELECT id FROM queue WHERE id = :queueId)")
  abstract override fun isExistsById(queueId: Long?): Boolean

  @Query("SELECT NOT EXISTS(SELECT 1 FROM queue)") abstract override fun isTableEmpty(): Boolean

  @Query(
      """
      WITH filtered_queues_cte AS (${_CTE_SELECT_PAGINATED_WITH_FILTER})
      SELECT * FROM filtered_queues_cte
      -- Sorting based on the data from `QueueSortMethod`.
      ORDER BY
          CASE WHEN :sortBy = 'CUSTOMER_NAME' AND :isAscending IS TRUE
              -- Sort null to the last.
              THEN CASE WHEN filtered_queues_cte.customer_name IS NULL THEN 1 ELSE 0 END
              END ASC,
          CASE WHEN :sortBy = 'CUSTOMER_NAME' AND :isAscending IS TRUE
              THEN filtered_queues_cte.customer_name END COLLATE NOCASE ASC,

          CASE WHEN :sortBy = 'CUSTOMER_NAME' AND :isAscending IS FALSE
              -- Sort null to the first.
              THEN CASE WHEN filtered_queues_cte.customer_name IS NULL THEN 0 ELSE 1 END
              END ASC,
          CASE WHEN :sortBy = 'CUSTOMER_NAME' AND :isAscending IS FALSE
              THEN filtered_queues_cte.customer_name END COLLATE NOCASE DESC,

          CASE WHEN :sortBy = 'DATE' AND :isAscending IS TRUE
              THEN filtered_queues_cte.date END ASC,
          CASE WHEN :sortBy = 'DATE' AND :isAscending IS FALSE
              THEN filtered_queues_cte.date END DESC,
          CASE WHEN :sortBy = 'TOTAL_PRICE' AND :isAscending IS TRUE
              THEN filtered_queues_cte.grand_total_price END ASC,
          CASE WHEN :sortBy = 'TOTAL_PRICE' AND :isAscending IS FALSE
              THEN filtered_queues_cte.grand_total_price END DESC
      """)
  @TypeConverters(BigDecimalConverter::class, InstantConverter::class)
  abstract fun selectAllPaginatedInfo(
      shouldCalculateGrandTotalPrice: Boolean,
      // Sort options from `QueueSortMethod`.
      sortBy: QueueSortMethod.SortBy,
      isAscending: Boolean,
      // Filter options from `QueueFilters`.
      filteredCustomerIds: List<Long>,
      isFilteredCustomerIdsEmpty: Boolean = filteredCustomerIds.isEmpty(),
      isNullCustomerShown: Boolean,
      filteredStatus: Set<QueueModel.Status>,
      filteredMinTotalPrice: BigDecimal?,
      filteredMaxTotalPrice: BigDecimal?,
      filteredDateStart: Instant,
      filteredDateEnd: Instant
  ): List<QueuePaginatedInfo>

  @Query(
      """
      WITH filtered_queues_cte AS (${_CTE_SELECT_PAGINATED_WITH_FILTER})
      SELECT * FROM filtered_queues_cte
      -- Sorting based on the data from `QueueSortMethod`.
      ORDER BY
          CASE WHEN :sortBy = 'CUSTOMER_NAME' AND :isAscending IS TRUE
              -- Sort null to the last.
              THEN CASE WHEN filtered_queues_cte.customer_name IS NULL THEN 1 ELSE 0 END
              END ASC,
          CASE WHEN :sortBy = 'CUSTOMER_NAME' AND :isAscending IS TRUE
              THEN filtered_queues_cte.customer_name END COLLATE NOCASE ASC,

          CASE WHEN :sortBy = 'CUSTOMER_NAME' AND :isAscending IS FALSE
              -- Sort null to the first.
              THEN CASE WHEN filtered_queues_cte.customer_name IS NULL THEN 0 ELSE 1 END
              END ASC,
          CASE WHEN :sortBy = 'CUSTOMER_NAME' AND :isAscending IS FALSE
              THEN filtered_queues_cte.customer_name END COLLATE NOCASE DESC,

          CASE WHEN :sortBy = 'DATE' AND :isAscending IS TRUE
              THEN filtered_queues_cte.date END ASC,
          CASE WHEN :sortBy = 'DATE' AND :isAscending IS FALSE
              THEN filtered_queues_cte.date END DESC,
          CASE WHEN :sortBy = 'TOTAL_PRICE' AND :isAscending IS TRUE
              THEN filtered_queues_cte.grand_total_price END ASC,
          CASE WHEN :sortBy = 'TOTAL_PRICE' AND :isAscending IS FALSE
              THEN filtered_queues_cte.grand_total_price END DESC
      LIMIT :limit OFFSET (:pageNumber - 1) * :itemPerPage
      """)
  @TypeConverters(BigDecimalConverter::class, InstantConverter::class)
  abstract fun selectPaginatedInfoByOffset(
      pageNumber: Int,
      itemPerPage: Int,
      limit: Int,
      shouldCalculateGrandTotalPrice: Boolean,
      // Sort options from `QueueSortMethod`.
      sortBy: QueueSortMethod.SortBy,
      isAscending: Boolean,
      // Filter options from `QueueFilters`.
      filteredCustomerIds: List<Long>,
      isFilteredCustomerIdsEmpty: Boolean = filteredCustomerIds.isEmpty(),
      isNullCustomerShown: Boolean,
      filteredStatus: Set<QueueModel.Status>,
      filteredMinTotalPrice: BigDecimal?,
      filteredMaxTotalPrice: BigDecimal?,
      filteredDateStart: Instant,
      filteredDateEnd: Instant
  ): List<QueuePaginatedInfo>

  @Query(
      """
      WITH filtered_queues_cte AS (${_CTE_SELECT_PAGINATED_WITH_FILTER})
      SELECT COUNT(*) FROM filtered_queues_cte
      """)
  @TypeConverters(BigDecimalConverter::class, InstantConverter::class)
  abstract fun countFilteredQueues(
      shouldCalculateGrandTotalPrice: Boolean,
      // Filter options from `QueueFilters`.
      filteredCustomerIds: List<Long>,
      isFilteredCustomerIdsEmpty: Boolean = filteredCustomerIds.isEmpty(),
      isNullCustomerShown: Boolean,
      filteredStatus: Set<QueueModel.Status>,
      filteredMinTotalPrice: BigDecimal?,
      filteredMaxTotalPrice: BigDecimal?,
      filteredDateStart: Instant,
      filteredDateEnd: Instant
  ): Long

  @Query("SELECT id, date FROM queue WHERE id IN (:queueIds)")
  @TypeConverters(InstantConverter::class)
  abstract fun selectDateInfoById(queueIds: List<Long>): List<QueueDateInfo>

  companion object {
    @Language("RoomSql")
    private const val _CTE_SELECT_PAGINATED_WITH_FILTER: String =
        """
        SELECT
            queue.id AS id,
            queue.customer_id AS customer_id,
            customer.name AS customer_name,
            queue.status AS status,
            queue.date AS date,
            IFNULL(grand_total_price, 0) AS grand_total_price
        FROM queue
        LEFT JOIN (
          -- Calculate grand total price for the sorting and filter operation.
          SELECT
              product_order.queue_id,
              SUM(CAST(product_order.total_price AS NUMERIC)) AS grand_total_price
          FROM product_order
          GROUP BY product_order.queue_id
        ) ON :shouldCalculateGrandTotalPrice IS TRUE AND queue_id = queue.id
        LEFT JOIN customer ON customer.id = queue.customer_id
        -- Condition based on the data from `QueueFilters`.
        WHERE
            -- Filter by customer ID.
            -- When the list of filtered customer ID is empty or the customer ID within the list.
            NOT (queue.customer_id IS NOT NULL
                AND :isFilteredCustomerIdsEmpty IS FALSE
                AND queue.customer_id NOT IN (:filteredCustomerIds))
            -- Or when null customer ID is being allowed.
            AND (queue.customer_id IS NOT NULL OR :isNullCustomerShown IS TRUE)

            -- Filter by status.
            AND queue.status IN (:filteredStatus)

            -- Filter by total price range.
            AND (:filteredMinTotalPrice IS NULL
                OR grand_total_price >= CAST(:filteredMinTotalPrice AS NUMERIC))
            AND (:filteredMaxTotalPrice IS NULL
                OR grand_total_price <= CAST(:filteredMaxTotalPrice AS NUMERIC))

            -- Filter by date range.
            AND (queue.date BETWEEN :filteredDateStart AND :filteredDateEnd)
        """
  }
}
