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

package io.github.robifr.ledger.ui.queue.viewmodel

import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.QueuePaginatedInfo
import io.github.robifr.ledger.repository.ModelChangedListener

class CustomerSyncListener(
    val currentQueues: () -> List<QueuePaginatedInfo>,
    val onSyncQueues: (List<QueuePaginatedInfo>) -> Unit
) : ModelChangedListener<CustomerModel> {
  override fun onModelAdded(customers: List<CustomerModel>) {
    // Current queues doesn't contains a newly added customer.
  }

  override fun onModelUpdated(customers: List<CustomerModel>) {
    onSyncQueues(
        currentQueues().toMutableList().apply {
          for (customer in customers) {
            for ((i, queue) in withIndex()) {
              // When customer updated, apply the changes into the queue model.
              if (queue.customerId != null &&
                  customer.id != null &&
                  queue.customerId == customer.id) {
                set(
                    i,
                    queue.copy(
                        customerId = customer.id,
                        customerName = customer.name,
                        fullModel = queue.fullModel?.copy(customer = customer)))
              }
            }
          }
        })
  }

  override fun onModelDeleted(customers: List<CustomerModel>) {
    onSyncQueues(
        currentQueues().toMutableList().apply {
          for (customer in customers) {
            for ((i, queue) in withIndex()) {
              // When customer deleted, remove them from the queue model.
              if (queue.customerId != null &&
                  customer.id != null &&
                  queue.customerId == customer.id) {
                set(
                    i,
                    queue.copy(
                        customerId = null,
                        customerName = null,
                        fullModel = queue.fullModel?.copy(customer = null)))
              }
            }
          }
        })
  }

  override fun onModelUpserted(customers: List<CustomerModel>) {
    // Only when customer updated, apply the changes into the queue model and ignore for
    // any inserted customer. Because current queues doesn't contains a newly added customer.
    onModelUpdated(customers)
  }
}
