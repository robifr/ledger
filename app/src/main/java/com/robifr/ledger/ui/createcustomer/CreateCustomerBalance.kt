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

import android.content.DialogInterface
import android.text.Editable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.robifr.ledger.R
import com.robifr.ledger.databinding.CreateCustomerDialogTransactionBinding
import com.robifr.ledger.ui.CurrencyTextWatcher
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerViewModel
import com.robifr.ledger.util.CurrencyFormat

class CreateCustomerBalance(private val _fragment: CreateCustomerFragment) {
  private val _withdrawBalanceDialogBinding: CreateCustomerDialogTransactionBinding =
      CreateCustomerDialogTransactionBinding.inflate(_fragment.layoutInflater)
  private val _withdrawBalanceDialog: AlertDialog =
      MaterialAlertDialogBuilder(_fragment.requireContext())
          .setView(_withdrawBalanceDialogBinding.root)
          .setNegativeButton(R.string.action_cancel) { dialog: DialogInterface?, _ ->
            dialog?.dismiss()
          }
          .setPositiveButton(R.string.action_withdraw) { dialog: DialogInterface?, _ ->
            _fragment.createCustomerViewModel.balanceView.onWithdrawBalanceSubmitted()
            dialog?.dismiss()
          }
          .create()
  private val _withdrawTextWatcher: WithdrawBalanceTextWatcher =
      WithdrawBalanceTextWatcher(
          _fragment.createCustomerViewModel, _withdrawBalanceDialogBinding.amount)

  private val _addBalanceDialogBinding: CreateCustomerDialogTransactionBinding =
      CreateCustomerDialogTransactionBinding.inflate(_fragment.layoutInflater)
  private val _addBalanceDialog: AlertDialog =
      MaterialAlertDialogBuilder(_fragment.requireContext())
          .setView(_addBalanceDialogBinding.root)
          .setNegativeButton(R.string.action_cancel) { dialog: DialogInterface?, _ ->
            dialog?.dismiss()
          }
          .setPositiveButton(R.string.action_add) { dialog: DialogInterface?, _ ->
            _fragment.createCustomerViewModel.balanceView.onAddBalanceSubmitted()
            dialog?.dismiss()
          }
          .create()
  private val _addBalanceTextWatcher: AddBalanceTextWatcher =
      AddBalanceTextWatcher(_fragment.createCustomerViewModel, _addBalanceDialogBinding.amount)

  init {
    _fragment.fragmentBinding.withdrawButton.setOnClickListener {
      _fragment.createCustomerViewModel.balanceView.onShowWithdrawBalanceDialog()
    }
    _withdrawBalanceDialog.setOnDismissListener {
      _fragment.createCustomerViewModel.balanceView.onCloseWithdrawBalanceDialog()
    }
    _withdrawBalanceDialogBinding.title.setText(R.string.createCustomer_balance_withdraw)
    _withdrawBalanceDialogBinding.amount.addTextChangedListener(_withdrawTextWatcher)

    _fragment.fragmentBinding.addBalanceButton.setOnClickListener {
      _fragment.createCustomerViewModel.balanceView.onShowAddBalanceDialog()
    }
    _addBalanceDialog.setOnDismissListener {
      _fragment.createCustomerViewModel.balanceView.onCloseAddBalanceDialog()
    }
    _addBalanceDialogBinding.title.setText(R.string.createCustomer_balance_add)
    _addBalanceDialogBinding.amount.addTextChangedListener(_addBalanceTextWatcher)
  }

  fun setInputtedBalance(balance: Long) {
    _fragment.fragmentBinding.balance.setText(
        CurrencyFormat.format(
            balance.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags()))
  }

  fun setAddBalanceButtonEnabled(isEnabled: Boolean) {
    _fragment.fragmentBinding.addBalanceButton.setEnabled(isEnabled)
  }

  fun setWithdrawBalanceButtonEnabled(isEnabled: Boolean) {
    _fragment.fragmentBinding.withdrawButton.setEnabled(isEnabled)
  }

  fun setAddBalanceDialogShown(isShown: Boolean) {
    if (isShown) {
      _addBalanceDialog.show()
    } else {
      _addBalanceDialog.dismiss()
      _addBalanceDialog.currentFocus?.clearFocus()
    }
  }

  fun setInputtedBalanceAmountText(formattedAmount: String) {
    if (_addBalanceDialogBinding.amount.text.toString() == formattedAmount) return
    // Remove listener to prevent any sort of formatting.
    _addBalanceDialogBinding.amount.removeTextChangedListener(_addBalanceTextWatcher)
    _addBalanceDialogBinding.amount.setText(formattedAmount)
    _addBalanceDialogBinding.amount.setSelection(formattedAmount.length)
    _addBalanceDialogBinding.amount.addTextChangedListener(_addBalanceTextWatcher)
  }

  fun setWithdrawBalanceDialogShown(isShown: Boolean) {
    if (isShown) {
      _withdrawBalanceDialog.show()
    } else {
      _withdrawBalanceDialog.dismiss()
      _withdrawBalanceDialog.currentFocus?.clearFocus()
    }
  }

  fun setInputtedWithdrawAmountText(formattedAmount: String) {
    if (_withdrawBalanceDialogBinding.amount.text.toString() == formattedAmount) return
    // Remove listener to prevent any sort of formatting.
    _withdrawBalanceDialogBinding.amount.removeTextChangedListener(_withdrawTextWatcher)
    _withdrawBalanceDialogBinding.amount.setText(formattedAmount)
    _withdrawBalanceDialogBinding.amount.setSelection(formattedAmount.length)
    _withdrawBalanceDialogBinding.amount.addTextChangedListener(_withdrawTextWatcher)
  }

  fun setAvailableAmountToWithdraw(amount: Long) {
    _withdrawBalanceDialogBinding.amountLayout.setHelperText(
        _fragment.getString(
            R.string.createCustomer_balance_withdraw_n_available,
            CurrencyFormat.format(
                amount.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())))
  }
}

private class AddBalanceTextWatcher(
    private val _createCustomerViewModel: CreateCustomerViewModel,
    editText: TextInputEditText,
) : CurrencyTextWatcher(editText) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _createCustomerViewModel.balanceView.onBalanceAmountTextChanged(newText())
  }
}

private class WithdrawBalanceTextWatcher(
    private val _createCustomerViewModel: CreateCustomerViewModel,
    editText: TextInputEditText,
) : CurrencyTextWatcher(editText) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _createCustomerViewModel.balanceView.onWithdrawAmountTextChanged(newText())
  }
}
