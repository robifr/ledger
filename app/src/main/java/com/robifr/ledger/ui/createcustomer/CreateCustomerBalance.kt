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
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.robifr.ledger.R
import com.robifr.ledger.databinding.CreateCustomerDialogTransactionBinding
import com.robifr.ledger.ui.CurrencyTextWatcher
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerViewModel
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal

class CreateCustomerBalance(private val _fragment: CreateCustomerFragment) :
    View.OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
  private val _withdrawBalanceDialogBinding: CreateCustomerDialogTransactionBinding =
      CreateCustomerDialogTransactionBinding.inflate(_fragment.layoutInflater)
  private val _withdrawBalanceDialog: AlertDialog =
      MaterialAlertDialogBuilder(_fragment.requireContext())
          .setView(_withdrawBalanceDialogBinding.root)
          .setNegativeButton(R.string.action_cancel, this)
          .setPositiveButton(R.string.action_withdraw, this)
          .create()
  private val _withdrawTextWatcher: WithdrawBalanceTextWatcher =
      WithdrawBalanceTextWatcher(
          _fragment.createCustomerViewModel, _withdrawBalanceDialogBinding.amount)

  private val _addBalanceDialogBinding: CreateCustomerDialogTransactionBinding =
      CreateCustomerDialogTransactionBinding.inflate(_fragment.layoutInflater)
  private val _addBalanceDialog: AlertDialog =
      MaterialAlertDialogBuilder(_fragment.requireContext())
          .setView(_addBalanceDialogBinding.root)
          .setNegativeButton(R.string.action_cancel, this)
          .setPositiveButton(R.string.action_add, this)
          .create()
  private val _addBalanceTextWatcher: AddBalanceTextWatcher =
      AddBalanceTextWatcher(_fragment.createCustomerViewModel, _addBalanceDialogBinding.amount)

  init {
    _fragment.fragmentBinding.withdrawButton.setOnClickListener(this)
    _withdrawBalanceDialog.setOnDismissListener(this)
    _withdrawBalanceDialogBinding.title.setText(R.string.createCustomer_balance_withdraw)
    _withdrawBalanceDialogBinding.amount.addTextChangedListener(_withdrawTextWatcher)

    _fragment.fragmentBinding.addBalanceButton.setOnClickListener(this)
    _addBalanceDialog.setOnDismissListener(this)
    _addBalanceDialogBinding.title.setText(R.string.createCustomer_balance_add)
    _addBalanceDialogBinding.amount.addTextChangedListener(_addBalanceTextWatcher)
  }

  override fun onClick(view: View?) {
    when (view?.id) {
      R.id.withdrawButton ->
          _fragment.createCustomerViewModel.balanceView.onShowWithdrawBalanceDialog()
      R.id.addBalanceButton ->
          _fragment.createCustomerViewModel.balanceView.onShowAddBalanceDialog()
    }
  }

  override fun onClick(dialog: DialogInterface?, buttonType: Int) {
    when (buttonType) {
      DialogInterface.BUTTON_POSITIVE -> {
        when (dialog) {
          _withdrawBalanceDialog ->
              _fragment.createCustomerViewModel.balanceView.onWithdrawBalanceSubmitted()
          _addBalanceDialog -> _fragment.createCustomerViewModel.balanceView.onAddBalanceSubmitted()
        }
        dialog?.dismiss()
      }
      DialogInterface.BUTTON_NEGATIVE -> dialog?.dismiss()
    }
  }

  override fun onDismiss(dialog: DialogInterface?) {
    when (dialog) {
      _withdrawBalanceDialog ->
          _fragment.createCustomerViewModel.balanceView.onCloseWithdrawBalanceDialog()
      _addBalanceDialog -> _fragment.createCustomerViewModel.balanceView.onCloseAddBalanceDialog()
    }
  }

  fun setInputtedBalance(balance: Long) {
    _fragment.fragmentBinding.balance.setText(
        CurrencyFormat.format(
            BigDecimal.valueOf(balance),
            AppCompatDelegate.getApplicationLocales().toLanguageTags()))
    // Disable withdraw button when the balance is zero.
    _fragment.fragmentBinding.withdrawButton.setEnabled(
        BigDecimal.valueOf(balance).compareTo(BigDecimal.ZERO) > 0)
    // Disable button to add balance when the balance is above or equals maximum limit.
    _fragment.fragmentBinding.addBalanceButton.setEnabled(
        BigDecimal.valueOf(balance).compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) < 0)
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
    val currentText: String = _addBalanceDialogBinding.amount.text.toString()
    if (currentText == formattedAmount) return
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
    val currentText: String = _withdrawBalanceDialogBinding.amount.text.toString()
    if (currentText == formattedAmount) return
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
                BigDecimal.valueOf(amount),
                AppCompatDelegate.getApplicationLocales().toLanguageTags())))
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
