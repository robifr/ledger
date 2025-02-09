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

package com.robifr.ledger.ui.filtercustomer.recycler

import android.view.LayoutInflater
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.chip.ChipGroup
import com.robifr.ledger.R
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.databinding.ListableListSelectedItemBinding
import com.robifr.ledger.databinding.ReusableChipInputBinding
import com.robifr.ledger.ui.common.RecyclerViewHolder

class FilterCustomerHeaderHolder(
    private val _selectedItemBinding: ListableListSelectedItemBinding,
    private val _filteredCustomers: () -> List<CustomerModel>,
    private val _onCustomerCheckedChanged: (CustomerModel) -> Unit
) : RecyclerViewHolder(_selectedItemBinding.root) {
  private val _chipGroup: ChipGroup =
      ChipGroup(itemView.context).apply {
        layoutParams =
            ChipGroup.LayoutParams(
                ChipGroup.LayoutParams.MATCH_PARENT, ChipGroup.LayoutParams.WRAP_CONTENT)
        updatePadding(top = itemView.context.resources.getDimension(R.dimen.space_small).toInt())
        isSingleLine = false
        chipSpacingVertical =
            itemView.context.resources.getDimension(R.dimen.space_smallMedium).toInt()
        chipSpacingHorizontal =
            itemView.context.resources.getDimension(R.dimen.space_smallMedium).toInt()
      }

  init {
    _selectedItemBinding.selectedItemContainer.addView(_chipGroup)
    _selectedItemBinding.selectedItemTitle.setText(R.string.filterCustomer_filteredCustomers)
    _selectedItemBinding.selectedItemDescription.isGone = true
    _selectedItemBinding.allListTitle.setText(R.string.filterCustomer_allCustomers)
    _selectedItemBinding.newButton.isGone = true
  }

  override fun bind(itemIndex: Int) {
    _selectedItemBinding.selectedItemTitle.isVisible = _filteredCustomers().isNotEmpty()
    _selectedItemBinding.selectedItemContainer.isVisible = _filteredCustomers().isNotEmpty()

    for ((i, filteredCustomer) in _filteredCustomers().withIndex()) {
      val chipBinding: ReusableChipInputBinding =
          if (i >= _chipGroup.childCount) {
            ReusableChipInputBinding.inflate(
                LayoutInflater.from(itemView.context), _chipGroup, false)
          } else {
            ReusableChipInputBinding.bind(_chipGroup.getChildAt(i))
          }
      chipBinding.apply {
        chip.text = filteredCustomer.name
        chip.setOnClickListener { _onCustomerCheckedChanged(filteredCustomer) }
      }
      if (chipBinding.chip.parent == null) _chipGroup.addView(chipBinding.chip)
    }
    // Delete old unused chips those are previously used by old customer.
    if (_chipGroup.childCount > _filteredCustomers().size) {
      _chipGroup.removeViews(
          _filteredCustomers().size, _chipGroup.childCount - _filteredCustomers().size)
    }
  }
}
