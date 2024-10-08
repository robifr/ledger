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

package com.robifr.ledger.local.access;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.TypeConverters;
import androidx.room.Update;
import com.robifr.ledger.data.model.CustomerBalanceInfo;
import com.robifr.ledger.data.model.CustomerDebtInfo;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.local.ColumnConverter.BigDecimalConverter;
import com.robifr.ledger.local.ColumnConverter.FtsStringConverter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Dao
public abstract class CustomerDao implements QueryAccessible<CustomerModel> {
  /**
   * @noinspection NullableProblems
   */
  @Override
  @Transaction
  public long insert(@NonNull CustomerModel customer) {
    Objects.requireNonNull(customer);

    final long rowId = this._insert(customer);

    this._insertFts(rowId, FtsStringConverter.toFtsSpacedString(customer.name()));
    return rowId;
  }

  /**
   * @noinspection NullableProblems
   */
  @Override
  @Transaction
  public int update(@NonNull CustomerModel customer) {
    Objects.requireNonNull(customer);

    final long rowId = this.selectRowIdById(customer.id());
    this._deleteFts(rowId);

    final int effectedRow = this._update(customer);

    this._insertFts(rowId, FtsStringConverter.toFtsSpacedString(customer.name()));
    return effectedRow;
  }

  /**
   * @noinspection NullableProblems
   */
  @Override
  @Transaction
  public int delete(@NonNull CustomerModel customer) {
    Objects.requireNonNull(customer);

    this._deleteFts(this.selectRowIdById(customer.id()));
    return this._delete(customer);
  }

  /**
   * @noinspection NullableProblems
   */
  @Override
  @NonNull
  @Query("SELECT * FROM customer")
  public abstract List<CustomerModel> selectAll();

  @Override
  @Nullable
  @Query("SELECT * FROM customer WHERE id = :customerId")
  public abstract CustomerModel selectById(@Nullable Long customerId);

  /**
   * @noinspection NullableProblems
   */
  @Override
  @NonNull
  @Transaction
  public List<CustomerModel> selectById(@NonNull List<Long> customerIds) {
    Objects.requireNonNull(customerIds);

    return customerIds.stream().map(this::selectById).collect(Collectors.toList());
  }

  @Override
  @Nullable
  @Query("SELECT * FROM customer WHERE rowid = :rowId")
  public abstract CustomerModel selectByRowId(long rowId);

  @Override
  @Query("SELECT id FROM customer WHERE rowid = :rowId")
  public abstract long selectIdByRowId(long rowId);

  @Override
  @Query("SELECT rowid FROM customer WHERE id = :customerId")
  public abstract long selectRowIdById(@Nullable Long customerId);

  @Override
  @Query("SELECT EXISTS(SELECT id FROM customer WHERE id = :customerId)")
  public abstract boolean isExistsById(@Nullable Long customerId);

  /**
   * @noinspection NullableProblems
   */
  @NonNull
  @Query("SELECT id, balance FROM customer WHERE balance > 0")
  public abstract List<CustomerBalanceInfo> selectAllInfoWithBalance();

  /**
   * @noinspection NullableProblems
   */
  @NonNull
  @Transaction
  public List<CustomerDebtInfo> selectAllInfoWithDebt() {
    final ArrayList<CustomerDebtInfo> debtInfo = new ArrayList<>();
    final ArrayList<Long> ids = new ArrayList<>(this._selectAllIds());

    for (long id : ids) {
      final BigDecimal debt = this.totalDebtById(id);

      if (debt.compareTo(BigDecimal.ZERO) < 0) debtInfo.add(new CustomerDebtInfo(id, debt));
    }

    return debtInfo;
  }

  /**
   * @noinspection NullableProblems
   */
  @NonNull
  @Transaction
  public List<CustomerModel> search(@NonNull String query) {
    Objects.requireNonNull(query);

    final String escapedQuery = query.replaceAll("\"", "\"\"");
    return this._search("*\"" + FtsStringConverter.toFtsSpacedString(escapedQuery) + "\"*");
  }

  /**
   * @return Current debt by counting all of product orders total price from unpaid queues.
   * @noinspection NullableProblems
   */
  @NonNull
  @Transaction
  public BigDecimal totalDebtById(@Nullable Long customerId) {
    return this._selectUnpaidQueueTotalPrice(customerId).stream()
        .reduce(BigDecimal.ZERO, BigDecimal::subtract);
  }

  /**
   * @noinspection NullableProblems
   */
  @Insert
  protected abstract long _insert(@NonNull CustomerModel customer);

  /**
   * @noinspection NullableProblems
   */
  @Update
  protected abstract int _update(@NonNull CustomerModel customer);

  /**
   * @noinspection NullableProblems
   */
  @Delete
  protected abstract int _delete(@NonNull CustomerModel customer);

  /**
   * @noinspection NullableProblems
   */
  @NonNull
  @Query("SELECT id FROM customer")
  protected abstract List<Long> _selectAllIds();

  /**
   * @noinspection NullableProblems
   */
  @NonNull
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
  protected abstract List<CustomerModel> _search(@NonNull String query);

  /**
   * @noinspection NullableProblems
   */
  @NonNull
  @Query(
      """
      SELECT product_order.total_price FROM product_order
      WHERE product_order.queue_id = (
        SELECT queue.id FROM queue
        WHERE queue.id = product_order.queue_id
            AND queue.customer_id = :customerId
            AND queue.status == 'UNPAID'
      )
      """)
  @TypeConverters(BigDecimalConverter.class)
  protected abstract List<BigDecimal> _selectUnpaidQueueTotalPrice(@Nullable Long customerId);

  /**
   * Delete customer virtual row from FTS table. It should be used before updating or deleting
   * customer from the actual table.
   */
  @Query("DELETE FROM customer_fts WHERE docid = :rowId")
  protected abstract void _deleteFts(long rowId);

  /**
   * Insert customer virtual row into FTS table. It should be used after updating or inserting
   * customer from the actual table.
   *
   * @return Inserted row ID.
   * @noinspection NullableProblems
   */
  @Query("INSERT INTO customer_fts(docid, name) VALUES (:rowId, :customerName)")
  protected abstract long _insertFts(long rowId, @NonNull String customerName);
}
