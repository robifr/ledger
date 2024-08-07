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

package com.robifr.ledger.ui.selectproduct.recycler;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.ProductModel;
import com.robifr.ledger.databinding.ListableListSelectedItemBinding;
import com.robifr.ledger.databinding.ProductCardWideBinding;
import com.robifr.ledger.ui.RecyclerViewHolder;
import com.robifr.ledger.ui.product.ProductCardNormalComponent;
import com.robifr.ledger.ui.selectproduct.SelectProductFragment;
import java.util.Objects;
import java.util.Optional;

public class SelectProductHeaderHolder extends RecyclerViewHolder<Optional<ProductModel>>
    implements View.OnClickListener {
  @NonNull private final SelectProductFragment _fragment;
  @NonNull private final ListableListSelectedItemBinding _headerBinding;
  @NonNull private final ProductCardWideBinding _selectedCardBinding;
  @NonNull private final ProductCardNormalComponent _selectedNormalCard;

  public SelectProductHeaderHolder(
      @NonNull SelectProductFragment fragment, @NonNull ListableListSelectedItemBinding binding) {
    super(binding.getRoot());
    this._fragment = Objects.requireNonNull(fragment);
    this._headerBinding = Objects.requireNonNull(binding);
    this._selectedCardBinding =
        ProductCardWideBinding.inflate(
            this._fragment.getLayoutInflater(), this._headerBinding.selectedItemContainer, false);
    this._selectedNormalCard =
        new ProductCardNormalComponent(
            this._fragment.requireContext(), this._selectedCardBinding.normalCard);

    this._headerBinding.selectedItemTitle.setText(
        this._fragment.getString(R.string.text_selected_product));
    this._headerBinding.selectedItemContainer.addView(this._selectedCardBinding.getRoot());
    this._headerBinding.allListTitle.setText(this._fragment.getString(R.string.text_all_products));
    this._headerBinding.newButton.setOnClickListener(this);
    // Don't set to `View.GONE` as the position will be occupied by checkbox.
    this._selectedCardBinding.normalCard.menuButton.setVisibility(View.INVISIBLE);
  }

  @Override
  public void bind(@NonNull Optional<ProductModel> selectedProduct) {
    if (!selectedProduct.isPresent()) {
      this._selectedCardBinding.cardView.setChecked(false);
      this._headerBinding.selectedItemDescription.setVisibility(View.GONE);
      this._headerBinding.selectedItemTitle.setVisibility(View.GONE);
      this._headerBinding.selectedItemContainer.setVisibility(View.GONE);
      return;
    }

    final ProductModel selectedProductOnDb =
        this._fragment.selectProductViewModel().products().getValue().stream()
            .filter(
                product -> product.id() != null && product.id().equals(selectedProduct.get().id()))
            .findFirst()
            .orElse(null);

    // The original product on database was deleted.
    if (selectedProductOnDb == null) {
      this._headerBinding.selectedItemDescription.setText(
          this._fragment.getString(R.string.text_originally_selected_product_was_deleted));
      this._headerBinding.selectedItemDescription.setVisibility(View.VISIBLE);

      // The original product on database was edited.
    } else if (!selectedProduct.get().equals(selectedProductOnDb)) {
      this._headerBinding.selectedItemDescription.setText(
          this._fragment.getString(R.string.text_originally_selected_product_was_changed));
      this._headerBinding.selectedItemDescription.setVisibility(View.VISIBLE);

      // It's the same unchanged product.
    } else {
      this._headerBinding.selectedItemDescription.setVisibility(View.GONE);
    }

    this._selectedCardBinding.cardView.setChecked(true);
    this._selectedNormalCard.setProduct(selectedProduct.orElse(null));
    this._headerBinding.selectedItemTitle.setVisibility(View.VISIBLE);
    this._headerBinding.selectedItemContainer.setVisibility(View.VISIBLE);
  }

  @Override
  public void onClick(@NonNull View view) {
    Objects.requireNonNull(view);

    switch (view.getId()) {
      case R.id.newButton ->
          Navigation.findNavController(this._fragment.fragmentBinding().getRoot())
              .navigate(R.id.createProductFragment);
    }
  }
}
