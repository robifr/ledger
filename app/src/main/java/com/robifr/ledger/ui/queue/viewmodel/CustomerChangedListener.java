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

package com.robifr.ledger.ui.queue.viewmodel;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.data.model.QueueModel;
import com.robifr.ledger.repository.ModelChangedListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class CustomerChangedListener implements ModelChangedListener<CustomerModel> {
  @NonNull private final QueueViewModel _viewModel;

  public CustomerChangedListener(@NonNull QueueViewModel viewModel) {
    this._viewModel = Objects.requireNonNull(viewModel);
  }

  @Override
  @WorkerThread
  public void onModelAdded(@NonNull List<CustomerModel> customers) {}

  @Override
  @WorkerThread
  public void onModelUpdated(@NonNull List<CustomerModel> customers) {
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              final ArrayList<QueueModel> queues =
                  new ArrayList<>(this._viewModel.queues().getValue());

              for (CustomerModel customer : customers) {
                for (int i = 0; i < queues.size(); i++) {
                  final QueueModel queue = queues.get(i);

                  // When customer updated, apply those changes into the queue model.
                  if (queue.customerId() != null && queue.customerId().equals(customer.id())) {
                    final QueueModel updatedQueue =
                        queue.withCustomerId(customer.id()).withCustomer(customer);
                    queues.set(i, updatedQueue);
                  }
                }
              }

              this._viewModel
                  .filterView()
                  .onFiltersChanged(this._viewModel.filterView().inputtedFilters(), queues);
            });
  }

  @Override
  @WorkerThread
  public void onModelDeleted(@NonNull List<CustomerModel> customers) {
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              final ArrayList<QueueModel> queues =
                  new ArrayList<>(this._viewModel.queues().getValue());

              for (CustomerModel customer : customers) {
                for (int i = 0; i < queues.size(); i++) {
                  final QueueModel queue = queues.get(i);

                  // When customer deleted, remove them from the queue model.
                  if (queue.customerId() != null && queue.customerId().equals(customer.id())) {
                    final QueueModel updatedQueue = queue.withCustomerId(null).withCustomer(null);
                    queues.set(i, updatedQueue);
                  }
                }
              }

              this._viewModel
                  .filterView()
                  .onFiltersChanged(this._viewModel.filterView().inputtedFilters(), queues);
            });
  }

  @Override
  @WorkerThread
  public void onModelUpserted(@NonNull List<CustomerModel> customers) {
    // Only when customer updated, apply those changes into the queue model
    // and ignore for any inserted customer.
    this.onModelUpdated(customers);
  }
}
