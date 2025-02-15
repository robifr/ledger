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

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.robifr.ledger.databinding.CustomerCardWideBinding
import com.robifr.ledger.databinding.ListableListSelectedItemBinding
import com.robifr.ledger.ui.common.RecyclerViewHolder
import com.robifr.ledger.ui.filtercustomer.FilterCustomerFragment

class FilterCustomerAdapter(private val _fragment: FilterCustomerFragment) :
    RecyclerView.Adapter<RecyclerViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder =
      when (ViewType.entries.find { it.value == viewType }) {
        ViewType.HEADER ->
            FilterCustomerHeaderHolder(
                _selectedItemBinding =
                    ListableListSelectedItemBinding.inflate(
                        _fragment.layoutInflater, parent, false),
                _filteredCustomers = {
                  _fragment.filterCustomerViewModel.uiState.safeValue.filteredCustomers
                },
                _onCustomerCheckedChanged =
                    _fragment.filterCustomerViewModel::onCustomerCheckedChanged)
        else ->
            FilterCustomerListHolder(
                _cardBinding =
                    CustomerCardWideBinding.inflate(_fragment.layoutInflater, parent, false),
                _customers = {
                  _fragment.filterCustomerViewModel.uiState.safeValue.pagination.paginatedItems
                },
                _onCustomerCheckedChanged =
                    _fragment.filterCustomerViewModel::onCustomerCheckedChanged,
                _filteredCustomers = {
                  _fragment.filterCustomerViewModel.uiState.safeValue.filteredCustomers
                },
                _onExpandedCustomerIndexChanged =
                    _fragment.filterCustomerViewModel::onExpandedCustomerIndexChanged,
                _expandedCustomer = {
                  _fragment.filterCustomerViewModel.uiState.safeValue.expandedCustomer
                })
      }

  override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
    when (holder) {
      is FilterCustomerHeaderHolder -> holder.bind()
      is FilterCustomerListHolder -> holder.bind(position - 1)
    }
  }

  override fun getItemCount(): Int =
      // +1 offset because header holder.
      _fragment.filterCustomerViewModel.uiState.safeValue.pagination.paginatedItems.size + 1

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
