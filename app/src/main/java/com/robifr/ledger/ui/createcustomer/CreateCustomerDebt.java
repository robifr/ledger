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

package com.robifr.ledger.ui.createcustomer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import com.robifr.ledger.R;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.util.Objects;

public class CreateCustomerDebt {
  @NonNull private final CreateCustomerFragment _fragment;

  public CreateCustomerDebt(@NonNull CreateCustomerFragment fragment) {
    this._fragment = Objects.requireNonNull(fragment);
  }

  public void setInputtedDebt(@NonNull BigDecimal debt) {
    Objects.requireNonNull(debt);

    final int textColor =
        debt.compareTo(BigDecimal.ZERO) < 0
            // Red for negative debt.
            ? this._fragment.requireContext().getColor(R.color.red)
            : this._fragment.requireContext().getColor(R.color.text_disabled);

    this._fragment
        .fragmentBinding()
        .debt
        .setText(
            CurrencyFormat.format(
                debt, AppCompatDelegate.getApplicationLocales().toLanguageTags()));
    this._fragment.fragmentBinding().debt.setTextColor(textColor);
  }
}
