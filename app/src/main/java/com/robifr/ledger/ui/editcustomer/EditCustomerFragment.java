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

package com.robifr.ledger.ui.editcustomer;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.Navigation;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.ui.FragmentResultKey;
import com.robifr.ledger.ui.createcustomer.CreateCustomerFragment;
import com.robifr.ledger.ui.editcustomer.viewmodel.EditCustomerViewModel;
import java.util.Objects;

public class EditCustomerFragment extends CreateCustomerFragment {
  public enum Arguments implements FragmentResultKey {
    INITIAL_CUSTOMER_ID_TO_EDIT;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  public enum Request implements FragmentResultKey {
    EDIT_CUSTOMER;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  public enum Result implements FragmentResultKey {
    EDITED_CUSTOMER_ID;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {
    super.onViewCreated(view, savedInstance);
    Objects.requireNonNull(this._fragmentBinding);

    this._createCustomerViewModel =
        new ViewModelProvider(this, new EditCustomerViewModel.Factory(this.requireContext()))
            .get(EditCustomerViewModel.class);
    this._viewModelHandler =
        new EditCustomerViewModelHandler(
            this, (EditCustomerViewModel) this._createCustomerViewModel);

    this._fragmentBinding.toolbar.setTitle(this.getString(R.string.text_edit_customer));

    final NavBackStackEntry backStackEntry =
        Navigation.findNavController(this._fragmentBinding.getRoot()).getCurrentBackStackEntry();

    if (this._createCustomerViewModel instanceof EditCustomerViewModel editCustomerViewModel
        && backStackEntry != null
        && backStackEntry.getArguments() != null) {
      final CustomerModel initialCustomer =
          editCustomerViewModel.selectCustomerById(
              backStackEntry
                  .getArguments()
                  .getLong(Arguments.INITIAL_CUSTOMER_ID_TO_EDIT.key(), 0L));
      Objects.requireNonNull(initialCustomer); // Logically shouldn't be null when editing data.

      editCustomerViewModel.setInitialCustomerToEdit(initialCustomer);
      editCustomerViewModel.onNameTextChanged(initialCustomer.name());
      editCustomerViewModel.onBalanceChanged(initialCustomer.balance());
      editCustomerViewModel.onDebtChanged(initialCustomer.debt());
    }
  }
}