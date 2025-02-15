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

package com.robifr.ledger.ui.filtercustomer.viewmodel

import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.CustomerPaginatedInfo
import com.robifr.ledger.ui.common.pagination.PaginationState

/**
 * @property expandedCustomerIndex Currently expanded customer index from
 *   [PaginationState.paginatedItems]. -1 to represent none being expanded.
 */
data class FilterCustomerState(
    val pagination: PaginationState<CustomerPaginatedInfo>,
    val expandedCustomerIndex: Int,
    val filteredCustomers: List<CustomerPaginatedInfo>
) {
  val expandedCustomer: CustomerModel?
    get() =
        // The full model is always loaded by default.
        if (expandedCustomerIndex != -1) pagination.paginatedItems[expandedCustomerIndex].fullModel
        else null
}
