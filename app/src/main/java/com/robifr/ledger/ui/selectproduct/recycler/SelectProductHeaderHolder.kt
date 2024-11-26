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

package com.robifr.ledger.ui.selectproduct.recycler

import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.robifr.ledger.R
import com.robifr.ledger.components.ProductCardWideComponent
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.databinding.ListableListSelectedItemBinding
import com.robifr.ledger.databinding.ProductCardWideBinding
import com.robifr.ledger.ui.RecyclerViewHolder

class SelectProductHeaderHolder(
    private val _selectedItemBinding: ListableListSelectedItemBinding,
    private val _initialSelectedProduct: () -> ProductModel?,
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
    _initialSelectedProduct()?.let {
      _selectedCard.setNormalCardProduct(it)
      _selectedCard.setExpandedCardProduct(it)
      _selectedCard.setCardChecked(true)
      setCardExpanded(_isSelectedProductPreviewExpanded())
    }
  }

  override fun onClick(view: View?) {
    when (view?.id) {
      R.id.expandButton -> {
        val isExpanded: Boolean = _selectedCardBinding.expandedCard.root.isVisible
        _onSelectedProductPreviewExpanded(!isExpanded)
        // Display ripple effect. The effect is gone due to the clicked view
        // set to gone when the card expand/collapse.
        if (isExpanded) {
          _selectedCardBinding.normalCard.expandButton.isPressed = true
          _selectedCardBinding.normalCard.expandButton.isPressed = false
        } else {
          _selectedCardBinding.expandedCard.expandButton.isPressed = true
          _selectedCardBinding.expandedCard.expandButton.isPressed = false
        }
      }
    }
  }

  fun setCardExpanded(isExpanded: Boolean) {
    _selectedCard.setCardExpanded(isExpanded)
  }

  fun setSelectedItemDescription(text: String?, isVisible: Boolean) {
    _selectedItemBinding.selectedItemDescription.text = text
    _selectedItemBinding.selectedItemDescription.isVisible = isVisible
  }
}
