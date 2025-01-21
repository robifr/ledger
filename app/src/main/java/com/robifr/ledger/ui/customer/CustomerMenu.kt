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

package com.robifr.ledger.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.robifr.ledger.R
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.databinding.CustomerCardDialogMenuBinding
import com.robifr.ledger.ui.editcustomer.EditCustomerFragment

class CustomerMenu(private val _fragment: Fragment, private val _onDialogClosed: () -> Unit) {
  private val _dialogBinding: CustomerCardDialogMenuBinding =
      CustomerCardDialogMenuBinding.inflate(LayoutInflater.from(_fragment.requireContext()))
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_fragment.requireContext(), R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
        setOnDismissListener { _onDialogClosed() }
      }

  fun showDialog(selectedCustomer: CustomerModel, onDeleteCustomer: (CustomerModel) -> Unit) {
    _dialogBinding.editButton.setOnClickListener {
      val customerId: Long = selectedCustomer.id ?: return@setOnClickListener
      _fragment
          .findNavController()
          .navigate(
              R.id.editCustomerFragment,
              Bundle().apply {
                putLong(
                    EditCustomerFragment.Arguments.INITIAL_CUSTOMER_ID_TO_EDIT_LONG.key(),
                    customerId)
              })
      _onDialogClosed()
    }
    _dialogBinding.deleteButton.setOnClickListener {
      MaterialAlertDialogBuilder(_fragment.requireContext())
          .setTitle(
              HtmlCompat.fromHtml(
                  _fragment.requireContext().getString(R.string.customer_cardMenu_deleteWarning),
                  HtmlCompat.FROM_HTML_MODE_LEGACY))
          .setMessage(
              HtmlCompat.fromHtml(
                  _fragment
                      .requireContext()
                      .getString(
                          R.string.customer_cardMenu_deleteWarning_description,
                          selectedCustomer.name),
                  HtmlCompat.FROM_HTML_MODE_LEGACY))
          .setNegativeButton(R.string.action_delete) { _, _ -> onDeleteCustomer(selectedCustomer) }
          .setPositiveButton(R.string.action_cancel) { _, _ -> }
          .show()
    }
    _dialog.show()
  }

  fun dismissDialog() {
    _dialog.dismiss()
  }
}
