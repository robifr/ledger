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

package com.robifr.ledger.ui.queue.filter

import android.os.Bundle
import android.widget.CompoundButton
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.robifr.ledger.R
import com.robifr.ledger.data.display.QueueFilters
import com.robifr.ledger.databinding.QueueDialogFilterBinding
import com.robifr.ledger.ui.filtercustomer.FilterCustomerFragment
import com.robifr.ledger.ui.queue.QueueFragment

class QueueFilterCustomer(
    dialog: BottomSheetDialog,
    private val _fragment: QueueFragment,
    private val _dialogBinding: QueueDialogFilterBinding
) : CompoundButton.OnCheckedChangeListener {
  init {
    _dialogBinding.filterCustomer.filterCustomerButton.setOnClickListener {
      _fragment
          .findNavController()
          .navigate(
              R.id.filterCustomerFragment,
              Bundle().apply {
                putLongArray(
                    FilterCustomerFragment.Arguments.INITIAL_FILTERED_CUSTOMER_IDS_LONG_ARRAY.key(),
                    _fragment.queueViewModel.filterView.uiState.safeValue.customerIds.toLongArray())
              })
      dialog.dismiss()
    }
    _dialogBinding.filterCustomer.showNullCustomer.setOnClickListener {
      _dialogBinding.filterCustomer.showNullCustomerCheckbox.performClick()
    }
    _dialogBinding.filterCustomer.showNullCustomerCheckbox.setOnCheckedChangeListener(this)
  }

  override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
    when (buttonView?.id) {
      R.id.showNullCustomerCheckbox ->
          _fragment.queueViewModel.filterView.onNullCustomerShown(isChecked)
    }
  }

  /** @param isShown [QueueFilters.isNullCustomerShown] */
  fun setNullCustomerShown(isShown: Boolean) {
    // Remove listener to prevent unintended updates to both view model
    // and the switch itself when manually set the switch.
    _dialogBinding.filterCustomer.showNullCustomerCheckbox.setOnCheckedChangeListener(null)
    _dialogBinding.filterCustomer.showNullCustomerCheckbox.isChecked = isShown
    _dialogBinding.filterCustomer.showNullCustomerCheckbox.setOnCheckedChangeListener(this)
  }
}
