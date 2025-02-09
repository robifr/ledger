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

package com.robifr.ledger.ui.selectcustomer.recycler

import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.robifr.ledger.R
import com.robifr.ledger.components.CustomerCardWideComponent
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.databinding.CustomerCardWideBinding
import com.robifr.ledger.databinding.ListableListSelectedItemBinding
import com.robifr.ledger.ui.common.RecyclerViewHolder

class SelectCustomerHeaderHolder(
    private val _selectedItemBinding: ListableListSelectedItemBinding,
    private val _initialSelectedCustomer: () -> CustomerModel?,
    private val _selectedItemDescription: () -> String?,
    private val _isSelectedCustomerPreviewExpanded: () -> Boolean,
    private val _onSelectedCustomerPreviewExpanded: (Boolean) -> Unit
) : RecyclerViewHolder(_selectedItemBinding.root), View.OnClickListener {
  private val _selectedCardBinding: CustomerCardWideBinding =
      CustomerCardWideBinding.inflate(
          LayoutInflater.from(itemView.context), _selectedItemBinding.selectedItemContainer, false)
  private val _selectedCard: CustomerCardWideComponent =
      CustomerCardWideComponent(itemView.context, _selectedCardBinding)

  init {
    _selectedItemBinding.selectedItemTitle.setText(R.string.selectCustomer_selectedCustomer)
    _selectedItemBinding.selectedItemContainer.addView(_selectedCardBinding.root)
    _selectedItemBinding.allListTitle.setText(R.string.selectCustomer_allCustomers)
    _selectedItemBinding.newButton.setOnClickListener {
      itemView.findNavController().navigate(R.id.createCustomerFragment)
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
    _selectedItemBinding.selectedItemTitle.isVisible = _initialSelectedCustomer() != null
    _selectedItemBinding.selectedItemContainer.isVisible = _initialSelectedCustomer() != null
    _selectedItemBinding.selectedItemDescription.text = _selectedItemDescription()
    _selectedItemBinding.selectedItemDescription.isVisible = _selectedItemDescription() != null
    _initialSelectedCustomer()?.let {
      _selectedCard.setNormalCardCustomer(it)
      _selectedCard.setExpandedCardCustomer(it)
      _selectedCard.setCardChecked(true)
      _selectedCard.setCardExpanded(_isSelectedCustomerPreviewExpanded())
    }
  }

  override fun onClick(view: View?) {
    when (view?.id) {
      R.id.expandButton -> {
        val isExpanded: Boolean = _selectedCardBinding.expandedCard.root.isVisible
        _onSelectedCustomerPreviewExpanded(!isExpanded)
      }
    }
  }
}
