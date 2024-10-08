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

package com.robifr.ledger.ui.createcustomer

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.robifr.ledger.R
import com.robifr.ledger.databinding.CreateCustomerFragmentBinding
import com.robifr.ledger.ui.FragmentResultKey
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerResultState
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerState
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerViewModel
import com.robifr.ledger.ui.createcustomer.viewmodel.CustomerBalanceAddState
import com.robifr.ledger.ui.createcustomer.viewmodel.CustomerBalanceWithdrawState
import com.robifr.ledger.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class CreateCustomerFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  protected var _fragmentBinding: CreateCustomerFragmentBinding? = null
  val fragmentBinding: CreateCustomerFragmentBinding
    get() = _fragmentBinding!!

  open val createCustomerViewModel: CreateCustomerViewModel by viewModels()
  private lateinit var _inputName: CreateCustomerName
  private lateinit var _inputBalance: CreateCustomerBalance
  private lateinit var _inputDebt: CreateCustomerDebt
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstance: Bundle?
  ): View? {
    _fragmentBinding = CreateCustomerFragmentBinding.inflate(inflater, container, false)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstance: Bundle?) {
    _inputName = CreateCustomerName(this)
    _inputBalance = CreateCustomerBalance(this)
    _inputDebt = CreateCustomerDebt(this)
    _onBackPressed = OnBackPressedHandler(this)
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_edit)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    with(createCustomerViewModel) {
      resultState.observe(viewLifecycleOwner) { it.handleIfNotHandled(::_onResultState) }
      snackbarState.observe(viewLifecycleOwner) { it.handleIfNotHandled(::_onSnackbarState) }
      uiState.observe(viewLifecycleOwner, ::_onUiState)
      balanceView.addBalanceState.observe(viewLifecycleOwner, ::_onInputBalanceAmountState)
      balanceView.withdrawBalanceState.observe(viewLifecycleOwner, ::_onInputWithdrawAmountState)
    }
  }

  override fun onMenuItemClick(item: MenuItem?): Boolean =
      when (item?.itemId) {
        R.id.save -> {
          createCustomerViewModel.onSave()
          true
        }
        else -> false
      }

  fun finish() {
    requireView().hideKeyboard()
    findNavController().popBackStack()
  }

  private fun _onResultState(state: CreateCustomerResultState) {
    state.createdCustomerId?.let {
      parentFragmentManager.setFragmentResult(
          Request.CREATE_CUSTOMER.key,
          Bundle().apply { putLong(Result.CREATED_CUSTOMER_ID_LONG.key, it) })
    }
    finish()
  }

  private fun _onSnackbarState(state: SnackbarState) {
    Snackbar.make(
            fragmentBinding.root.parent as View,
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onUiState(state: CreateCustomerState) {
    _inputName.setInputtedNameText(
        state.name, state.nameErrorMessageRes?.toStringValue(requireContext()))
    _inputBalance.setInputtedBalance(state.balance)
    _inputBalance.setAddBalanceButtonEnabled(state.isAddBalanceButtonEnabled)
    _inputBalance.setWithdrawBalanceButtonEnabled(state.isWithdrawBalanceButtonEnabled)
    _inputDebt.setInputtedDebt(state.debt, state.debtColorRes)
  }

  private fun _onInputBalanceAmountState(state: CustomerBalanceAddState) {
    _inputBalance.setAddBalanceDialogShown(state.isDialogShown)
    _inputBalance.setInputtedBalanceAmountText(state.formattedAmount)
  }

  private fun _onInputWithdrawAmountState(state: CustomerBalanceWithdrawState) {
    _inputBalance.setWithdrawBalanceDialogShown(state.isDialogShown)
    _inputBalance.setInputtedWithdrawAmountText(state.formattedAmount)
    _inputBalance.setAvailableAmountToWithdraw(state.availableAmountToWithdraw)
  }

  enum class Request : FragmentResultKey {
    CREATE_CUSTOMER
  }

  enum class Result : FragmentResultKey {
    CREATED_CUSTOMER_ID_LONG
  }
}

private class OnBackPressedHandler(private val _fragment: CreateCustomerFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    MaterialAlertDialogBuilder(_fragment.requireContext())
        .setMessage(R.string.createCustomer_unsavedChangesWarning)
        .setNegativeButton(R.string.action_discardAndLeave) { _, _ -> _fragment.finish() }
        .setPositiveButton(R.string.action_cancel) { dialog: DialogInterface?, _ ->
          dialog?.dismiss()
        }
        .show()
  }
}
