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

package com.robifr.ledger.data.display

import com.robifr.ledger.data.model.QueueModel
import java.math.BigDecimal
import java.time.LocalDate

class QueueFilterer(
    var filters: QueueFilters =
        QueueFilters(
            filteredCustomerIds = listOf(),
            isNullCustomerShown = true,
            filteredStatus = QueueModel.Status.entries.toSet(),
            filteredTotalPrice = null to null,
            filteredDate = QueueDateKt(QueueDateKt.Range.ALL_TIME))
) {
  fun filter(queues: List<QueueModel>): List<QueueModel> =
      queues.filter {
        !_shouldFilteredOutByCustomerId(it) &&
            !_shouldFilteredOutByStatus(it) &&
            !_shouldFilteredOutByDate(it) &&
            !_shouldFilteredOutByTotalPrice(it)
      }

  private fun _shouldFilteredOutByCustomerId(queue: QueueModel): Boolean {
    val isCustomerNotInFilterGroup: Boolean =
        queue.customerId != null &&
            // Show all customers when the list empty.
            filters.filteredCustomerIds.isNotEmpty() &&
            filters.filteredCustomerIds.none { it == queue.customerId }
    return (queue.customerId == null && !filters.isNullCustomerShown) || isCustomerNotInFilterGroup
  }

  private fun _shouldFilteredOutByDate(queue: QueueModel): Boolean {
    val date: LocalDate = queue.date.atZone(filters.filteredDate.dateStart.zone).toLocalDate()
    return date.isBefore(filters.filteredDate.dateStart.toLocalDate()) ||
        date.isAfter(filters.filteredDate.dateEnd.toLocalDate())
  }

  private fun _shouldFilteredOutByStatus(queue: QueueModel): Boolean =
      !filters.filteredStatus.contains(queue.status)

  private fun _shouldFilteredOutByTotalPrice(queue: QueueModel): Boolean {
    val (first: BigDecimal?, second: BigDecimal?) = filters.filteredTotalPrice
    return ((first != null && queue.grandTotalPrice().compareTo(first) < 0) ||
        (second != null && queue.grandTotalPrice().compareTo(second) > 0))
  }
}
