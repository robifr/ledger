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

package com.robifr.ledger.ui.dashboard

import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import com.robifr.ledger.R
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal

class DashboardBalance(private val _fragment: DashboardFragment) {
  fun setTotalBalance(amount: BigDecimal, totalCustomers: Int) {
    _fragment.fragmentBinding.balance.totalBalance.text =
        CurrencyFormat.formatCents(
            amount, AppCompatDelegate.getApplicationLocales().toLanguageTags())
    _fragment.fragmentBinding.balance.totalCustomersWithBalanceTitle.text =
        HtmlCompat.fromHtml(
            _fragment.resources.getQuantityString(
                R.plurals.dashboard_balance_from_n_customer, totalCustomers, totalCustomers),
            HtmlCompat.FROM_HTML_MODE_LEGACY)
  }

  fun setTotalDebt(amount: BigDecimal, @ColorRes amountTextColor: Int, totalCustomers: Int) {
    _fragment.fragmentBinding.balance.totalDebt.text =
        CurrencyFormat.formatCents(
            amount, AppCompatDelegate.getApplicationLocales().toLanguageTags())
    _fragment.fragmentBinding.balance.totalDebt.setTextColor(
        _fragment.requireContext().getColor(amountTextColor))
    _fragment.fragmentBinding.balance.totalCustomersWithDebtTitle.text =
        HtmlCompat.fromHtml(
            _fragment.resources.getQuantityString(
                R.plurals.dashboard_balance_from_n_customer, totalCustomers, totalCustomers),
            HtmlCompat.FROM_HTML_MODE_LEGACY)
  }
}
