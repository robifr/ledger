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

package com.robifr.ledger.ui.searchcustomer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.robifr.ledger.R;
import com.robifr.ledger.databinding.SearchableFragmentBinding;
import com.robifr.ledger.ui.BackStack;
import com.robifr.ledger.ui.FragmentResultKey;
import com.robifr.ledger.ui.searchcustomer.recycler.SearchCustomerAdapter;
import com.robifr.ledger.ui.searchcustomer.viewmodel.SearchCustomerViewModel;
import com.robifr.ledger.util.Compats;
import java.util.Objects;

public class SearchCustomerFragment extends Fragment implements SearchView.OnQueryTextListener {
  public enum Request implements FragmentResultKey {
    SELECT_CUSTOMER;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  public enum Result implements FragmentResultKey {
    SELECTED_CUSTOMER_ID;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  @NonNull private final OnBackPressedHandler _onBackPressed = new OnBackPressedHandler();
  @Nullable private final String _initialQuery;
  @Nullable private SearchableFragmentBinding _fragmentBinding;
  @Nullable private SearchCustomerAdapter _adapter;
  @ColorInt private int _normalStatusBarColor;

  @Nullable private SearchCustomerViewModel _searchCustomerViewModel;
  @Nullable private SearchCustomerViewModelHandler _viewModelHandler;

  /** Default constructor when configuration changes. */
  public SearchCustomerFragment() {
    this(null);
  }

  private SearchCustomerFragment(@Nullable String initialQuery) {
    this._initialQuery = initialQuery;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstance) {
    Objects.requireNonNull(inflater);

    this._fragmentBinding = SearchableFragmentBinding.inflate(inflater, container, false);
    return this._fragmentBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {
    Objects.requireNonNull(view);
    Objects.requireNonNull(this._fragmentBinding);

    this._adapter = new SearchCustomerAdapter(this);
    this._normalStatusBarColor = this.requireActivity().getWindow().getStatusBarColor();
    this._searchCustomerViewModel =
        new ViewModelProvider(this, new SearchCustomerViewModel.Factory(this.requireContext()))
            .get(SearchCustomerViewModel.class);
    this._viewModelHandler =
        new SearchCustomerViewModelHandler(this, this._searchCustomerViewModel);

    this.requireActivity()
        .getOnBackPressedDispatcher()
        .addCallback(this.getViewLifecycleOwner(), this._onBackPressed);
    this.requireActivity()
        .getWindow() // Match status bar color with toolbar.
        .setStatusBarColor(this.requireContext().getColor(R.color.surface));
    this._fragmentBinding.toolbar.setNavigationOnClickListener(
        v -> this._onBackPressed.handleOnBackPressed());
    this._fragmentBinding.seachView.setQueryHint(this.getString(R.string.text_search_customers));
    this._fragmentBinding.seachView.setOnQueryTextListener(this);
    this._fragmentBinding.seachView.requestFocus();
    this._fragmentBinding.noResultsImage.image.setImageResource(R.drawable.image_noresultsfound);
    this._fragmentBinding.noResultsImage.title.setText(R.string.text_no_results_found);
    this._fragmentBinding.noResultsImage.description.setText(
        this.getString(R.string.text_cant_find_any_matching_customers));
    this._fragmentBinding.recyclerView.setLayoutManager(
        new LinearLayoutManager(this.requireContext()));
    this._fragmentBinding.recyclerView.setAdapter(this._adapter);
    this._fragmentBinding.recyclerView.setItemViewCacheSize(0);

    if (this._initialQuery != null) {
      this._fragmentBinding.seachView.setQuery(this._initialQuery, true);
    } else {
      Compats.showKeyboard(this.requireContext(), this._fragmentBinding.seachView);
    }
  }

  @Override
  public boolean onQueryTextSubmit(@NonNull String query) {
    return false;
  }

  @Override
  public boolean onQueryTextChange(@NonNull String newText) {
    Objects.requireNonNull(this._searchCustomerViewModel);

    this._searchCustomerViewModel.onSearch(newText);
    return true;
  }

  @NonNull
  public SearchableFragmentBinding fragmentBinding() {
    return Objects.requireNonNull(this._fragmentBinding);
  }

  @NonNull
  public SearchCustomerAdapter adapter() {
    return Objects.requireNonNull(this._adapter);
  }

  @NonNull
  public SearchCustomerViewModel searchCustomerViewModel() {
    return Objects.requireNonNull(this._searchCustomerViewModel);
  }

  public void finish() {
    Objects.requireNonNull(this._fragmentBinding);

    if (this.requireActivity() instanceof BackStack navigation
        && navigation.currentTabStackTag() != null) {
      Compats.hideKeyboard(this.requireContext(), this.requireView().findFocus());
      this.requireActivity().getWindow().setStatusBarColor(this._normalStatusBarColor);
      navigation.popFragmentStack(navigation.currentTabStackTag());
    }
  }

  public static class Factory extends FragmentFactory {
    @Nullable private final String _initialQuery;

    public Factory(@Nullable String initialQuery) {
      this._initialQuery = initialQuery;
    }

    @Override
    @NonNull
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
      Objects.requireNonNull(classLoader);
      Objects.requireNonNull(className);

      return (className.equals(SearchCustomerFragment.class.getName()))
          ? new SearchCustomerFragment(this._initialQuery)
          : super.instantiate(classLoader, className);
    }
  }

  private class OnBackPressedHandler extends OnBackPressedCallback {
    public OnBackPressedHandler() {
      super(true);
    }

    @Override
    public void handleOnBackPressed() {
      SearchCustomerFragment.this.searchCustomerViewModel().onCustomerSelected(null);
    }
  }
}
