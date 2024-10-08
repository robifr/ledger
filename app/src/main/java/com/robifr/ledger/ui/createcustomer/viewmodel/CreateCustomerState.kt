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

package com.robifr.ledger.ui.createcustomer.viewmodel

import androidx.annotation.ColorRes
import com.robifr.ledger.R
import com.robifr.ledger.ui.StringResourceType
import java.math.BigDecimal

data class CreateCustomerState(
    val name: String,
    val nameErrorMessageRes: StringResourceType?,
    val balance: Long,
    val debt: BigDecimal
) {
  val isAddBalanceButtonEnabled: Boolean
    get() = balance.toBigDecimal().compareTo(Long.MAX_VALUE.toBigDecimal()) < 0

  val isWithdrawBalanceButtonEnabled: Boolean
    get() = balance.toBigDecimal().compareTo(0.toBigDecimal()) > 0

  @get:ColorRes
  val debtColorRes: Int
    get() = if (debt.compareTo(0.toBigDecimal()) < 0) R.color.red else R.color.text_disabled
}
