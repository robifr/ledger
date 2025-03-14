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

package io.github.robifr.ledger.ui.selectproduct.recycler

import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import io.github.robifr.ledger.R
import io.github.robifr.ledger.components.ProductCardWideComponent
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.databinding.ListableListSelectedItemBinding
import io.github.robifr.ledger.databinding.ProductCardWideBinding
import io.github.robifr.ledger.ui.common.RecyclerViewHolder

class SelectProductHeaderHolder(
    private val _selectedItemBinding: ListableListSelectedItemBinding,
    private val _initialSelectedProduct: () -> ProductModel?,
    private val _selectedItemDescription: () -> String?,
    private val _isSelectedProductPreviewExpanded: () -> Boolean,
    private val _onSelectedProductPreviewExpanded: (Boolean) -> Unit
) : RecyclerViewHolder(_selectedItemBinding.root), View.OnClickListener {
  private val _selectedCardBinding: ProductCardWideBinding =
      ProductCardWideBinding.inflate(
          LayoutInflater.from(itemView.context), _selectedItemBinding.selectedItemContainer, false)
  private val _selectedCard: ProductCardWideComponent =
      ProductCardWideComponent(itemView.context, _selectedCardBinding)

  init {
    _selectedItemBinding.selectedItemTitle.setText(R.string.selectProduct_selectedProduct)
    _selectedItemBinding.selectedItemContainer.addView(_selectedCardBinding.root)
    _selectedItemBinding.allListTitle.setText(R.string.selectProduct_allProducts)
    _selectedItemBinding.newButton.setOnClickListener {
      itemView.findNavController().navigate(R.id.createProductFragment)
    }
    // Don't set menu button to gone as the position will be occupied by expand button.
    _selectedCardBinding.normalCard.menuButton.isInvisible = true
    _selectedCardBinding.normalCard.expandButton.isVisible = true
    _selectedCardBinding.normalCard.expandButton.setOnClickListener(this)
    _selectedCardBinding.expandedCard.menuButton.isInvisible = true
    _selectedCardBinding.expandedCard.expandButton.isVisible = true
    _selectedCardBinding.expandedCard.expandButton.setOnClickListener(this)
  }

  override fun bind(itemIndex: Int) {
    _selectedCard.reset()
    _selectedItemBinding.selectedItemTitle.isVisible = _initialSelectedProduct() != null
    _selectedItemBinding.selectedItemContainer.isVisible = _initialSelectedProduct() != null
    _selectedItemBinding.selectedItemDescription.text = _selectedItemDescription()
    _selectedItemBinding.selectedItemDescription.isVisible = _selectedItemDescription() != null
    _initialSelectedProduct()?.let {
      _selectedCard.setNormalCardProduct(it)
      _selectedCard.setExpandedCardProduct(it)
      _selectedCard.setCardChecked(true)
      _selectedCard.setCardExpanded(_isSelectedProductPreviewExpanded())
    }
  }

  override fun onClick(view: View?) {
    when (view?.id) {
      R.id.expandButton -> {
        val isExpanded: Boolean = _selectedCardBinding.expandedCard.root.isVisible
        _onSelectedProductPreviewExpanded(!isExpanded)
      }
    }
  }
}
