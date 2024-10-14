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

package com.robifr.ledger.ui.createqueue

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import com.robifr.ledger.R
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.ui.selectcustomer.SelectCustomerFragment

class CreateQueueCustomer(private val _fragment: CreateQueueFragment) {
  init {
    _fragment.fragmentBinding.customerLayout.setEndIconVisible(false)
    _fragment.fragmentBinding.customerLayout.setEndIconOnClickListener {
      _fragment.createQueueViewModel.onCustomerChanged(null)
    }
    _fragment.fragmentBinding.customer.setOnClickListener {
      _fragment
          .findNavController()
          .navigate(
              R.id.selectCustomerFragment,
              Bundle().apply {
                putParcelable(
                    SelectCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_PARCELABLE.key,
                    _fragment.createQueueViewModel.uiState.safeValue.customer)
              })
    }
  }

  fun setInputtedCustomer(customer: CustomerModel?, isEndIconVisible: Boolean) {
    _fragment.fragmentBinding.customer.setText(customer?.name)
    _fragment.fragmentBinding.customerLayout.setEndIconVisible(isEndIconVisible)
  }
}
