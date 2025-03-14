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

package io.github.robifr.ledger.data.display

import io.github.robifr.ledger.data.model.CustomerModel
import java.text.Collator
import java.util.Locale

class FakeCustomerSorter(
    var sortMethod: CustomerSortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, true)
) {
  fun sort(customers: List<CustomerModel>): List<CustomerModel> =
      when (sortMethod.sortBy) {
        CustomerSortMethod.SortBy.NAME -> _sortByName(customers)
        CustomerSortMethod.SortBy.BALANCE -> _sortByBalance(customers)
      }

  private fun _sortByName(customers: List<CustomerModel>): List<CustomerModel> {
    val collator: Collator = Collator.getInstance(Locale.US).apply { strength = Collator.SECONDARY }
    val comparator: Comparator<CustomerModel> = Comparator.comparing(CustomerModel::name, collator)
    return customers.sortedWith(if (sortMethod.isAscending) comparator else comparator.reversed())
  }

  private fun _sortByBalance(customers: List<CustomerModel>): List<CustomerModel> {
    val comparator: Comparator<CustomerModel> = Comparator.comparing(CustomerModel::balance)
    return customers.sortedWith(if (sortMethod.isAscending) comparator else comparator.reversed())
  }
}
