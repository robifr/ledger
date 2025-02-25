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

import com.robifr.ledger.data.model.CustomerModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CustomerSorterTest {
  private val _customer: CustomerModel = CustomerModel(id = null, name = "")

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `sort by name with primary collator strength`(isAscending: Boolean) {
    val customers: List<CustomerModel> =
        listOf("Amy", "Ben", "Cal").mapIndexed { i, name ->
          _customer.copy(id = (i + 1) * 111L, name = name)
        }
    val sorter: FakeCustomerSorter =
        FakeCustomerSorter().apply {
          sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.NAME, isAscending)
        }
    assertEquals(
        if (isAscending) listOf("Amy", "Ben", "Cal") else listOf("Cal", "Ben", "Amy"),
        sorter.sort(customers).map { it.name },
        "Sort customers based on their name")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `sort by balance`(isAscending: Boolean) {
    val customers: List<CustomerModel> =
        listOf(0L, 100L, 200L).mapIndexed { i, balance ->
          _customer.copy(id = (i + 1) * 111L, balance = balance)
        }
    val sorter: FakeCustomerSorter =
        FakeCustomerSorter().apply {
          sortMethod = CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, isAscending)
        }
    assertEquals(
        if (isAscending) listOf(0L, 100L, 200L) else listOf(200L, 100L, 0L),
        sorter.sort(customers).map { it.balance },
        "Sort customers based on their balance")
  }
}
