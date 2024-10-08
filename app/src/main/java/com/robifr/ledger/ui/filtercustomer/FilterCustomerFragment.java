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

package com.robifr.ledger.ui.filtercustomer;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.databinding.ListableFragmentBinding;
import com.robifr.ledger.ui.FragmentResultKey;
import com.robifr.ledger.ui.filtercustomer.recycler.FilterCustomerAdapter;
import com.robifr.ledger.ui.filtercustomer.viewmodel.FilterCustomerViewModel;
import com.robifr.ledger.ui.searchcustomer.SearchCustomerFragment;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Objects;

@AndroidEntryPoint
public class FilterCustomerFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
  public enum Arguments implements FragmentResultKey {
    INITIAL_FILTERED_CUSTOMER_IDS_LONG_ARRAY
  }

  public enum Request implements FragmentResultKey {
    FILTER_CUSTOMER
  }

  public enum Result implements FragmentResultKey {
    FILTERED_CUSTOMER_IDS_LONG_ARRAY
  }

  @NonNull private final OnBackPressedHandler _onBackPressed = new OnBackPressedHandler();
  @Nullable private ListableFragmentBinding _fragmentBinding;
  @Nullable private FilterCustomerAdapter _adapter;
  @Nullable private FilterCustomerResultHandler _resultHandler;

  @Nullable private FilterCustomerViewModel _filterCustomerViewModel;
  @Nullable private FilterCustomerViewModelHandler _viewModelHandler;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstance) {
    Objects.requireNonNull(inflater);

    this._fragmentBinding = ListableFragmentBinding.inflate(inflater, container, false);
    return this._fragmentBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {
    Objects.requireNonNull(view);
    Objects.requireNonNull(this._fragmentBinding);

    this._adapter = new FilterCustomerAdapter(this);
    this._filterCustomerViewModel = new ViewModelProvider(this).get(FilterCustomerViewModel.class);
    this._viewModelHandler =
        new FilterCustomerViewModelHandler(this, this._filterCustomerViewModel);

    this.requireActivity()
        .getOnBackPressedDispatcher()
        .addCallback(this.getViewLifecycleOwner(), this._onBackPressed);
    this._fragmentBinding.toolbar.getMenu().clear();
    this._fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_select_multiple);
    this._fragmentBinding.toolbar.setTitle(R.string.filterCustomer);
    this._fragmentBinding.toolbar.setOnMenuItemClickListener(this);
    this._fragmentBinding.toolbar.setNavigationOnClickListener(
        v -> this._onBackPressed.handleOnBackPressed());
    this._fragmentBinding.horizontalToolbar.setVisibility(View.GONE);
    this._fragmentBinding.recyclerView.setLayoutManager(
        new LinearLayoutManager(this.requireContext()));
    this._fragmentBinding.recyclerView.setAdapter(this._adapter);
    this._fragmentBinding.recyclerView.setItemViewCacheSize(0);
  }

  @Override
  public void onStart() {
    super.onStart();
    // Should be called after `FilterCustomerViewModelHandler` called. `onStart` is perfect place
    // for it. If there's a fragment inherit from this class, which mostly inherit their own
    // view model handler too. Then it's impossible to not call them both inside `onViewCreated`,
    // unless `super` call is omitted entirely.
    this._resultHandler = new FilterCustomerResultHandler(this);
  }

  @Override
  public boolean onMenuItemClick(@NonNull MenuItem item) {
    Objects.requireNonNull(item);
    Objects.requireNonNull(this._fragmentBinding);
    Objects.requireNonNull(this._filterCustomerViewModel);

    return switch (item.getItemId()) {
      case R.id.search -> {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(
            SearchCustomerFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key(), true);
        bundle.putLongArray(
            SearchCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_IDS_LONG_ARRAY.key(),
            this._filterCustomerViewModel.filteredCustomers().getValue().stream()
                .map(CustomerModel::id)
                .mapToLong(Long::longValue)
                .toArray());

        Navigation.findNavController(this._fragmentBinding.getRoot())
            .navigate(R.id.searchCustomerFragment, bundle);
        yield true;
      }

      case R.id.save -> {
        this._filterCustomerViewModel.onSave();
        yield true;
      }

      default -> false;
    };
  }

  @NonNull
  public ListableFragmentBinding fragmentBinding() {
    return Objects.requireNonNull(this._fragmentBinding);
  }

  @NonNull
  public FilterCustomerAdapter adapter() {
    return Objects.requireNonNull(this._adapter);
  }

  @NonNull
  public FilterCustomerViewModel filterCustomerViewModel() {
    return Objects.requireNonNull(this._filterCustomerViewModel);
  }

  public void finish() {
    Objects.requireNonNull(this._fragmentBinding);

    Navigation.findNavController(this._fragmentBinding.getRoot()).popBackStack();
  }

  private class OnBackPressedHandler extends OnBackPressedCallback {
    public OnBackPressedHandler() {
      super(true);
    }

    @Override
    public void handleOnBackPressed() {
      FilterCustomerFragment.this.filterCustomerViewModel().onSave();
    }
  }
}
