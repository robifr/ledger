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

package com.robifr.ledger.ui.customer.recycler

import com.robifr.ledger.components.CustomerCardWideComponent
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.CustomerPaginatedInfo
import com.robifr.ledger.databinding.CustomerCardWideBinding
import com.robifr.ledger.ui.common.RecyclerViewHolder

class CustomerListHolder(
    private val _cardBinding: CustomerCardWideBinding,
    private val _customers: () -> List<CustomerPaginatedInfo>,
    private val _onExpandedCustomerIndexChanged: (Int) -> Unit,
    private val _expandedCustomer: () -> CustomerModel?,
    private val _onCustomerMenuDialogShown: (selectedCustomer: CustomerPaginatedInfo) -> Unit
) : RecyclerViewHolder(_cardBinding.root) {
  private var _customerIndex: Int = -1
  private val _card: CustomerCardWideComponent =
      CustomerCardWideComponent(itemView.context, _cardBinding)

  init {
    _cardBinding.cardView.setOnClickListener { _onExpandedCustomerIndexChanged(_customerIndex) }
    _cardBinding.normalCard.menuButton.setOnClickListener {
      _onCustomerMenuDialogShown(_customers()[_customerIndex])
    }
    _cardBinding.expandedCard.menuButton.setOnClickListener {
      _onCustomerMenuDialogShown(_customers()[_customerIndex])
    }
  }

  override fun bind(itemIndex: Int) {
    _customerIndex = itemIndex
    _card.reset()
    _customers()[_customerIndex].fullModel?.let { _card.setNormalCardCustomer(it) }
    _setCardExpanded(_customers()[_customerIndex].id == _expandedCustomer()?.id)
  }

  private fun _setCardExpanded(isExpanded: Boolean) {
    _card.setCardExpanded(isExpanded)
    // Only fill the view when it's shown on screen.
    if (isExpanded)
        _customers()[_customerIndex].fullModel?.let { _card.setExpandedCardCustomer(it) }
  }
}
