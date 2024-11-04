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

import android.os.Bundle
import android.view.LayoutInflater
import androidx.navigation.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.robifr.ledger.R
import com.robifr.ledger.databinding.CustomerCardDialogMenuBinding
import com.robifr.ledger.ui.editcustomer.EditCustomerFragment

class CustomerListMenu(private val _holder: CustomerListHolder) {
  private val _dialogBinding: CustomerCardDialogMenuBinding =
      CustomerCardDialogMenuBinding.inflate(LayoutInflater.from(_holder.itemView.context)).apply {
        editButton.setOnClickListener {
          val customerId: Long =
              _holder._customers()[_holder._customerIndex].id ?: return@setOnClickListener
          _holder.itemView
              .findNavController()
              .navigate(
                  R.id.editCustomerFragment,
                  Bundle().apply {
                    putLong(
                        EditCustomerFragment.Arguments.INITIAL_CUSTOMER_ID_TO_EDIT_LONG.key,
                        customerId)
                  })
          _dialog.dismiss()
        }
        deleteButton.setOnClickListener {
          _holder._onDeleteCustomer(_holder._customers()[_holder._customerIndex])
          _dialog.dismiss()
        }
      }
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_holder.itemView.context, R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
      }

  fun openDialog() {
    _dialog.show()
  }
}
