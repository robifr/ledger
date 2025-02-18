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
import com.robifr.ledger.data.display.ProductSortMethod
import com.robifr.ledger.data.model.ProductFtsModel
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductPaginatedInfo
import com.robifr.ledger.local.BigDecimalConverter
import com.robifr.ledger.local.FtsStringConverter
import org.intellij.lang.annotations.Language

@Dao
abstract class ProductDao : QueryAccessible<ProductModel> {
  @Transaction
  override fun insert(product: ProductModel): Long {
    val rowId: Long = _insert(product)
    _insertFts(ProductFtsModel(rowId, product))
    return rowId
  }

  @Transaction
  override fun update(product: ProductModel): Int {
    val rowId: Long = selectRowIdById(product.id)
    _deleteFts(rowId)
    val effectedRow: Int = _update(product)
    _insertFts(ProductFtsModel(rowId, product))
    return effectedRow
  }

  @Transaction
  override fun delete(productId: Long?): Int {
    _deleteFts(selectRowIdById(productId))
    return _delete(productId)
  }

  @Query("SELECT * FROM product") abstract override fun selectAll(): List<ProductModel>

  @Query("SELECT * FROM product WHERE id = :productId")
  abstract override fun selectById(productId: Long?): ProductModel?

  @Query("SELECT * FROM product WHERE id IN (:productIds)")
  abstract override fun selectById(productIds: List<Long>): List<ProductModel>

  @Query("SELECT * FROM product WHERE rowid = :rowId")
  abstract override fun selectByRowId(rowId: Long): ProductModel?

  @Query("SELECT id FROM product WHERE rowid = :rowId")
  abstract override fun selectIdByRowId(rowId: Long): Long

  @Query("SELECT rowid FROM product WHERE id = :productId")
  abstract override fun selectRowIdById(productId: Long?): Long

  @Query("SELECT EXISTS(SELECT id FROM product WHERE id = :productId)")
  abstract override fun isExistsById(productId: Long?): Boolean

  @Query("SELECT NOT EXISTS(SELECT 1 FROM product)") abstract override fun isTableEmpty(): Boolean

  @Query(
      """
      ${_CTE_SELECT_PAGINATED_WITH_FILTER}
      SELECT * FROM filtered_products_cte
      -- Sorting based on the data from `ProductSortMethod`.
      ORDER BY
          CASE WHEN :sortBy = 'NAME' AND :isAscending IS TRUE
              THEN filtered_products_cte.name END COLLATE NOCASE ASC,
          CASE WHEN :sortBy = 'NAME' AND :isAscending IS FALSE
              THEN filtered_products_cte.name END COLLATE NOCASE DESC,
          CASE WHEN :sortBy = 'PRICE' AND :isAscending IS TRUE
              THEN filtered_products_cte.price END ASC,
          CASE WHEN :sortBy = 'PRICE' AND :isAscending IS FALSE
              THEN filtered_products_cte.price END DESC
      LIMIT :limit OFFSET (:pageNumber - 1) * :limit
      """)
  @TypeConverters(BigDecimalConverter::class)
  abstract fun selectPaginatedInfoByOffset(
      pageNumber: Int,
      limit: Int,
      // Sort options from `ProductSortMethod`.
      sortBy: ProductSortMethod.SortBy,
      isAscending: Boolean,
      // Filter options from `ProductFilters`.
      filteredMinPrice: Long?,
      filteredMaxPrice: Long?
  ): List<ProductPaginatedInfo>

  @Query(
      """
      ${_CTE_SELECT_PAGINATED_WITH_FILTER}
      SELECT COUNT(*) FROM filtered_products_cte
      """)
  @TypeConverters(BigDecimalConverter::class)
  abstract fun countFilteredProducts(
      // Filter options from `ProductFilters`.
      filteredMinPrice: Long?,
      filteredMaxPrice: Long?
  ): Long

  @Transaction
  open fun search(query: String): List<ProductModel> {
    val escapedQuery: String = query.replace("\"".toRegex(), "\"\"")
    return _search("*\"${FtsStringConverter.toFtsSpacedString(escapedQuery)}\"*")
  }

  @Insert protected abstract fun _insert(product: ProductModel): Long

  @Update protected abstract fun _update(product: ProductModel): Int

  @Query("DELETE FROM product WHERE id = :productId") abstract fun _delete(productId: Long?): Int

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
  @Insert protected abstract fun _insertFts(productFts: ProductFtsModel): Long

  companion object {
    @Language("RoomSql")
    private const val _CTE_SELECT_PAGINATED_WITH_FILTER: String =
        """
        WITH filtered_products_cte AS (
          SELECT
              product.id AS id,
              product.name AS name,
              product.price AS price
          FROM product
          -- Condition based on the data from `ProductFilters`.
          WHERE
              -- Filter by price range.
              (:filteredMinPrice IS NULL OR price >= :filteredMinPrice)
              AND (:filteredMaxPrice IS NULL OR price <= :filteredMaxPrice)
        )
        """
  }
}
