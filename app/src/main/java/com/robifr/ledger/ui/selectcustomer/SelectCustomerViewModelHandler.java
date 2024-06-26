/**
 * Copyright (c) 2024 Robi
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

package com.robifr.ledger.ui.selectcustomer;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.ui.LiveDataEvent.Observer;
import com.robifr.ledger.ui.StringResources;
import com.robifr.ledger.ui.selectcustomer.viewmodel.SelectCustomerViewModel;
import java.util.List;
import java.util.Objects;

public class SelectCustomerViewModelHandler {
  @NonNull private final SelectCustomerFragment _fragment;
  @NonNull private final SelectCustomerViewModel _viewModel;

  public SelectCustomerViewModelHandler(
      @NonNull SelectCustomerFragment fragment, @NonNull SelectCustomerViewModel viewModel) {
    this._fragment = Objects.requireNonNull(fragment);
    this._viewModel = Objects.requireNonNull(viewModel);

    this._viewModel
        .resultSelectedCustomerId()
        .observe(
            this._fragment.getViewLifecycleOwner(),
            new Observer<>(this::_onResultSelectedCustomerId));
    this._viewModel
        .snackbarMessage()
        .observe(this._fragment.getViewLifecycleOwner(), new Observer<>(this::_onSnackbarMessage));
    this._viewModel.customers().observe(this._fragment.getViewLifecycleOwner(), this::_onCustomers);
  }

  private void _onResultSelectedCustomerId(@Nullable Long customerId) {
    final Bundle bundle = new Bundle();

    if (customerId != null) {
      bundle.putLong(SelectCustomerFragment.Result.SELECTED_CUSTOMER_ID.key(), customerId);
    }

    this._fragment
        .getParentFragmentManager()
        .setFragmentResult(SelectCustomerFragment.Request.SELECT_CUSTOMER.key(), bundle);
    this._fragment.finish();
  }

  private void _onSnackbarMessage(@Nullable StringResources stringRes) {
    if (stringRes == null) return;

    Snackbar.make(
            (View) this._fragment.fragmentBinding().getRoot().getParent(),
            StringResources.stringOf(this._fragment.requireContext(), stringRes),
            Snackbar.LENGTH_LONG)
        .show();
  }

  private void _onCustomers(@Nullable List<CustomerModel> customers) {
    this._fragment.adapter().notifyDataSetChanged();
  }
}
