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

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.robifr.ledger.R
import com.robifr.ledger.components.CustomerCardWideComponent
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.databinding.CustomerCardWideBinding
import com.robifr.ledger.ui.common.RecyclerViewHolder

class SelectCustomerListHolder(
    private val _cardBinding: CustomerCardWideBinding,
    private val _initialSelectedCustomerIds: () -> List<Long>,
    private val _customers: () -> List<CustomerModel>,
    private val _onCustomerSelected: (CustomerModel) -> Unit,
    private val _expandedCustomerIndex: () -> Int,
    private val _onExpandedCustomerIndexChanged: (Int) -> Unit
) : RecyclerViewHolder(_cardBinding.root), View.OnClickListener {
  private var _customerIndex: Int = -1
  private val _card: CustomerCardWideComponent =
      CustomerCardWideComponent(itemView.context, _cardBinding)

  init {
    _cardBinding.cardView.setOnClickListener { _onCustomerSelected(_customers()[_customerIndex]) }
    // Don't set menu button to gone as the position will be occupied by expand button.
    _cardBinding.normalCard.menuButton.isInvisible = true
    _cardBinding.normalCard.expandButton.isVisible = true
    _cardBinding.normalCard.expandButton.setOnClickListener(this)
    _cardBinding.expandedCard.menuButton.isInvisible = true
    _cardBinding.expandedCard.expandButton.isVisible = true
    _cardBinding.expandedCard.expandButton.setOnClickListener(this)
  }

  override fun bind(itemIndex: Int) {
    _customerIndex = itemIndex
    _card.reset()
    _card.setNormalCardCustomer(_customers()[_customerIndex])
    _card.setCardChecked(_initialSelectedCustomerIds().contains(_customers()[_customerIndex].id))
    setCardExpanded(
        _expandedCustomerIndex() != -1 &&
            _customers()[_customerIndex] == _customers()[_expandedCustomerIndex()])
  }

  override fun onClick(view: View?) {
    when (view?.id) {
      R.id.expandButton -> _onExpandedCustomerIndexChanged(_customerIndex)
    }
  }

  fun setCardExpanded(isExpanded: Boolean) {
    _card.setCardExpanded(isExpanded)
    // Only fill the view when it's shown on screen.
    if (isExpanded) _card.setExpandedCardCustomer(_customers()[_customerIndex])
  }
}
