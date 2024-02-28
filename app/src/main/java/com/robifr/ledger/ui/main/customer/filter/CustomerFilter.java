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

package com.robifr.ledger.ui.main.customer.filter;

import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.robifr.ledger.R;
import com.robifr.ledger.databinding.CustomerDialogFilterBinding;
import com.robifr.ledger.ui.main.customer.CustomerFragment;
import java.util.Objects;

public class CustomerFilter implements DialogInterface.OnDismissListener {
  @NonNull private final CustomerFragment _fragment;
  @NonNull private final CustomerDialogFilterBinding _dialogBinding;
  @NonNull private final BottomSheetDialog _dialog;
  @NonNull private final CustomerFilterBalance _filterBalance;
  @NonNull private final CustomerFilterDebt _filterDebt;

  public CustomerFilter(@NonNull CustomerFragment fragment) {
    this._fragment = Objects.requireNonNull(fragment);
    this._dialogBinding = CustomerDialogFilterBinding.inflate(this._fragment.getLayoutInflater());

    this._dialog =
        new BottomSheetDialog(this._fragment.requireContext(), R.style.BottomSheetDialog);
    this._dialog.setContentView(this._dialogBinding.getRoot());

    this._filterBalance =
        new CustomerFilterBalance(this._fragment, this._dialogBinding, this._dialog);
    this._filterDebt = new CustomerFilterDebt(this._fragment, this._dialogBinding, this._dialog);

    this._dialog.setOnDismissListener(this);
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    Objects.requireNonNull(dialog);

    this._fragment
        .customerViewModel()
        .onCustomersChanged(this._fragment.customerViewModel().fetchAllCustomers());
    this._fragment
        .customerViewModel()
        .filterView()
        .onFiltersChanged(this._fragment.customerViewModel().filterView().inputtedFilters());

    if (this._dialog.getCurrentFocus() != null) this._dialog.getCurrentFocus().clearFocus();
  }

  @NonNull
  public CustomerFilterBalance filterBalance() {
    return this._filterBalance;
  }

  @NonNull
  public CustomerFilterDebt filterDebt() {
    return this._filterDebt;
  }

  public void openDialog() {
    // Allow bottom sheet to go fully expanded.
    final View bottomSheet =
        Objects.requireNonNull(
            this._dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet));
    bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
    bottomSheet.setLayoutParams(bottomSheet.getLayoutParams());

    this._dialog.getBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
    this._dialog.show();
  }
}
