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

package com.robifr.ledger.data.model

import java.math.BigDecimal

@JvmRecord
data class CustomerDebtInfo(val id: Long?, val debt: BigDecimal) : Info {
  override fun modelId(): Long? = id

  companion object {
    @JvmStatic
    fun withModel(customer: CustomerModel): CustomerDebtInfo =
        CustomerDebtInfo(customer.id, customer.debt)
  }
}
