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

package com.robifr.ledger.ui.createcustomer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.robifr.ledger.R;
import com.robifr.ledger.databinding.CreateCustomerFragmentBinding;
import com.robifr.ledger.ui.BackStack;
import com.robifr.ledger.ui.FragmentResultKey;
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerViewModel;
import com.robifr.ledger.util.Compats;
import java.math.BigDecimal;
import java.util.Objects;

public class CreateCustomerFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
  public enum Request implements FragmentResultKey {
    CREATE_CUSTOMER;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  public enum Result implements FragmentResultKey {
    CREATED_CUSTOMER_ID;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  @NonNull protected final OnBackPressedHandler _onBackPressed = new OnBackPressedHandler();
  @Nullable protected CreateCustomerFragmentBinding _fragmentBinding;
  @Nullable protected CreateCustomerName _inputName;
  @Nullable protected CreateCustomerBalance _inputBalance;
  @Nullable protected CreateCustomerDebt _inputDebt;

  @Nullable protected CreateCustomerViewModel _createCustomerViewModel;
  @Nullable protected CreateCustomerViewModelHandler _viewModelHandler;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstance) {
    Objects.requireNonNull(inflater);

    this._fragmentBinding = CreateCustomerFragmentBinding.inflate(inflater, container, false);
    return this._fragmentBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {
    Objects.requireNonNull(view);
    Objects.requireNonNull(this._fragmentBinding);

    this._inputName = new CreateCustomerName(this);
    this._inputBalance = new CreateCustomerBalance(this);
    this._inputDebt = new CreateCustomerDebt(this);
    this._createCustomerViewModel =
        new ViewModelProvider(this, new CreateCustomerViewModel.Factory(this.requireContext()))
            .get(CreateCustomerViewModel.class);
    this._viewModelHandler =
        new CreateCustomerViewModelHandler(this, this._createCustomerViewModel);

    this.requireActivity()
        .getOnBackPressedDispatcher()
        .addCallback(this.getViewLifecycleOwner(), this._onBackPressed);
    this._fragmentBinding.toolbar.getMenu().clear();
    this._fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_edit);
    this._fragmentBinding.toolbar.setOnMenuItemClickListener(this);
    this._fragmentBinding.toolbar.setNavigationOnClickListener(
        v -> this._onBackPressed.handleOnBackPressed());

    this._createCustomerViewModel.onBalanceChanged(0L);
    this._createCustomerViewModel.onDebtChanged(BigDecimal.ZERO);
  }

  @Override
  public boolean onMenuItemClick(@NonNull MenuItem item) {
    Objects.requireNonNull(item);
    Objects.requireNonNull(this._createCustomerViewModel);

    return switch (item.getItemId()) {
      case R.id.save -> {
        this._createCustomerViewModel.onSave();
        yield true;
      }

      default -> false;
    };
  }

  @NonNull
  public CreateCustomerFragmentBinding fragmentBinding() {
    return Objects.requireNonNull(this._fragmentBinding);
  }

  @NonNull
  public CreateCustomerName inputName() {
    return Objects.requireNonNull(this._inputName);
  }

  @NonNull
  public CreateCustomerBalance inputBalance() {
    return Objects.requireNonNull(this._inputBalance);
  }

  @NonNull
  public CreateCustomerDebt inputDebt() {
    return Objects.requireNonNull(this._inputDebt);
  }

  @NonNull
  public CreateCustomerViewModel createCustomerViewModel() {
    return Objects.requireNonNull(this._createCustomerViewModel);
  }

  public void finish() {
    if (this.requireActivity() instanceof BackStack navigation
        && navigation.currentTabStackTag() != null) {
      Compats.hideKeyboard(this.requireContext(), this.requireView().findFocus());
      navigation.popFragmentStack(navigation.currentTabStackTag());
    }
  }

  public static class Factory extends FragmentFactory {
    @Override
    @NonNull
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
      Objects.requireNonNull(classLoader);
      Objects.requireNonNull(className);

      return (className.equals(CreateCustomerFragment.class.getName()))
          ? new CreateCustomerFragment()
          : super.instantiate(classLoader, className);
    }
  }

  private class OnBackPressedHandler extends OnBackPressedCallback {
    public OnBackPressedHandler() {
      super(true);
    }

    @Override
    public void handleOnBackPressed() {
      new MaterialAlertDialogBuilder(CreateCustomerFragment.this.requireContext())
          .setTitle(CreateCustomerFragment.this.getString(R.string.text_discard_this_unsaved_task))
          .setNegativeButton(
              CreateCustomerFragment.this.getString(R.string.text_discard),
              (dialog, type) -> CreateCustomerFragment.this.finish())
          .setPositiveButton(
              CreateCustomerFragment.this.getString(R.string.text_cancel),
              (dialog, type) -> dialog.dismiss())
          .show();
    }
  }
}
