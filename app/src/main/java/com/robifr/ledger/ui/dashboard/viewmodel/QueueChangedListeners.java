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

package com.robifr.ledger.ui.dashboard.viewmodel;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import com.robifr.ledger.data.InfoUpdater;
import com.robifr.ledger.data.ModelUpdater;import com.robifr.ledger.data.display.QueueDate;
import com.robifr.ledger.data.model.QueueModel;
import com.robifr.ledger.data.model.QueueWithProductOrdersInfo;
import com.robifr.ledger.repository.ModelChangedListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;import java.util.function.BiFunction;

class QueueChangedListeners implements ModelChangedListener<QueueModel> {
  @NonNull private final DashboardViewModel _viewModel;

  public QueueChangedListeners(@NonNull DashboardViewModel viewModel) {
    this._viewModel = Objects.requireNonNull(viewModel);
  }

  @Override
  @WorkerThread
  public void onModelAdded(@NonNull List<QueueModel> queues) {
    Objects.requireNonNull(queues);

    new Handler(Looper.getMainLooper())
        .post(() -> {
          this._updateQueues(queues, ModelUpdater::addModel);
          this._updateQueueInfo(queues, InfoUpdater::addInfo);
        });
  }

  @Override
  @WorkerThread
  public void onModelUpdated(@NonNull List<QueueModel> queues) {
    Objects.requireNonNull(queues);

    new Handler(Looper.getMainLooper())
        .post(() -> {
          this._updateQueues(queues, ModelUpdater::updateModel);
          this._updateQueueInfo(queues, InfoUpdater::updateInfo);
        });
  }

  @Override
  @WorkerThread
  public void onModelDeleted(@NonNull List<QueueModel> queues) {
    Objects.requireNonNull(queues);

    new Handler(Looper.getMainLooper())
        .post(() -> {
          this._updateQueues(queues, ModelUpdater::deleteModel);
          this._updateQueueInfo(queues, InfoUpdater::deleteInfo);
        });
  }

  @Override
  @WorkerThread
  public void onModelUpserted(@NonNull List<QueueModel> queues) {}

  private void _updateQueues(
      @NonNull List<QueueModel> queues,
      @NonNull BiFunction<List<QueueModel>, List<QueueModel>, List<QueueModel>> updater) {
    Objects.requireNonNull(queues);
    Objects.requireNonNull(updater);

    final QueueDate date =
        Objects.requireNonNullElse(
            this._viewModel.date().getValue(), QueueDate.withRange(QueueDate.Range.ALL_TIME));
    final ArrayList<QueueModel> currentQueues =
        this._viewModel.queues().getValue() != null
            ? new ArrayList<>(this._viewModel.queues().getValue())
            : new ArrayList<>();
    final List<QueueModel> filteredQueues =
        updater.apply(queues, currentQueues);

    filteredQueues.removeIf(
        info ->
            info.date().isBefore(date.dateStart().toInstant())
                || info.date().isAfter(date.dateEnd().toInstant()));

    this._viewModel.onQueuesChanged(filteredQueues);
  }

  private void _updateQueueInfo(
      @NonNull List<QueueModel> queues,
      @NonNull InfoUpdaterFunction<QueueModel, QueueWithProductOrdersInfo> updater) {
    Objects.requireNonNull(queues);
    Objects.requireNonNull(updater);

    final QueueDate date =
        Objects.requireNonNullElse(
            this._viewModel.date().getValue(), QueueDate.withRange(QueueDate.Range.ALL_TIME));
    final ArrayList<QueueWithProductOrdersInfo> currentQueueInfo =
        this._viewModel.queuesWithProductOrders().getValue() != null
            ? new ArrayList<>(this._viewModel.queuesWithProductOrders().getValue())
            : new ArrayList<>();
    final List<QueueWithProductOrdersInfo> queueInfo =
        updater.apply(queues, currentQueueInfo, QueueWithProductOrdersInfo::withModel);

    queueInfo.removeIf(
        info ->
            info.date().isBefore(date.dateStart().toInstant())
                || info.date().isAfter(date.dateEnd().toInstant()));
    this._viewModel.onQueuesWithProductOrdersChanged(queueInfo);
  }
}
