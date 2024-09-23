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

package com.robifr.ledger.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.robifr.ledger.R;
import com.robifr.ledger.databinding.ListableFragmentBinding;
import com.robifr.ledger.ui.product.filter.ProductFilter;
import com.robifr.ledger.ui.product.recycler.ProductAdapter;
import com.robifr.ledger.ui.product.viewmodel.ProductViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Objects;

@AndroidEntryPoint
public class ProductFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
  @Nullable private ListableFragmentBinding _fragmentBinding;
  @Nullable private ProductSort _sort;
  @Nullable private ProductFilter _filter;
  @Nullable private ProductAdapter _adapter;

  @Nullable private ProductViewModel _productViewModel;
  @Nullable private ProductViewModelHandler _viewModelHandler;

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

    this._sort = new ProductSort(this);
    this._filter = new ProductFilter(this);
    this._adapter = new ProductAdapter(this);
    // Use activity store owner because this fragment is used by bottom navigation.
    // Which to prevents view model re-instantiation.
    this._productViewModel =
        new ViewModelProvider(this.requireActivity()).get(ProductViewModel.class);
    this._viewModelHandler = new ProductViewModelHandler(this, this._productViewModel);

    this._fragmentBinding.toolbar.getMenu().clear();
    this._fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_main);
    this._fragmentBinding.toolbar.setNavigationIcon(null);
    this._fragmentBinding.toolbar.setTitle(this.getString(R.string.app_name));
    this._fragmentBinding.toolbar.setOnMenuItemClickListener(this);
    this._fragmentBinding.sortByChip.setOnClickListener(chip -> this._sort.openDialog());
    this._fragmentBinding.filtersChip.setOnClickListener(chip -> this._filter.openDialog());
    this._fragmentBinding.recyclerView.setLayoutManager(
        new LinearLayoutManager(this.requireContext()));
    this._fragmentBinding.recyclerView.setAdapter(this._adapter);
    this._fragmentBinding.recyclerView.setItemViewCacheSize(0);
  }

  @Override
  public boolean onMenuItemClick(@NonNull MenuItem item) {
    Objects.requireNonNull(item);
    Objects.requireNonNull(this._fragmentBinding);

    return switch (item.getItemId()) {
      case R.id.search -> {
        Navigation.findNavController(this._fragmentBinding.getRoot()).navigate(R.id.searchFragment);
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
  public ProductSort sort() {
    return Objects.requireNonNull(this._sort);
  }

  @NonNull
  public ProductFilter filter() {
    return Objects.requireNonNull(this._filter);
  }

  @NonNull
  public ProductAdapter adapter() {
    return Objects.requireNonNull(this._adapter);
  }

  @NonNull
  public ProductViewModel productViewModel() {
    return Objects.requireNonNull(this._productViewModel);
  }
}
