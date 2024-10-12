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

package com.robifr.ledger.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.robifr.ledger.data.model.CustomerBalanceInfo;
import com.robifr.ledger.data.model.CustomerDebtInfo;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.local.LocalDatabase;
import com.robifr.ledger.local.access.CustomerDao;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class CustomerRepository
    implements ModelChangedListener.Source<CustomerModel>,
        QueryReadable<CustomerModel>,
        QueryModifiable<CustomerModel> {
  @Nullable private static CustomerRepository _instance;
  @NonNull private final CustomerDao _localDao;

  @NonNull
  private final HashSet<ModelChangedListener<CustomerModel>> _modelChangedListeners =
      new HashSet<>();

  private CustomerRepository(@NonNull Context context) {
    Objects.requireNonNull(context);

    this._localDao = LocalDatabase.instance(context.getApplicationContext()).customerDao();
  }

  @NonNull
  public static synchronized CustomerRepository instance(@NonNull Context context) {
    Objects.requireNonNull(context);

    return CustomerRepository._instance =
        CustomerRepository._instance == null
            ? new CustomerRepository(context)
            : CustomerRepository._instance;
  }

  @Override
  public void addModelChangedListener(@NonNull ModelChangedListener<CustomerModel> listener) {
    Objects.requireNonNull(listener);

    this._modelChangedListeners.add(listener);
  }

  @Override
  public void removeModelChangedListener(@NonNull ModelChangedListener<CustomerModel> listener) {
    Objects.requireNonNull(listener);

    this._modelChangedListeners.remove(listener);
  }

  @Override
  public void notifyModelAdded(@NonNull List<CustomerModel> customers) {
    Objects.requireNonNull(customers);

    this._modelChangedListeners.forEach(listener -> listener.onModelAdded(customers));
  }

  @Override
  public void notifyModelUpdated(@NonNull List<CustomerModel> customers) {
    Objects.requireNonNull(customers);

    this._modelChangedListeners.forEach(listener -> listener.onModelUpdated(customers));
  }

  @Override
  public void notifyModelDeleted(@NonNull List<CustomerModel> customers) {
    Objects.requireNonNull(customers);

    this._modelChangedListeners.forEach(listener -> listener.onModelDeleted(customers));
  }

  @Override
  public void notifyModelUpserted(@NonNull List<CustomerModel> customers) {
    Objects.requireNonNull(customers);

    this._modelChangedListeners.forEach(listener -> listener.onModelUpserted(customers));
  }

  @Override
  @NonNull
  public CompletableFuture<List<CustomerModel>> selectAll() {
    return CompletableFuture.supplyAsync(this._localDao::selectAll)
        .thenComposeAsync(this::_mapFields);
  }

  @Override
  @NonNull
  public CompletableFuture<CustomerModel> selectById(@Nullable Long id) {
    return CompletableFuture.supplyAsync(() -> this._localDao.selectById(id))
        .thenComposeAsync(this::_mapFields);
  }

  @Override
  @NonNull
  public CompletableFuture<List<CustomerModel>> selectById(@NonNull List<Long> ids) {
    Objects.requireNonNull(ids);

    return CompletableFuture.supplyAsync(() -> this._localDao.selectById(ids))
        .thenComposeAsync(this::_mapFields);
  }

  @Override
  @NonNull
  public CompletableFuture<Boolean> isExistsById(@Nullable Long id) {
    return CompletableFuture.supplyAsync(() -> this._localDao.isExistsById(id));
  }

  @Override
  @NonNull
  public CompletableFuture<Long> add(@NonNull CustomerModel customer) {
    Objects.requireNonNull(customer);

    final CompletableFuture<Long> insert =
        CompletableFuture.supplyAsync(() -> this._localDao.insert(customer))
            .thenComposeAsync(
                rowId ->
                    CompletableFuture.supplyAsync(() -> this._localDao.selectIdByRowId(rowId)));

    return insert.thenComposeAsync(
        insertedCustomerId -> {
          this.selectById(insertedCustomerId)
              .thenAcceptAsync(
                  insertedCustomer -> {
                    if (insertedCustomer != null) this.notifyModelAdded(List.of(insertedCustomer));
                  });
          return CompletableFuture.completedFuture(insertedCustomerId);
        });
  }

  @Override
  @NonNull
  public CompletableFuture<Integer> update(@NonNull CustomerModel customer) {
    Objects.requireNonNull(customer);

    final CompletableFuture<Integer> update =
        CompletableFuture.supplyAsync(() -> this._localDao.update(customer));

    update.thenAcceptAsync(
        effected -> {
          if (effected == 0) return;

          this.selectById(customer.id())
              .thenAcceptAsync(
                  updatedCustomer -> {
                    if (updatedCustomer != null) this.notifyModelUpdated(List.of(updatedCustomer));
                  });
        });
    return update;
  }

  @Override
  @NonNull
  public CompletableFuture<Integer> delete(@NonNull CustomerModel customer) {
    Objects.requireNonNull(customer);

    // Note: `customer_id` on `queue` table will automatically set to null upon customer deletion.
    return this.selectById(customer.id())
        .thenComposeAsync(
            customerToDelete -> {
              if (customerToDelete == null) return CompletableFuture.completedFuture(0);

              final CompletableFuture<Integer> delete =
                  CompletableFuture.supplyAsync(() -> this._localDao.delete(customerToDelete));

              delete.thenAcceptAsync(
                  effected -> {
                    if (effected > 0) this.notifyModelDeleted(List.of(customerToDelete));
                  });
              return delete;
            });
  }

  @NonNull
  public CompletableFuture<List<CustomerModel>> search(@NonNull String query) {
    Objects.requireNonNull(query);

    return CompletableFuture.supplyAsync(() -> this._localDao.search(query))
        .thenComposeAsync(this::_mapFields);
  }

  @NonNull
  public CompletableFuture<List<CustomerBalanceInfo>> selectAllInfoWithBalance() {
    return CompletableFuture.supplyAsync(this._localDao::selectAllInfoWithBalance);
  }

  @NonNull
  public CompletableFuture<List<CustomerDebtInfo>> selectAllInfoWithDebt() {
    return CompletableFuture.supplyAsync(this._localDao::selectAllInfoWithDebt);
  }

  /**
   * Specifically used when query returning object model, mostly select query. Like when {@link
   * CustomerModel} consisting {@link CustomerModel#debt()} field, which can only be obtained from
   * database. We have to make sure those field mapped into it.
   */
  @NonNull
  private CompletableFuture<CustomerModel> _mapFields(@Nullable CustomerModel customer) {
    if (customer == null) return CompletableFuture.completedFuture(null);

    final CompletableFuture<BigDecimal> countDebt =
        CompletableFuture.supplyAsync(() -> this._localDao.totalDebtById(customer.id()));

    return countDebt.thenApplyAsync(customer::withDebt);
  }

  /**
   * @see #_mapFields(CustomerModel)
   */
  @NonNull
  private CompletableFuture<List<CustomerModel>> _mapFields(
      @Nullable List<CustomerModel> customers) {
    if (customers == null) return CompletableFuture.completedFuture(null);

    final List<CompletableFuture<CustomerModel>> mapFields =
        customers.stream().map(this::_mapFields).collect(Collectors.toList());

    return CompletableFuture.allOf(mapFields.stream().toArray(CompletableFuture[]::new))
        .thenApplyAsync(
            ignore -> mapFields.stream().map(CompletableFuture::join).collect(Collectors.toList()));
  }
}
