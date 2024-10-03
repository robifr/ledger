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

package com.robifr.ledger.ui.editproduct;

import android.os.Bundle;
import android.view.View;
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
    INITIAL_PRODUCT_ID_TO_EDIT_LONG
  }

  public enum Request implements FragmentResultKey {
    EDIT_PRODUCT
  }

  public enum Result implements FragmentResultKey {
    EDITED_PRODUCT_ID_LONG
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {
    super.onViewCreated(view, savedInstance);
    Objects.requireNonNull(this._fragmentBinding);

    this._createProductViewModel = new ViewModelProvider(this).get(EditProductViewModel.class);
    this._viewModelHandler =
        new EditProductViewModelHandler(this, (EditProductViewModel) this._createProductViewModel);

    this._fragmentBinding.toolbar.setTitle(R.string.editProduct);
  }
}
