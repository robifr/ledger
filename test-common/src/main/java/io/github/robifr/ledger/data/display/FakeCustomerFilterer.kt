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
import java.math.BigDecimal

class FakeCustomerFilterer(
    var filters: CustomerFilters = CustomerFilters(null to null, null to null)
) {
  fun filter(customers: List<CustomerModel>): List<CustomerModel> =
      customers.filter { !_shouldFilteredOutByBalance(it) && !_shouldFilteredOutByDebt(it) }

  private fun _shouldFilteredOutByBalance(customer: CustomerModel): Boolean {
    val (first: Long?, second: Long?) = filters.filteredBalance
    return (first != null && customer.balance < first) ||
        (second != null && customer.balance > second)
  }

  private fun _shouldFilteredOutByDebt(customer: CustomerModel): Boolean {
    val (first: BigDecimal?, second: BigDecimal?) = filters.filteredDebt
    return (first != null && customer.debt.abs().compareTo(first.abs()) < 0) ||
        (second != null && customer.debt.abs().compareTo(second.abs()) > 0)
  }
}
