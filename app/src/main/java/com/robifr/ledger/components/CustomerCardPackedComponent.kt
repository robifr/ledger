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

package com.robifr.ledger.components

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.R as MaterialR
import com.google.android.material.shape.ShapeAppearanceModel
import com.robifr.ledger.R
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.databinding.CustomerCardPackedBinding
import com.robifr.ledger.util.CurrencyFormat

class CustomerCardPackedComponent(
    private val _context: Context,
    private val _binding: CustomerCardPackedBinding
) {
  init {
    _binding.image.shapeableImage.shapeAppearanceModel =
        ShapeAppearanceModel.builder(
                _context,
                MaterialR.style.Widget_MaterialComponents_ShapeableImageView,
                R.style.Shape_Round)
            .build()
  }

  fun setCustomer(customer: CustomerModel) {
    _setName(customer.name)
    _setBalance(customer.balance)
  }

  private fun _setName(name: String) {
    _binding.name.text = name
    _binding.image.text.text = name.take(1)
  }

  private fun _setBalance(balance: Long) {
    _binding.balance.text =
        CurrencyFormat.format(
            balance.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
  }
}
