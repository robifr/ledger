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

/**
 * @param filteredCustomerIds Filter queue if [customer ID][QueueModel.customerId] is included
 *   inside the list.
 * @param isNullCustomerShown Whether queue with no [customer ID][QueueModel.customerId] should be
 *   shown or not.
 * @param filteredStatus Filter queue if [status][QueueModel.status] is included.
 * @param filteredTotalPrice Filter queue if [grand total price][QueueModel.grandTotalPrice] is
 *   in-between min (first) and max (second). Set the pair value as null to represent unbounded
 *   number.
 * @param filteredDate Filter queues if [date][QueueModel.date] is still considered within specified
 *   range of start and end date.
 */
data class QueueFilters(
    val filteredCustomerIds: List<Long>,
    val isNullCustomerShown: Boolean,
    val filteredStatus: Set<QueueModel.Status>,
    val filteredTotalPrice: Pair<BigDecimal?, BigDecimal?>,
    val filteredDate: QueueDate
)
