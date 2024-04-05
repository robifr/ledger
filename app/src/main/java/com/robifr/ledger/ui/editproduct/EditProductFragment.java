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

package com.robifr.ledger.ui.editproduct;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.robifr.ledger.R;
import com.robifr.ledger.ui.FragmentResultKey;
import com.robifr.ledger.ui.createproduct.CreateProductFragment;
import com.robifr.ledger.ui.editproduct.viewmodel.EditProductViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Objects;

@AndroidEntryPoint
public class EditProductFragment extends CreateProductFragment {
  public enum Arguments implements FragmentResultKey {
    INITIAL_PRODUCT_ID_TO_EDIT;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  public enum Request implements FragmentResultKey {
    EDIT_PRODUCT;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  public enum Result implements FragmentResultKey {
    EDITED_PRODUCT_ID;

    @Override
    @NonNull
    public String key() {
      return FragmentResultKey.generateKey(this);
    }
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this._createProductViewModel = new ViewModelProvider(this).get(EditProductViewModel.class);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstance) {
    Objects.requireNonNull(this._createProductViewModel);

    final View view = super.onCreateView(inflater, container, savedInstance);
    this._viewModelHandler =
        new EditProductViewModelHandler(this, (EditProductViewModel) this._createProductViewModel);

    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {
    super.onViewCreated(view, savedInstance);
    Objects.requireNonNull(this._fragmentBinding);

    this._fragmentBinding.toolbar.setTitle(this.getString(R.string.text_edit_product));
  }
}
