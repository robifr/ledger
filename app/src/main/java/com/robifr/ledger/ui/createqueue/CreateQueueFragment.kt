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
import com.robifr.ledger.databinding.CreateQueueFragmentBinding
import com.robifr.ledger.ui.FragmentResultKey
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.createqueue.viewmodel.CreateQueueResultState
import com.robifr.ledger.ui.createqueue.viewmodel.CreateQueueState
import com.robifr.ledger.ui.createqueue.viewmodel.CreateQueueViewModel
import com.robifr.ledger.ui.createqueue.viewmodel.MakeProductOrderState
import com.robifr.ledger.ui.createqueue.viewmodel.SelectProductOrderState
import com.robifr.ledger.ui.selectcustomer.SelectCustomerFragment
import com.robifr.ledger.ui.selectproduct.SelectProductFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class CreateQueueFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  protected var _fragmentBinding: CreateQueueFragmentBinding? = null
  val fragmentBinding: CreateQueueFragmentBinding
    get() = _fragmentBinding!!

  open val createQueueViewModel: CreateQueueViewModel by viewModels()
  private lateinit var _inputCustomer: CreateQueueCustomer
  private lateinit var _inputDate: CreateQueueDate
  private lateinit var _inputStatus: CreateQueueStatus
  private lateinit var _inputPaymentMethod: CreateQueuePaymentMethod
  private lateinit var _inputProductOrder: CreateQueueProductOrder
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstance: Bundle?
  ): View? {
    _fragmentBinding = CreateQueueFragmentBinding.inflate(inflater, container, false)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstance: Bundle?) {
    _inputCustomer = CreateQueueCustomer(this)
    _inputDate = CreateQueueDate(this)
    _inputStatus = CreateQueueStatus(this)
    _inputPaymentMethod = CreateQueuePaymentMethod(this)
    _inputProductOrder = CreateQueueProductOrder(this)
    _onBackPressed = OnBackPressedHandler(this)
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_edit)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    with(createQueueViewModel) {
      resultState.observe(viewLifecycleOwner) { it.handleIfNotHandled(::_onResultState) }
      snackbarState.observe(viewLifecycleOwner) { it.handleIfNotHandled(::_onSnackbarState) }
      uiState.observe(viewLifecycleOwner, ::_onUiState)
      makeProductOrderView.uiState.observe(viewLifecycleOwner, ::_onMakeProductOrderState)
      selectProductOrderView.uiState.observe(viewLifecycleOwner, ::_onSelectProductOrderState)
    }
  }

  override fun onStart() {
    super.onStart()
    // Result should be called after all the view model state fully observed. `onStart` is perfect
    // place for it. If there's a fragment inherit from this class, then it's impossible to not call
    // them both inside `onViewCreated`, unless `super` call is omitted entirely.
    parentFragmentManager.setFragmentResultListener(
        SelectCustomerFragment.Request.SELECT_CUSTOMER.key,
        viewLifecycleOwner,
        ::_onSelectCustomerResult)
    parentFragmentManager.setFragmentResultListener(
        SelectProductFragment.Request.SELECT_PRODUCT.key,
        viewLifecycleOwner,
        ::_onSelectProductResult)
  }

  override fun onMenuItemClick(item: MenuItem?): Boolean =
      when (item?.itemId) {
        R.id.save -> {
          createQueueViewModel.onSave()
          true
        }
        else -> false
      }

  fun finish() {
    findNavController().popBackStack()
  }

  private fun _onResultState(state: CreateQueueResultState) {
    state.createdQueueId?.let {
      parentFragmentManager.setFragmentResult(
          Request.CREATE_QUEUE.key,
          Bundle().apply { putLong(Result.CREATED_QUEUE_ID_LONG.key, it) })
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

  private fun _onUiState(state: CreateQueueState) {
    val croppedCustomerName: String = state.customer?.name?.take(12) ?: ""
    _inputCustomer.setInputtedCustomer(state.customer, state.isCustomerEndIconVisible)
    _inputProductOrder.setCustomerBalanceAfterPaymentTitle(
        croppedCustomerName, state.isCustomerSummaryAfterPaymentVisible)
    _inputProductOrder.setCustomerBalanceAfterPayment(
        state.temporalCustomer?.balance, state.isCustomerSummaryAfterPaymentVisible)
    _inputProductOrder.setCustomerDebtAfterPaymentTitle(
        croppedCustomerName, state.isCustomerSummaryAfterPaymentVisible)
    _inputProductOrder.setCustomerDebtAfterPayment(
        state.temporalCustomer?.debt,
        state.customerDebtColor,
        state.isCustomerSummaryAfterPaymentVisible)

    _inputDate.setInputtedDate(state.date)
    _inputStatus.setInputtedStatus(state.status)
    _inputPaymentMethod.setInputtedPaymentMethod(state.paymentMethod)
    _inputPaymentMethod.setEnabledButtons(state.allowedPaymentMethods)
    _inputPaymentMethod.setVisible(state.isPaymentMethodVisible)
    _inputProductOrder.setInputtedProductOrders(state.productOrders)
    _inputProductOrder.setTotalDiscount(createQueueViewModel.inputtedQueue.totalDiscount())
    _inputProductOrder.setGrandTotalPrice(createQueueViewModel.inputtedQueue.grandTotalPrice())
  }

  private fun _onMakeProductOrderState(state: MakeProductOrderState) {
    if (state.isDialogShown) {
      if (state.productOrderToEdit != null) {
        _inputProductOrder.makeProductOrder.openEditDialog(state.isSaveButtonEnabled)
      } else {
        _inputProductOrder.makeProductOrder.openCreateDialog(state.isSaveButtonEnabled)
      }
    }
    _inputProductOrder.makeProductOrder.setInputtedProduct(state.product)
    _inputProductOrder.makeProductOrder.setInputtedQuantityText(state.formattedQuantity)
    _inputProductOrder.makeProductOrder.setInputtedDiscountText(state.formattedDiscount)
    _inputProductOrder.makeProductOrder.setInputtedTotalPrice(state.totalPrice)
  }

  private fun _onSelectProductOrderState(state: SelectProductOrderState) {
    _inputProductOrder.setContextualMode(state.isContextualModeActive)
    _inputProductOrder.setSelectedProductOrderByIndexes(state.selectedIndexes)
    // Disable every possible irrelevant action when contextual mode is on.
    fragmentBinding.customerLayout.setEnabled(!state.isContextualModeActive)
    fragmentBinding.customer.setEnabled(!state.isContextualModeActive)
    fragmentBinding.date.setEnabled(!state.isContextualModeActive)
    fragmentBinding.status.setEnabled(!state.isContextualModeActive)
    fragmentBinding.paymentMethodCashButton.setEnabled(!state.isContextualModeActive)
    fragmentBinding.paymentMethodAccountBalanceButton.setEnabled(!state.isContextualModeActive)
    fragmentBinding.productOrder.addButton.setEnabled(!state.isContextualModeActive)
  }

  private fun _onSelectCustomerResult(requestKey: String, result: Bundle) {
    when (SelectCustomerFragment.Request.entries.firstOrNull { it.key == requestKey }) {
      SelectCustomerFragment.Request.SELECT_CUSTOMER -> {
        val customerId: Long =
            result.getLong(SelectCustomerFragment.Result.SELECTED_CUSTOMER_ID_LONG.key)
        if (customerId != 0L) {
          createQueueViewModel
              .selectCustomerById(customerId)
              .observe(viewLifecycleOwner, createQueueViewModel::onCustomerChanged)
        }
      }
      null -> Unit
    }
  }

  private fun _onSelectProductResult(requestKey: String, result: Bundle) {
    when (SelectProductFragment.Request.entries.firstOrNull { it.key == requestKey }) {
      SelectProductFragment.Request.SELECT_PRODUCT -> {
        val productId: Long =
            result.getLong(SelectProductFragment.Result.SELECTED_PRODUCT_ID_LONG.key)
        if (productId != 0L) {
          createQueueViewModel
              .selectProductById(productId)
              .observe(
                  viewLifecycleOwner, createQueueViewModel.makeProductOrderView::onProductChanged)
        }
        createQueueViewModel.makeProductOrderView.onShowDialog(
            createQueueViewModel.makeProductOrderView.uiState.safeValue.productOrderToEdit)
      }
      null -> Unit
    }
  }

  enum class Request : FragmentResultKey {
    CREATE_QUEUE
  }

  enum class Result : FragmentResultKey {
    CREATED_QUEUE_ID_LONG
  }
}

private class OnBackPressedHandler(private val _fragment: CreateQueueFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    MaterialAlertDialogBuilder(_fragment.requireContext())
        .setMessage(R.string.createQueue_unsavedChangesWarning)
        .setNegativeButton(R.string.action_discardAndLeave) { _, _ -> _fragment.finish() }
        .setPositiveButton(R.string.action_cancel) { dialog: DialogInterface?, _ ->
          dialog?.dismiss()
        }
        .show()
  }
}
