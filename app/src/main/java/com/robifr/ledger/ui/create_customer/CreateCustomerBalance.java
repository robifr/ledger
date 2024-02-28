/**
 * Copyright (c) 2022-present Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package com.robifr.ledger.ui.create_customer;

import android.content.DialogInterface;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.robifr.ledger.R;
import com.robifr.ledger.databinding.CreateCustomerDialogTransactionBinding;
import com.robifr.ledger.ui.CurrencyTextWatcher;
import com.robifr.ledger.ui.create_customer.view_model.CustomerBalanceViewModel;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.util.Objects;

public class CreateCustomerBalance
    implements View.OnClickListener,
        DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener {
  @NonNull private final CreateCustomerFragment _fragment;

  @NonNull private final CreateCustomerDialogTransactionBinding _withdrawDialogBinding;
  @NonNull private final AlertDialog _withdrawDialog;
  @NonNull private final BalanceTextWatcher _withdrawTextWatcher;

  @NonNull private final CreateCustomerDialogTransactionBinding _depositDialogBinding;
  @NonNull private final AlertDialog _depositDialog;
  @NonNull private final BalanceTextWatcher _depositTextWatcher;

  public CreateCustomerBalance(@NonNull CreateCustomerFragment fragment) {
    this._fragment = Objects.requireNonNull(fragment);

    this._withdrawDialogBinding =
        CreateCustomerDialogTransactionBinding.inflate(this._fragment.getLayoutInflater());
    this._withdrawDialog =
        new MaterialAlertDialogBuilder(this._fragment.requireContext(), R.style.MaterialAlertDialog)
            .setView(this._withdrawDialogBinding.getRoot())
            .setNegativeButton(this._fragment.getString(R.string.text_cancel), this)
            .setPositiveButton(this._fragment.getString(R.string.text_withdraw), this)
            .create();
    this._withdrawTextWatcher =
        new BalanceTextWatcher(this._withdrawDialogBinding.amount, "id", "ID");

    this._depositDialogBinding =
        CreateCustomerDialogTransactionBinding.inflate(this._fragment.getLayoutInflater());
    this._depositDialog =
        new MaterialAlertDialogBuilder(this._fragment.requireContext(), R.style.MaterialAlertDialog)
            .setView(this._depositDialogBinding.getRoot())
            .setNegativeButton(this._fragment.getString(R.string.text_cancel), this)
            .setPositiveButton(this._fragment.getString(R.string.text_deposit), this)
            .create();
    this._depositTextWatcher =
        new BalanceTextWatcher(this._depositDialogBinding.amount, "id", "ID");

    this._fragment.fragmentBinding().withdrawButton.setOnClickListener(this);
    this._withdrawDialog.setOnDismissListener(this);
    this._withdrawDialogBinding.title.setText(
        this._fragment.getString(R.string.text_withdraw_balance));
    this._withdrawDialogBinding.amount.addTextChangedListener(this._withdrawTextWatcher);

    this._fragment.fragmentBinding().depositButton.setOnClickListener(this);
    this._depositDialog.setOnDismissListener(this);
    this._depositDialogBinding.title.setText(
        this._fragment.getString(R.string.text_deposit_balance));
    this._depositDialogBinding.amount.addTextChangedListener(this._depositTextWatcher);
  }

  @Override
  public void onClick(@NonNull View view) {
    Objects.requireNonNull(view);

    switch (view.getId()) {
      case R.id.withdrawButton -> {
        this._fragment
            .createCustomerViewModel()
            .balanceView()
            .setAvailableBalanceToWithdraw(
                this._fragment.createCustomerViewModel().inputtedCustomer().balance());
        this._withdrawDialog.show();
      }

      case R.id.depositButton -> this._depositDialog.show();
    }
  }

  @Override
  public void onClick(@NonNull DialogInterface dialog, int buttonType) {
    Objects.requireNonNull(dialog);

    switch (buttonType) {
      case DialogInterface.BUTTON_POSITIVE -> {
        if (dialog.equals(this._withdrawDialog)) {
          this._fragment.createCustomerViewModel().balanceView().onWithdrawSubmitted();

        } else if (dialog.equals(this._depositDialog)) {
          this._fragment.createCustomerViewModel().balanceView().onDepositSubmitted();
        }

        dialog.dismiss();
      }

      case DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss();
    }
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    Objects.requireNonNull(dialog);

    this._fragment.createCustomerViewModel().balanceView().onReset();

    if (this._withdrawDialog.getCurrentFocus() != null) {
      this._withdrawDialog.getCurrentFocus().clearFocus();
    }
    if (this._depositDialog.getCurrentFocus() != null) {
      this._depositDialog.getCurrentFocus().clearFocus();
    }
  }

  public void setInputtedBalance(long balance) {
    final String formattedBalance = CurrencyFormat.format(BigDecimal.valueOf(balance), "id", "ID");
    this._fragment.fragmentBinding().balance.setText(formattedBalance);

    // Disable withdraw button when the balance is zero.
    final boolean isBalanceAboveZero = BigDecimal.valueOf(balance).compareTo(BigDecimal.ZERO) > 0;
    this._fragment.fragmentBinding().withdrawButton.setEnabled(isBalanceAboveZero);

    // Disable deposit button when above or equals maximum limit.
    final boolean isBalanceBelowLimit =
        BigDecimal.valueOf(balance).compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) < 0;
    this._fragment.fragmentBinding().depositButton.setEnabled(isBalanceBelowLimit);
  }

  /**
   * @param amount Formatted text of deposit amount.
   */
  public void setInputtedDepositAmountText(@Nullable String amount) {
    final String currentText = this._depositDialogBinding.amount.getText().toString();
    if (currentText.equals(amount)) return;

    final int cursorPosition = amount != null ? amount.length() : 0;

    // Remove listener to prevent any sort of formatting.
    this._depositDialogBinding.amount.removeTextChangedListener(this._depositTextWatcher);
    this._depositDialogBinding.amount.setText(amount);
    this._depositDialogBinding.amount.setSelection(cursorPosition);
    this._depositDialogBinding.amount.addTextChangedListener(this._depositTextWatcher);
  }

  /**
   * @param amount Formatted text of withdraw amount.
   */
  public void setInputtedWithdrawAmountText(@Nullable String amount) {
    final String currentText = this._withdrawDialogBinding.amount.getText().toString();
    if (currentText.equals(amount)) return;

    final int cursorPosition = amount != null ? amount.length() : 0;

    // Remove listener to prevent any sort of formatting.
    this._withdrawDialogBinding.amount.removeTextChangedListener(this._withdrawTextWatcher);
    this._withdrawDialogBinding.amount.setText(amount);
    this._withdrawDialogBinding.amount.setSelection(cursorPosition);
    this._withdrawDialogBinding.amount.addTextChangedListener(this._withdrawTextWatcher);
  }

  public void setAvailableAmountToWithdraw(long amount) {
    this._withdrawDialogBinding.amountLayout.setHelperText(
        this._fragment.getString(
            R.string.createcustomerdialog_balanceavailable_helper,
            CurrencyFormat.format(BigDecimal.valueOf(amount), "id", "ID")));
  }

  private class BalanceTextWatcher extends CurrencyTextWatcher {
    public BalanceTextWatcher(
        @NonNull EditText editText, @NonNull String language, @NonNull String country) {
      super(editText, language, country);
    }

    @Override
    public void afterTextChanged(@NonNull Editable editable) {
      super.afterTextChanged(editable);

      final CustomerBalanceViewModel balanceViewModel =
          CreateCustomerBalance.this._fragment.createCustomerViewModel().balanceView();

      if (this._view == CreateCustomerBalance.this._depositDialogBinding.amount) {
        balanceViewModel.onDepositAmountTextChanged(this.newText());

      } else if (this._view == CreateCustomerBalance.this._withdrawDialogBinding.amount) {
        balanceViewModel.onWithdrawAmountTextChanged(this.newText());
      }
    }
  }
}
