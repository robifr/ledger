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

package io.github.robifr.ledger.ui.createcustomer

import android.content.DialogInterface
import android.text.Editable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import io.github.robifr.ledger.R
import io.github.robifr.ledger.databinding.CreateCustomerDialogTransactionBinding
import io.github.robifr.ledger.ui.common.CurrencyTextWatcher
import io.github.robifr.ledger.util.CurrencyFormat

class CreateCustomerBalance(private val _fragment: CreateCustomerFragment) {
  private val _withdrawBalanceDialogBinding: CreateCustomerDialogTransactionBinding =
      CreateCustomerDialogTransactionBinding.inflate(_fragment.layoutInflater).apply {
        title.setText(R.string.createCustomer_balance_withdraw)
      }
  private val _withdrawBalanceDialog: AlertDialog =
      MaterialAlertDialogBuilder(_fragment.requireContext())
          .setView(_withdrawBalanceDialogBinding.root)
          .setNegativeButton(R.string.action_cancel) { _, _ -> }
          .setPositiveButton(R.string.action_withdraw) { _, _ ->
            _fragment.createCustomerViewModel.balanceView.onWithdrawBalanceSubmitted()
          }
          .create()
          .apply {
            setOnDismissListener {
              _fragment.createCustomerViewModel.balanceView.onWithdrawBalanceDialogClosed()
            }
          }
  private val _withdrawTextWatcher: WithdrawBalanceTextWatcher =
      WithdrawBalanceTextWatcher(_fragment, _withdrawBalanceDialogBinding.amount)

  private val _addBalanceDialogBinding: CreateCustomerDialogTransactionBinding =
      CreateCustomerDialogTransactionBinding.inflate(_fragment.layoutInflater).apply {
        title.setText(R.string.createCustomer_balance_add)
      }
  private val _addBalanceDialog: AlertDialog =
      MaterialAlertDialogBuilder(_fragment.requireContext())
          .setView(_addBalanceDialogBinding.root)
          .setNegativeButton(R.string.action_cancel) { _, _ -> }
          .setPositiveButton(R.string.action_add) { _, _ ->
            _fragment.createCustomerViewModel.balanceView.onAddBalanceSubmitted()
          }
          .create()
          .apply {
            setOnDismissListener {
              _fragment.createCustomerViewModel.balanceView.onAddBalanceDialogClosed()
            }
          }
  private val _addBalanceTextWatcher: AddBalanceTextWatcher =
      AddBalanceTextWatcher(_fragment, _addBalanceDialogBinding.amount)

  init {
    _fragment.fragmentBinding.withdrawButton.setOnClickListener {
      _fragment.createCustomerViewModel.balanceView.onWithdrawBalanceDialogShown()
    }
    _fragment.fragmentBinding.addBalanceButton.setOnClickListener {
      _fragment.createCustomerViewModel.balanceView.onAddBalanceDialogShown()
    }
    _withdrawBalanceDialogBinding.amount.addTextChangedListener(_withdrawTextWatcher)
    _addBalanceDialogBinding.amount.addTextChangedListener(_addBalanceTextWatcher)
  }

  fun setInputtedBalance(balance: Long) {
    _fragment.fragmentBinding.balance.setText(
        CurrencyFormat.formatCents(
            balance.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags()))
  }

  fun showAddBalanceDialog() {
    _addBalanceDialog.show()
  }

  fun dismissAddBalanceDialog() {
    _addBalanceDialog.dismiss()
    _addBalanceDialog.currentFocus?.clearFocus()
  }

  fun setAddBalanceButtonEnabled(isEnabled: Boolean) {
    _fragment.fragmentBinding.addBalanceButton.isEnabled = isEnabled
  }

  fun showWithdrawBalanceDialog() {
    _withdrawBalanceDialog.show()
  }

  fun dismissWithdrawBalanceDialog() {
    _withdrawBalanceDialog.dismiss()
    _withdrawBalanceDialog.currentFocus?.clearFocus()
  }

  fun setWithdrawBalanceButtonEnabled(isEnabled: Boolean) {
    _fragment.fragmentBinding.withdrawButton.isEnabled = isEnabled
  }

  fun setInputtedBalanceAmountText(formattedAmount: String, isAddButtonEnabled: Boolean) {
    _addBalanceDialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = isAddButtonEnabled
    if (_addBalanceDialogBinding.amount.text.toString() == formattedAmount) return
    // Remove listener to prevent any sort of formatting.
    _addBalanceDialogBinding.amount.removeTextChangedListener(_addBalanceTextWatcher)
    _addBalanceDialogBinding.amount.setText(formattedAmount)
    _addBalanceDialogBinding.amount.addTextChangedListener(_addBalanceTextWatcher)
  }

  fun setInputtedWithdrawAmountText(formattedAmount: String, isWithdrawButtonEnabled: Boolean) {
    _withdrawBalanceDialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled =
        isWithdrawButtonEnabled
    if (_withdrawBalanceDialogBinding.amount.text.toString() == formattedAmount) return
    // Remove listener to prevent any sort of formatting.
    _withdrawBalanceDialogBinding.amount.removeTextChangedListener(_withdrawTextWatcher)
    _withdrawBalanceDialogBinding.amount.setText(formattedAmount)
    _withdrawBalanceDialogBinding.amount.addTextChangedListener(_withdrawTextWatcher)
  }

  fun setAvailableAmountToWithdraw(amount: Long) {
    _withdrawBalanceDialogBinding.amountLayout.helperText =
        _fragment.getString(
            R.string.createCustomer_balance_withdraw_n_available,
            CurrencyFormat.formatCents(
                amount.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags()))
  }
}

private class AddBalanceTextWatcher(
    private val _fragment: CreateCustomerFragment,
    editText: TextInputEditText
) : CurrencyTextWatcher(editText) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.createCustomerViewModel.balanceView.onAddBalanceAmountTextChanged(newText())
  }
}

private class WithdrawBalanceTextWatcher(
    private val _fragment: CreateCustomerFragment,
    editText: TextInputEditText
) : CurrencyTextWatcher(editText) {
  override fun afterTextChanged(editable: Editable) {
    super.afterTextChanged(editable)
    _fragment.createCustomerViewModel.balanceView.onWithdrawAmountTextChanged(newText())
  }
}
