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

package com.robifr.ledger.ui.selectcustomer.viewmodel

import androidx.annotation.StringRes
import com.robifr.ledger.R
import com.robifr.ledger.data.model.CustomerModel

data class SelectCustomerState(
    val initialSelectedCustomer: CustomerModel?,
    val selectedCustomerOnDatabase: CustomerModel?,
    val customers: List<CustomerModel>,
    /** Currently expanded customer index from [customers]. -1 to represent none being expanded. */
    val expandedCustomerIndex: Int,
    val isSelectedCustomerPreviewExpanded: Boolean
) {
  @get:StringRes
  val selectedItemDescriptionStringRes: Int?
    // Actually, it should always be set to an empty text or not visible at all, because
    // selecting a customer can only occur during queue creation or editing. Unlike
    // `ProductModel`, the referenced customer in the `QueueModel` is never stored, only its ID.
    // This means the customer will always be up to date. However, it's nice to keep this unused
    // feature.
    get() =
        // The original customer in the database was deleted.
        if (initialSelectedCustomer != null &&
            selectedCustomerOnDatabase == null &&
            // Don't show text when the product isn't set yet,
            // preventing initial text from flashing.
            customers.isNotEmpty()) {
          R.string.selectCustomer_originalCustomerDeleted
          // The original customer in the database was edited.
        } else if (initialSelectedCustomer != null &&
            initialSelectedCustomer != selectedCustomerOnDatabase &&
            // Don't show text when the product isn't set yet,
            // preventing initial text from flashing.
            customers.isNotEmpty()) {
          R.string.selectCustomer_originalCustomerChanged
          // It’s the same unchanged customer.
        } else {
          null
        }
}
