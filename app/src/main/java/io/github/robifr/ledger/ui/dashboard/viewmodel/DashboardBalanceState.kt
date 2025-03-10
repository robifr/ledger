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

package io.github.robifr.ledger.ui.dashboard.viewmodel

import androidx.annotation.ColorRes
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.model.CustomerBalanceInfo
import io.github.robifr.ledger.data.model.CustomerDebtInfo
import java.math.BigDecimal

data class DashboardBalanceState(
    val customersWithBalance: List<CustomerBalanceInfo>,
    val customersWithDebt: List<CustomerDebtInfo>
) {
  fun totalBalance(): BigDecimal = customersWithBalance.sumOf { it.balance.toBigDecimal() }

  fun totalDebt(): BigDecimal = customersWithDebt.sumOf { it.debt }

  @ColorRes
  fun totalDebtColorRes(): Int =
      if (totalDebt().compareTo(0.toBigDecimal()) < 0) R.color.red else R.color.text_enabled
}
