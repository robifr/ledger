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

package com.robifr.ledger.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.text.HtmlCompat;
import com.robifr.ledger.R;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.util.Objects;

public class DashboardBalance {
  @NonNull private final DashboardFragment _fragment;

  public DashboardBalance(@NonNull DashboardFragment fragment) {
    this._fragment = Objects.requireNonNull(fragment);
  }

  public void setTotalBalance(@NonNull BigDecimal amount) {
    Objects.requireNonNull(amount);

    this._fragment
        .fragmentBinding()
        .balance
        .totalBalance
        .setText(
            CurrencyFormat.format(
                amount, AppCompatDelegate.getApplicationLocales().toLanguageTags()));
  }

  public void setTotalCustomersWithBalance(int amount) {
    final String totalText =
        this._fragment
            .getResources()
            .getQuantityString(R.plurals.dashboard_balance_from_n_customer, amount, amount);

    this._fragment
        .fragmentBinding()
        .balance
        .totalCustomersWithBalanceTitle
        .setText(HtmlCompat.fromHtml(totalText, HtmlCompat.FROM_HTML_MODE_LEGACY));
  }

  public void setTotalDebt(@NonNull BigDecimal amount) {
    Objects.requireNonNull(amount);

    final int amountTextColor =
        amount.compareTo(BigDecimal.ZERO) < 0
            // Negative debt will be shown red.
            ? this._fragment.requireContext().getColor(R.color.red)
            : this._fragment.requireContext().getColor(R.color.text_enabled);

    this._fragment
        .fragmentBinding()
        .balance
        .totalDebt
        .setText(
            CurrencyFormat.format(
                amount, AppCompatDelegate.getApplicationLocales().toLanguageTags()));
    this._fragment.fragmentBinding().balance.totalDebt.setTextColor(amountTextColor);
  }

  public void setTotalCustomersWithDebt(int amount) {
    final String totalText =
        this._fragment
            .getResources()
            .getQuantityString(R.plurals.dashboard_balance_from_n_customer, amount, amount);

    this._fragment
        .fragmentBinding()
        .balance
        .totalCustomersWithDebtTitle
        .setText(HtmlCompat.fromHtml(totalText, HtmlCompat.FROM_HTML_MODE_LEGACY));
  }
}
