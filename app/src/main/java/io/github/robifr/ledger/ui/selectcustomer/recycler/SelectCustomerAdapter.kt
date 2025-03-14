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

package io.github.robifr.ledger.ui.selectcustomer.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.robifr.ledger.databinding.CustomerCardWideBinding
import io.github.robifr.ledger.databinding.ListableListSelectedItemBinding
import io.github.robifr.ledger.ui.common.RecyclerViewHolder
import io.github.robifr.ledger.ui.selectcustomer.SelectCustomerFragment

class SelectCustomerAdapter(private val _fragment: SelectCustomerFragment) :
    RecyclerView.Adapter<RecyclerViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
    return when (ViewType.entries.find { it.value == viewType }) {
      ViewType.HEADER ->
          SelectCustomerHeaderHolder(
              _selectedItemBinding =
                  ListableListSelectedItemBinding.inflate(_fragment.layoutInflater, parent, false),
              _initialSelectedCustomer = {
                _fragment.selectCustomerViewModel.uiState.safeValue.initialSelectedCustomer
              },
              _selectedItemDescription = {
                _fragment.selectCustomerViewModel.uiState.safeValue.selectedItemDescriptionStringRes
                    ?.let { _fragment.getString(it) }
              },
              _isSelectedCustomerPreviewExpanded = {
                _fragment.selectCustomerViewModel.uiState.safeValue
                    .isSelectedCustomerPreviewExpanded
              },
              _onSelectedCustomerPreviewExpanded =
                  _fragment.selectCustomerViewModel::onSelectedCustomerPreviewExpanded)
      else ->
          SelectCustomerListHolder(
              _cardBinding =
                  CustomerCardWideBinding.inflate(_fragment.layoutInflater, parent, false),
              _initialSelectedCustomerIds = {
                listOfNotNull(
                    _fragment.selectCustomerViewModel.uiState.safeValue.initialSelectedCustomer?.id)
              },
              _customers = {
                _fragment.selectCustomerViewModel.uiState.safeValue.pagination.paginatedItems
              },
              _onCustomerSelected = _fragment.selectCustomerViewModel::onCustomerSelected,
              _onExpandedCustomerIndexChanged =
                  _fragment.selectCustomerViewModel::onExpandedCustomerIndexChanged,
              _expandedCustomer = {
                _fragment.selectCustomerViewModel.uiState.safeValue.expandedCustomer
              })
    }
  }

  override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
    when (holder) {
      is SelectCustomerHeaderHolder -> holder.bind()
      is SelectCustomerListHolder -> holder.bind(position - 1)
    }
  }

  override fun getItemCount(): Int =
      // +1 offset because header holder.
      _fragment.selectCustomerViewModel.uiState.safeValue.pagination.paginatedItems.size + 1

  override fun getItemViewType(position: Int): Int =
      when (position) {
        0 -> ViewType.HEADER.value
        else -> ViewType.LIST.value
      }

  private enum class ViewType(val value: Int) {
    HEADER(0),
    LIST(1)
  }
}
