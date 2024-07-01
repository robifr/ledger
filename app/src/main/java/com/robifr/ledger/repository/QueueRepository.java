/**
 * Copyright (c) 2024 Robi
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
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.data.model.ProductOrderModel;
import com.robifr.ledger.data.model.QueueModel;
import com.robifr.ledger.local.LocalDatabase;
import com.robifr.ledger.local.access.QueueDao;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class QueueRepository
    implements ModelChangedListener.Source<QueueModel>,
        QueryReadable<QueueModel>,
        QueryModifiable<QueueModel> {
  @Nullable private static QueueRepository _instance;
  @NonNull private final QueueDao _localDao;
  @NonNull private final CustomerRepository _customerRepository;
  @NonNull private final ProductOrderRepository _productOrderRepository;

  @NonNull
  private final HashSet<ModelChangedListener<QueueModel>> _modelChangedListeners = new HashSet<>();

  private QueueRepository(@NonNull Context context) {
    Objects.requireNonNull(context);

    this._localDao = LocalDatabase.instance(context.getApplicationContext()).queueDao();
    this._customerRepository = CustomerRepository.instance(context.getApplicationContext());
    this._productOrderRepository = ProductOrderRepository.instance(context.getApplicationContext());
  }

  @NonNull
  public static synchronized QueueRepository instance(@NonNull Context context) {
    Objects.requireNonNull(context);

    return QueueRepository._instance =
        QueueRepository._instance == null
            ? new QueueRepository(context)
            : QueueRepository._instance;
  }

  @Override
  public void addModelChangedListener(@NonNull ModelChangedListener<QueueModel> listener) {
    Objects.requireNonNull(listener);

    this._modelChangedListeners.add(listener);
  }

  @Override
  public void removeModelChangedListener(@NonNull ModelChangedListener<QueueModel> listener) {
    Objects.requireNonNull(listener);

    this._modelChangedListeners.remove(listener);
  }

  @Override
  public void notifyModelAdded(@NonNull List<QueueModel> queues) {
    Objects.requireNonNull(queues);

    this._modelChangedListeners.forEach(listener -> listener.onModelAdded(queues));
  }

  @Override
  public void notifyModelUpdated(@NonNull List<QueueModel> queues) {
    Objects.requireNonNull(queues);

    this._modelChangedListeners.forEach(listener -> listener.onModelUpdated(queues));
  }

  @Override
  public void notifyModelDeleted(@NonNull List<QueueModel> queues) {
    Objects.requireNonNull(queues);

    this._modelChangedListeners.forEach(listener -> listener.onModelDeleted(queues));
  }

  @Override
  public void notifyModelUpserted(@NonNull List<QueueModel> queues) {
    Objects.requireNonNull(queues);

    this._modelChangedListeners.forEach(listener -> listener.onModelUpserted(queues));
  }

  @Override
  @NonNull
  public CompletableFuture<List<QueueModel>> selectAll() {
    return CompletableFuture.supplyAsync(this._localDao::selectAll)
        .thenComposeAsync(this::_mapFields);
  }

  @Override
  @NonNull
  public CompletableFuture<QueueModel> selectById(@Nullable Long id) {
    return CompletableFuture.supplyAsync(() -> this._localDao.selectById(id))
        .thenComposeAsync(this::_mapFields);
  }

  @Override
  @NonNull
  public CompletableFuture<List<QueueModel>> selectById(@NonNull List<Long> ids) {
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
  public CompletableFuture<Long> add(@NonNull QueueModel queue) {
    Objects.requireNonNull(queue);

    final BiFunction<List<ProductOrderModel>, Long, CompletableFuture<List<Long>>>
        insertProductOrders =
            (productOrders, queueId) -> {
              final List<ProductOrderModel> orders =
                  productOrders.stream()
                      .map(order -> ProductOrderModel.toBuilder(order).setQueueId(queueId).build())
                      .collect(Collectors.toList());
              return this._productOrderRepository.add(orders);
            };

    final Function<QueueModel, CompletableFuture<Integer>> updateCustomer =
        insertedQueue ->
            this._customerRepository
                .selectById(insertedQueue.customerId())
                .thenComposeAsync(
                    selectedCustomer -> {
                      // Make customer pay the already inserted queue.
                      final CustomerModel customer =
                          CustomerModel.toBuilder(selectedCustomer)
                              .setBalance(selectedCustomer.balanceOnMadePayment(insertedQueue))
                              .build();
                      return this._customerRepository.update(customer);
                    });

    final CompletableFuture<Long> insert =
        CompletableFuture.supplyAsync(() -> this._localDao.insert(queue))
            .thenComposeAsync(
                rowId ->
                    CompletableFuture.supplyAsync(() -> this._localDao.selectIdByRowId(rowId)));

    return insert.thenComposeAsync(
        insertedQueueId -> {
          if (insertedQueueId == 0L) return CompletableFuture.completedFuture(0L);

          final CompletableFuture<QueueModel> updateForeign =
              insertProductOrders
                  .apply(queue.productOrders(), insertedQueueId)
                  // First select query to get a queue mapped with newly inserted orders.
                  .thenComposeAsync(ignore -> this.selectById(insertedQueueId))
                  .thenAcceptAsync(updateCustomer::apply)
                  // Re-select to get a queue mapped with an updated customer.
                  .thenComposeAsync(ignore -> this.selectById(insertedQueueId));

          // Update foreign column firstly, so that queue
          // already provided with an updated value when notified.
          updateForeign.thenAcceptAsync(
              updatedQueue -> {
                if (updatedQueue != null) this.notifyModelAdded(List.of(updatedQueue));
              });
          return CompletableFuture.completedFuture(insertedQueueId);
        });
  }

  @Override
  @NonNull
  public CompletableFuture<Integer> update(@NonNull QueueModel queue) {
    Objects.requireNonNull(queue);

    final BiFunction<QueueModel, QueueModel, CompletableFuture<Void>> updateProductOrders =
        (oldQueue, updatedQueue) -> {
          final ArrayList<ProductOrderModel> ordersToUpsert = new ArrayList<>();
          final ArrayList<ProductOrderModel> ordersToDelete =
              new ArrayList<>(oldQueue.productOrders());

          for (ProductOrderModel productOrder : updatedQueue.productOrders()) {
            final ProductOrderModel orderToUpsert =
                ProductOrderModel.toBuilder(productOrder)
                    .setQueueId(updatedQueue.id()) // In case they're newly created.
                    .build();

            ordersToUpsert.add(orderToUpsert);
            // Remove product order with equal ID if they're exists inside `ordersToDelete`,
            // so that they will get an upsert, while leaving product orders to delete.
            ordersToDelete.stream()
                .filter(order -> order.id() != null && order.id().equals(orderToUpsert.id()))
                .findFirst()
                .ifPresent(ordersToDelete::remove);
          }

          return CompletableFuture.allOf(
              this._productOrderRepository.upsert(ordersToUpsert),
              this._productOrderRepository.delete(ordersToDelete));
        };

    final BiFunction<QueueModel, QueueModel, CompletableFuture<Void>> updateCustomer =
        (oldQueue, updatedQueue) -> {
          final CompletableFuture<CustomerModel> selectUpdatedCustomer =
              this._customerRepository.selectById(updatedQueue.customerId());
          final CompletableFuture<CustomerModel> selectOldCustomer =
              this._customerRepository.selectById(oldQueue.customerId());

          final BiFunction<CustomerModel, CustomerModel, CompletableFuture<Integer>>
              updateOldCustomer =
                  (oldCustomer, updatedCustomer) ->
                      oldCustomer != null
                              && oldCustomer.id() != null
                              && (updatedCustomer == null
                                  || !oldCustomer.id().equals(updatedCustomer.id()))
                          // Revert back old customer balance when different customer selected,
                          // even when the new one is null.
                          ? this._customerRepository.update(
                              CustomerModel.toBuilder(oldCustomer)
                                  .setBalance(oldCustomer.balanceOnRevertedPayment(oldQueue))
                                  .build())
                          : CompletableFuture.completedFuture(0);
          final BiFunction<CustomerModel, CustomerModel, CompletableFuture<Integer>>
              updateNewCustomer =
                  (oldCustomer, updatedCustomer) ->
                      updatedCustomer != null
                          // Update customer balance for newly selected customer.
                          ? this._customerRepository.update(
                              CustomerModel.toBuilder(updatedCustomer)
                                  .setBalance(
                                      updatedCustomer.balanceOnUpdatedPayment(
                                          oldQueue, updatedQueue))
                                  .build())
                          : CompletableFuture.completedFuture(0);

          return selectUpdatedCustomer.thenAcceptAsync(
              updatedCustomer ->
                  selectOldCustomer.thenAcceptAsync(
                      oldCustomer ->
                          CompletableFuture.allOf(
                              updateOldCustomer.apply(oldCustomer, updatedCustomer),
                              updateNewCustomer.apply(oldCustomer, updatedCustomer))));
        };

    return this.selectById(queue.id())
        .thenComposeAsync(
            oldQueue -> {
              if (oldQueue == null) return CompletableFuture.completedFuture(0);

              final CompletableFuture<Void> updateForeignFuture =
                  updateProductOrders
                      .apply(oldQueue, queue)
                      .thenRunAsync(() -> updateCustomer.apply(oldQueue, queue));
              final CompletableFuture<Integer> update =
                  updateForeignFuture.thenApplyAsync(ignore -> this._localDao.update(queue));

              // Update foreign column firstly, so that queue
              // already provided with an updated value when returned.
              update.thenAcceptAsync(
                  effected -> {
                    if (effected == 0) return;

                    this.selectById(queue.id())
                        .thenAcceptAsync(
                            updatedQueue -> this.notifyModelUpdated(List.of(updatedQueue)));
                  });
              return update;
            });
  }

  @Override
  @NonNull
  public CompletableFuture<Integer> delete(@NonNull QueueModel queue) {
    Objects.requireNonNull(queue);

    // Note: Associated rows on product order table will automatically deleted upon queue deletion.

    final Function<QueueModel, CompletableFuture<Integer>> updateCustomer =
        (oldQueue) ->
            this._customerRepository
                .selectById(oldQueue.customerId())
                .thenComposeAsync(
                    customer -> {
                      // Revert back customer balance.
                      final CustomerModel updatedCustomer =
                          CustomerModel.toBuilder(customer)
                              .setBalance(customer.balanceOnRevertedPayment(oldQueue))
                              .build();
                      return this._customerRepository.update(updatedCustomer);
                    });

    return this.selectById(queue.id())
        .thenComposeAsync(
            oldQueue -> {
              final CompletableFuture<Integer> delete =
                  CompletableFuture.supplyAsync(() -> this._localDao.delete(queue));

              delete.thenAcceptAsync(
                  effected -> {
                    if (effected == 0) return;

                    updateCustomer.apply(oldQueue);
                    this.notifyModelDeleted(List.of(oldQueue));
                  });
              return delete;
            });
  }

  @NonNull
  public CompletableFuture<List<QueueModel>> selectAllInRange(
      @NonNull ZonedDateTime startDate, @NonNull ZonedDateTime endDate) {
    Objects.requireNonNull(startDate);
    Objects.requireNonNull(endDate);

    return CompletableFuture.supplyAsync(
            () -> this._localDao.selectAllInRange(startDate.toInstant(), endDate.toInstant()))
        .thenComposeAsync(this::_mapFields);
  }

  /**
   * Specifically used when query returning object model, mostly select query. Like when {@link
   * QueueModel} consisting {@link QueueModel#customer()}, which can only be obtained from database.
   * We have to make sure those field mapped into it.
   */
  @NonNull
  private CompletableFuture<QueueModel> _mapFields(@Nullable QueueModel queue) {
    if (queue == null) return CompletableFuture.completedFuture(null);

    final CompletableFuture<CustomerModel> selectCustomer =
        this._customerRepository.selectById(queue.customerId());
    final CompletableFuture<List<ProductOrderModel>> selectProductOrders =
        this._productOrderRepository.selectAllByQueueId(queue.id());

    return CompletableFuture.allOf(selectCustomer, selectProductOrders)
        .thenApplyAsync(
            ignore ->
                QueueModel.toBuilder(queue)
                    .setCustomer(selectCustomer.join())
                    .setProductOrders(selectProductOrders.join())
                    .build());
  }

  /**
   * @see QueueRepository#_mapFields(QueueModel)
   */
  @NonNull
  private CompletableFuture<List<QueueModel>> _mapFields(@Nullable List<QueueModel> queues) {
    if (queues == null) return CompletableFuture.completedFuture(null);

    final List<CompletableFuture<QueueModel>> mapFields =
        queues.stream().map(this::_mapFields).collect(Collectors.toList());

    return CompletableFuture.allOf(mapFields.stream().toArray(CompletableFuture[]::new))
        .thenApplyAsync(
            ignore -> mapFields.stream().map(CompletableFuture::join).collect(Collectors.toList()));
  }
}
