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

package com.robifr.ledger.ui.createcustomer

import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatDelegate
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal

class CreateCustomerDebt(private val _fragment: CreateCustomerFragment) {
  fun setInputtedDebt(debt: BigDecimal, @ColorRes textColor: Int) {
    _fragment.fragmentBinding.debt.setText(
        CurrencyFormat.format(debt, AppCompatDelegate.getApplicationLocales().toLanguageTags()))
    _fragment.fragmentBinding.debt.setTextColor(_fragment.requireContext().getColor(textColor))
  }
}
