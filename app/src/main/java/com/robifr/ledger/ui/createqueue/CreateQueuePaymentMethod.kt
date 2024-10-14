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

package com.robifr.ledger.ui.createqueue

import android.graphics.drawable.Drawable
import android.view.View
import com.google.android.material.button.MaterialButton
import com.robifr.ledger.R
import com.robifr.ledger.data.model.QueueModel

class CreateQueuePaymentMethod(private val _fragment: CreateQueueFragment) : View.OnClickListener {
  init {
    _fragment.fragmentBinding.paymentMethodCashButton.setOnClickListener(this)
    _fragment.fragmentBinding.paymentMethodAccountBalanceButton.setOnClickListener(this)
  }

  override fun onClick(view: View?) {
    when (view?.id) {
      R.id.paymentMethodCashButton,
      R.id.paymentMethodAccountBalanceButton -> {
        _fragment.createQueueViewModel.onPaymentMethodChanged(
            QueueModel.PaymentMethod.valueOf(view.tag.toString()))
      }
    }
  }

  fun setInputtedPaymentMethod(paymentMethod: QueueModel.PaymentMethod) {
    // Unselect all buttons other than given payment method.
    for (payment in QueueModel.PaymentMethod.entries) {
      val button: MaterialButton =
          _fragment.fragmentBinding.root.findViewWithTag(payment.toString())
      if (payment == paymentMethod) _selectButton(button) else _unselectButton(button)
    }
  }

  fun setEnabledButtons(paymentMethods: Set<QueueModel.PaymentMethod>) {
    for (paymentMethod in QueueModel.PaymentMethod.entries) {
      val button: MaterialButton =
          _fragment.fragmentBinding.root.findViewWithTag(paymentMethod.toString())
      val buttonIcons: Array<Drawable> = button.compoundDrawables
      buttonIcons[0].setTintList(_fragment.requireContext().getColorStateList(R.color.text))
      button.setEnabled(paymentMethods.contains(paymentMethod))
      // Index 0 is payment method icon, index 2 is checkmark icon.
      button.setCompoundDrawables(buttonIcons[0], null, buttonIcons[2], null)
    }
  }

  fun setVisible(isVisible: Boolean) {
    val visibility = if (isVisible) View.VISIBLE else View.GONE
    _fragment.fragmentBinding.paymentMethodTitle.setVisibility(visibility)
    _fragment.fragmentBinding.paymentMethodCashButton.setVisibility(visibility)
    _fragment.fragmentBinding.paymentMethodAccountBalanceButton.setVisibility(visibility)
  }

  private fun _selectButton(button: MaterialButton) {
    button.setCompoundDrawablesWithIntrinsicBounds(
        button.compoundDrawables[0], // Payment method icon.
        null,
        _fragment.requireContext().getDrawable(R.drawable.icon_check),
        null)
    button.setChecked(true)
  }

  private fun _unselectButton(button: MaterialButton) {
    button.setCompoundDrawablesWithIntrinsicBounds(
        button.compoundDrawables[0], // Payment method icon.
        null,
        null,
        null)
    button.setChecked(false)
  }
}
