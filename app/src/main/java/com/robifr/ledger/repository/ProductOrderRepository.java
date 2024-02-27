/**
 * Copyright (c) 2022-present Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package com.robifr.ledger.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.robifr.ledger.data.model.ProductOrderModel;
import com.robifr.ledger.local.LocalDatabase;
import com.robifr.ledger.local.access.ProductOrderDao;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class ProductOrderRepository
    implements ModelChangedListener.Source<ProductOrderModel>,
        QueryReadable<ProductOrderModel>,
        QueryModifiable<ProductOrderModel> {
  @Nullable private static ProductOrderRepository _instance;
  @NonNull private final ProductOrderDao _localDao;

  @NonNull
  private final HashSet<ModelChangedListener<ProductOrderModel>> _modelChangedListeners =
      new HashSet<>();

  private ProductOrderRepository(@NonNull Context context) {
    Objects.requireNonNull(context);

    this._localDao = LocalDatabase.instance(context.getApplicationContext()).productOrderDao();
  }

  @NonNull
  public static synchronized ProductOrderRepository instance(@NonNull Context context) {
    Objects.requireNonNull(context);

    return ProductOrderRepository._instance =
        ProductOrderRepository._instance == null
            ? new ProductOrderRepository(context)
            : ProductOrderRepository._instance;
  }

  @Override
  public void addModelChangedListener(@NonNull ModelChangedListener<ProductOrderModel> listener) {
    Objects.requireNonNull(listener);

    this._modelChangedListeners.add(listener);
  }

  @Override
  public void removeModelChangedListener(
      @NonNull ModelChangedListener<ProductOrderModel> listener) {
    Objects.requireNonNull(listener);

    this._modelChangedListeners.remove(listener);
  }

  @Override
  public void notifyModelAdded(@NonNull List<ProductOrderModel> productOrders) {
    Objects.requireNonNull(productOrders);

    this._modelChangedListeners.forEach(listeners -> listeners.onModelAdded(productOrders));
  }

  @Override
  public void notifyModelUpdated(@NonNull List<ProductOrderModel> productOrders) {
    Objects.requireNonNull(productOrders);

    this._modelChangedListeners.forEach(listeners -> listeners.onModelUpdated(productOrders));
  }

  @Override
  public void notifyModelDeleted(@NonNull List<ProductOrderModel> productOrders) {
    Objects.requireNonNull(productOrders);

    this._modelChangedListeners.forEach(listeners -> listeners.onModelDeleted(productOrders));
  }

  @Override
  public void notifyModelUpserted(@NonNull List<ProductOrderModel> productOrders) {
    Objects.requireNonNull(productOrders);

    this._modelChangedListeners.forEach(listener -> listener.onModelUpserted(productOrders));
  }

  @Override
  @NonNull
  public CompletableFuture<List<ProductOrderModel>> selectAll() {
    return CompletableFuture.supplyAsync(this._localDao::selectAll);
  }

  @Override
  @NonNull
  public CompletableFuture<ProductOrderModel> selectById(@Nullable Long id) {
    return CompletableFuture.supplyAsync(() -> this._localDao.selectById(id));
  }

  @Override
  @NonNull
  public CompletableFuture<List<ProductOrderModel>> selectById(@NonNull List<Long> ids) {
    Objects.requireNonNull(ids);

    return CompletableFuture.supplyAsync(() -> this._localDao.selectById(ids));
  }

  @Override
  @NonNull
  public CompletableFuture<Boolean> isExistsById(@Nullable Long id) {
    return CompletableFuture.supplyAsync(() -> this._localDao.isExistsById(id));
  }

  @Override
  @NonNull
  public CompletableFuture<Long> add(@NonNull ProductOrderModel productOrder) {
    Objects.requireNonNull(productOrder);

    final CompletableFuture<Long> insert =
        CompletableFuture.supplyAsync(() -> this._localDao.insert(productOrder));

    return insert.thenComposeAsync(
        rowId -> {
          final CompletableFuture<Long> selectId =
              CompletableFuture.supplyAsync(() -> this._localDao.selectIdByRowId(rowId));

          selectId
              .thenComposeAsync(this::selectById)
              .thenAcceptAsync(
                  insertedOrder -> {
                    if (insertedOrder != null) this.notifyModelAdded(List.of(insertedOrder));
                  });
          return selectId;
        });
  }

  /**
   * @return Inserted model IDs. List of null for a failed operation.
   */
  @NonNull
  public CompletableFuture<List<Long>> add(@NonNull List<ProductOrderModel> productOrders) {
    Objects.requireNonNull(productOrders);

    final CompletableFuture<List<Long>> insert =
        CompletableFuture.supplyAsync(() -> this._localDao.insert(productOrders));

    return insert.thenComposeAsync(
        rowIds -> {
          final CompletableFuture<List<Long>> selectIds =
              CompletableFuture.supplyAsync(() -> this._localDao.selectIdByRowId(rowIds));

          selectIds
              .thenComposeAsync(this::selectById)
              .thenAcceptAsync(
                  insertedOrders -> {
                    // Only notify the one with a valid ID and successfully inserted.
                    final List<ProductOrderModel> notifiedOrders =
                        insertedOrders.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    this.notifyModelAdded(notifiedOrders);
                  });
          return selectIds;
        });
  }

  @Override
  @NonNull
  public CompletableFuture<Integer> update(@NonNull ProductOrderModel productOrder) {
    Objects.requireNonNull(productOrder);

    final CompletableFuture<Integer> update =
        CompletableFuture.supplyAsync(() -> this._localDao.update(productOrder));

    update.thenAcceptAsync(
        effected -> {
          if (effected == 0) return;

          this.selectById(productOrder.id())
              .thenAcceptAsync(
                  updatedOrder -> {
                    if (updatedOrder != null) this.notifyModelUpdated(List.of(updatedOrder));
                  });
        });
    return update;
  }

  /**
   * @return Number of row effected. 0 for a failed operation.
   */
  @NonNull
  public CompletableFuture<Integer> update(@NonNull List<ProductOrderModel> productOrders) {
    Objects.requireNonNull(productOrders);

    final List<Long> ids =
        productOrders.stream().map(ProductOrderModel::id).collect(Collectors.toList());
    final CompletableFuture<Integer> update =
        CompletableFuture.supplyAsync(() -> this._localDao.update(productOrders));

    update.thenAcceptAsync(
        effected -> {
          if (effected == 0) return;

          this.selectById(ids)
              .thenAcceptAsync(
                  updatedOrders -> {
                    final List<ProductOrderModel> notifiedOrders =
                        updatedOrders.stream()
                            .filter(Objects::nonNull) // Only notify the one with a valid ID.
                            .collect(Collectors.toList());
                    this.notifyModelUpdated(notifiedOrders);
                  });
        });
    return update;
  }

  @Override
  @NonNull
  public CompletableFuture<Integer> delete(@NonNull ProductOrderModel productOrder) {
    Objects.requireNonNull(productOrder);

    return this.selectById(productOrder.id())
        .thenComposeAsync(
            orderToDelete -> {
              if (orderToDelete == null) return CompletableFuture.completedFuture(0);

              final CompletableFuture<Integer> delete =
                  CompletableFuture.supplyAsync(() -> this._localDao.delete(orderToDelete));

              delete.thenAcceptAsync(
                  effected -> {
                    if (effected > 0) this.notifyModelDeleted(List.of(orderToDelete));
                  });
              return delete;
            });
  }

  /**
   * @return Number of row effected. 0 for a failed operation.
   */
  @NonNull
  public CompletableFuture<Integer> delete(@NonNull List<ProductOrderModel> productOrders) {
    Objects.requireNonNull(productOrders);

    final List<Long> ids =
        productOrders.stream().map(ProductOrderModel::id).collect(Collectors.toList());

    return this.selectById(ids)
        .thenComposeAsync(
            ordersToDelete -> {
              final CompletableFuture<Integer> delete =
                  CompletableFuture.supplyAsync(() -> this._localDao.delete(ordersToDelete));

              delete.thenAcceptAsync(
                  effected -> {
                    if (effected == 0) return;

                    final List<ProductOrderModel> notifiedOrders =
                        ordersToDelete.stream()
                            .filter(Objects::nonNull) // Only notify the one with a valid ID.
                            .collect(Collectors.toList());
                    this.notifyModelDeleted(notifiedOrders);
                  });
              return delete;
            });
  }

  /**
   * @return Upserted product order ID. Null for a failed operation.
   */
  @NonNull
  public CompletableFuture<Long> upsert(@NonNull ProductOrderModel productOrder) {
    Objects.requireNonNull(productOrder);

    final CompletableFuture<Long> upsert =
        CompletableFuture.supplyAsync(() -> this._localDao.upsert(productOrder));

    return upsert.thenComposeAsync(
        rowId -> {
          final CompletableFuture<Long> selectId =
              CompletableFuture.supplyAsync(() -> this._localDao.selectIdByRowId(rowId));

          selectId
              .thenComposeAsync(this::selectById)
              .thenAcceptAsync(
                  upsertedOrder -> {
                    if (upsertedOrder != null) this.notifyModelUpserted(List.of(upsertedOrder));
                  });
          return selectId;
        });
  }

  /**
   * @return Upserted product order IDs. List of null for a failed operation.
   */
  @NonNull
  public CompletableFuture<List<Long>> upsert(@NonNull List<ProductOrderModel> productOrders) {
    Objects.requireNonNull(productOrders);

    final CompletableFuture<List<Long>> upsert =
        CompletableFuture.supplyAsync(() -> this._localDao.upsert(productOrders));

    return upsert.thenComposeAsync(
        rowIds -> {
          final CompletableFuture<List<Long>> selectIds =
              CompletableFuture.supplyAsync(() -> this._localDao.selectIdByRowId(rowIds));

          selectIds
              .thenComposeAsync(this::selectById)
              .thenAcceptAsync(
                  upsertedOrders -> {
                    // Only notify the one with a valid ID and successfully upserted.
                    final List<ProductOrderModel> notifiedOrders =
                        upsertedOrders.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    this.notifyModelUpserted(notifiedOrders);
                  });
          return selectIds;
        });
  }

  @NonNull
  public CompletableFuture<List<ProductOrderModel>> selectAllByQueueId(@Nullable Long queueId) {
    return CompletableFuture.supplyAsync(() -> this._localDao.selectAllByQueueId(queueId));
  }
}
