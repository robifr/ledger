/**
 * Copyright 2025 Robi
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

package io.github.robifr.ledger.data.model

import androidx.room.ColumnInfo
import androidx.room.Ignore
import java.math.BigDecimal

data class CustomerPaginatedInfo(
    @ColumnInfo(name = "id") override val id: Long?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "balance") val balance: Long,
    @ColumnInfo(name = "debt") val debt: BigDecimal,
    @Ignore val fullModel: CustomerModel? = null
) : Info {
  /** Reserved constructor to be used by Room upon querying. */
  constructor(
      id: Long?,
      name: String,
      balance: Long,
      debt: BigDecimal
  ) : this(
      id,
      name,
      balance,
      debt,
      // Normally, `fullModel` should only be instantiated when full data is required, for example,
      // when a card expands. However, since the data for both the normal and expanded cards is
      // identical, it's acceptable to instantiate it here.
      CustomerModel(id, name, balance, debt))

  constructor(
      customer: CustomerModel
  ) : this(customer.id, customer.name, customer.balance, customer.debt, customer)
}
