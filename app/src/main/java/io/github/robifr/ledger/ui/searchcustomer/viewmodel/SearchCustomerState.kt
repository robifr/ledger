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

package io.github.robifr.ledger.ui.searchcustomer.viewmodel

import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.ui.searchcustomer.SearchCustomerFragment

/**
 * @property isSelectionEnabled Whether the fragment should return
 *   [SearchCustomerFragment.Request.SELECT_CUSTOMER] on back navigation.
 * @property expandedCustomerIndex Currently expanded customer index from [customers]. -1 to
 *   represent none being expanded.
 */
data class SearchCustomerState(
    val isSelectionEnabled: Boolean,
    val isToolbarVisible: Boolean,
    val initialQuery: String,
    val query: String,
    val initialSelectedCustomerIds: List<Long>,
    val customers: List<CustomerModel>,
    val expandedCustomerIndex: Int,
    val isCustomerMenuDialogShown: Boolean,
    val selectedCustomerMenu: CustomerModel?
) {
  val expandedCustomer: CustomerModel?
    get() = if (expandedCustomerIndex != -1) customers[expandedCustomerIndex] else null

  val isNoResultFoundIllustrationVisible: Boolean
    get() = query.isNotEmpty() && customers.isEmpty()

  val isRecyclerViewVisible: Boolean
    get() = query.isNotEmpty() && customers.isNotEmpty()
}
