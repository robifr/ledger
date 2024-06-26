/**
 * Copyright (c) 2024 Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package com.robifr.ledger.data.model

@JvmRecord
data class CustomerBalanceInfo(val id: Long?, val balance: Long) : Info {
  companion object {
    @JvmStatic
    fun withModel(customer: CustomerModel): CustomerBalanceInfo =
        CustomerBalanceInfo(customer.id, customer.balance)
  }

  override fun modelId(): Long? = this.id
}
