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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerFiltererTest {
  private val _customer: CustomerModel = CustomerModel(id = null, name = "")

  private fun `_filter by balance cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(listOf(0L, 100L, 200L), null, null, listOf(0L, 100L, 200L)),
          arrayOf(listOf(0L, 100L, 200L), null, 100L, listOf(0L, 100L)),
          arrayOf(listOf(0L, 100L, 200L), 100L, null, listOf(100L, 200L)),
          arrayOf(listOf(0L, 100L, 200L), 100L, 100L, listOf(100L)))

  @ParameterizedTest
  @MethodSource("_filter by balance cases")
  fun `filter by balance`(
      balances: List<Long>,
      filteredMinBalance: Long?,
      filteredMaxBalance: Long?,
      filteredBalances: List<Long>
  ) {
    val customers: List<CustomerModel> =
        balances.mapIndexed { i, balance -> _customer.copy(id = (i + 1) * 111L, balance = balance) }
    val filterer: FakeCustomerFilterer =
        FakeCustomerFilterer().apply {
          filters = filters.copy(filteredBalance = filteredMinBalance to filteredMaxBalance)
        }
    assertEquals(
        filteredBalances,
        filterer.filter(customers).map { it.balance },
        "Filter customers based on their balance")
  }

  private fun `_filter by debt cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(listOf(0, -100, -200), null, null, listOf(0, -100, -200)),
          arrayOf(listOf(0, -100, -200), null, 100, listOf(0, -100)),
          arrayOf(listOf(0, -100, -200), null, -100, listOf(0, -100)),
          arrayOf(listOf(0, -100, -200), 100, null, listOf(-100, -200)),
          arrayOf(listOf(0, -100, -200), -100, null, listOf(-100, -200)),
          arrayOf(listOf(0, -100, -200), 100, 100, listOf(-100)),
          arrayOf(listOf(0, -100, -200), -100, -100, listOf(-100)))

  @ParameterizedTest
  @MethodSource("_filter by debt cases")
  fun `filter by debt`(
      debts: List<Int>,
      filteredMinDebt: Int?,
      filteredMaxDebt: Int?,
      filteredDebts: List<Int>
  ) {
    val customers: List<CustomerModel> =
        debts.mapIndexed { i, debt ->
          _customer.copy(id = (i + 1) * 111L, debt = debt.toBigDecimal())
        }
    val filterer: FakeCustomerFilterer =
        FakeCustomerFilterer().apply {
          filters =
              filters.copy(
                  filteredDebt = filteredMinDebt?.toBigDecimal() to filteredMaxDebt?.toBigDecimal())
        }
    assertEquals(
        filteredDebts,
        filterer.filter(customers).map { it.debt.toInt() },
        "Filter customers based on their debt")
  }
}
