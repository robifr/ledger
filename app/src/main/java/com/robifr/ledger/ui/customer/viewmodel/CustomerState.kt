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

package com.robifr.ledger.ui.customer.viewmodel

import com.robifr.ledger.data.display.CustomerSortMethod
import com.robifr.ledger.data.model.CustomerModel

/**
 * @property expandedCustomerIndex Currently expanded customer index from [customers]. -1 to
 *   represent none being expanded.
 */
data class CustomerState(
    val customers: List<CustomerModel>,
    val expandedCustomerIndex: Int,
    val isCustomerMenuDialogShown: Boolean,
    val selectedCustomerMenu: CustomerModel?,
    val isNoCustomersAddedIllustrationVisible: Boolean,
    val sortMethod: CustomerSortMethod,
    val isSortMethodDialogShown: Boolean
)
