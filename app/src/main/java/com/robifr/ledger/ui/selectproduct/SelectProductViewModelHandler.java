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

package com.robifr.ledger.ui.selectproduct;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.ui.LiveDataEvent.Observer;
import com.robifr.ledger.ui.StringResources;
import com.robifr.ledger.ui.selectproduct.viewmodel.SelectProductViewModel;
import java.util.List;
import java.util.Objects;

public class SelectProductViewModelHandler {
  @NonNull private final SelectProductFragment _fragment;
  @NonNull private final SelectProductViewModel _viewModel;

  public SelectProductViewModelHandler(
      @NonNull SelectProductFragment fragment, @NonNull SelectProductViewModel viewModel) {
    this._fragment = Objects.requireNonNull(fragment);
    this._viewModel = Objects.requireNonNull(viewModel);

    this._viewModel
        .resultSelectedProductId()
        .observe(
            this._fragment.getViewLifecycleOwner(),
            new Observer<>(this::_onResultSelectedProductId));
    this._viewModel
        .snackbarMessage()
        .observe(this._fragment.getViewLifecycleOwner(), new Observer<>(this::_onSnackbarMessage));
    this._viewModel.products().observe(this._fragment.getViewLifecycleOwner(), this::_onProducts);
  }

  private void _onResultSelectedProductId(@Nullable Long productId) {
    final Bundle bundle = new Bundle();

    if (productId != null) {
      bundle.putLong(SelectProductFragment.Result.SELECTED_PRODUCT_ID.key(), productId);
    }

    this._fragment
        .getParentFragmentManager()
        .setFragmentResult(SelectProductFragment.Request.SELECT_PRODUCT.key(), bundle);
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

  private void _onProducts(@Nullable List<ProductModel> products) {
    this._fragment.adapter().notifyDataSetChanged();
  }
}
