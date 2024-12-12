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
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.QueueModel
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
import com.robifr.ledger.util.getColorAttr
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
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = CreateQueueFragmentBinding.inflate(inflater, container, false)
    _inputCustomer = CreateQueueCustomer(this)
    _inputDate = CreateQueueDate(this)
    _inputStatus = CreateQueueStatus(this)
    _inputPaymentMethod = CreateQueuePaymentMethod(this)
    _inputProductOrder = CreateQueueProductOrder(this)
    _onBackPressed = OnBackPressedHandler(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    requireActivity().window.statusBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().window.navigationBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_edit)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    createQueueViewModel.resultState.observe(viewLifecycleOwner, ::_onResultState)
    createQueueViewModel.snackbarState.observe(viewLifecycleOwner, ::_onSnackbarState)
    createQueueViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
    createQueueViewModel.makeProductOrderView.uiState.observe(
        viewLifecycleOwner, ::_onMakeProductOrderState)
    createQueueViewModel.selectProductOrderView.uiState.observe(
        viewLifecycleOwner, ::_onSelectProductOrderState)
  }

  override fun onStart() {
    super.onStart()
    // Result should be called after all the view model state fully observed.
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
    parentFragmentManager.setFragmentResult(
        Request.CREATE_QUEUE.key,
        Bundle().apply {
          state.createdQueueId?.let { putLong(Result.CREATED_QUEUE_ID_LONG.key, it) }
        })
    finish()
  }

  private fun _onSnackbarState(state: SnackbarState) {
    Snackbar.make(
            fragmentBinding.root as View,
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onUiState(state: CreateQueueState) {
    val croppedCustomerName: String = state.customer?.name?.take(12) ?: ""
    _inputCustomer.setInputtedCustomer(state.customer, state.isCustomerEndIconVisible)
    _inputProductOrder.setTemporalCustomerBalanceTitle(
        croppedCustomerName, state.isTemporalCustomerSummaryVisible)
    _inputProductOrder.setTemporalCustomerBalance(
        state.temporalCustomer?.balance, state.isTemporalCustomerSummaryVisible)
    _inputProductOrder.setTemporalCustomerDebtTitle(
        croppedCustomerName, state.isTemporalCustomerSummaryVisible)
    _inputProductOrder.setTemporalCustomerDebt(
        state.temporalCustomer?.debt,
        state.customerDebtColorRes,
        state.isTemporalCustomerSummaryVisible)

    val inputtedQueue: QueueModel = createQueueViewModel.parseInputtedQueue()
    _inputDate.setInputtedDate(state.date, state.dateFormat())
    _inputStatus.setInputtedStatus(state.status)
    _inputPaymentMethod.setInputtedPaymentMethod(state.paymentMethod)
    _inputPaymentMethod.setEnabledButtons(state.allowedPaymentMethods)
    _inputPaymentMethod.setVisible(state.isPaymentMethodVisible)
    _inputProductOrder.setInputtedProductOrders(state.productOrders)
    _inputProductOrder.setTotalDiscount(inputtedQueue.totalDiscount())
    _inputProductOrder.setGrandTotalPrice(inputtedQueue.grandTotalPrice())
  }

  private fun _onMakeProductOrderState(state: MakeProductOrderState) {
    if (state.isDialogShown) {
      if (state.productOrderToEdit != null) {
        _inputProductOrder.makeProductOrder.openEditDialog()
      } else {
        _inputProductOrder.makeProductOrder.openCreateDialog(state.isAddButtonEnabled)
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
    fragmentBinding.customerLayout.isEnabled = !state.isContextualModeActive
    fragmentBinding.customer.isEnabled = !state.isContextualModeActive
    fragmentBinding.date.isEnabled = !state.isContextualModeActive
    fragmentBinding.status.isEnabled = !state.isContextualModeActive
    fragmentBinding.productOrder.addButton.isEnabled = !state.isContextualModeActive
    _inputPaymentMethod.setEnabledButtons(
        if (!state.isContextualModeActive) {
          // Only enable the allowed buttons, preventing all buttons from being enabled
          // when the contextual mode is switched.
          createQueueViewModel.uiState.safeValue.allowedPaymentMethods
        } else {
          setOf()
        })
  }

  private fun _onSelectCustomerResult(requestKey: String, result: Bundle) {
    when (SelectCustomerFragment.Request.entries.find { it.key == requestKey }) {
      SelectCustomerFragment.Request.SELECT_CUSTOMER -> {
        val customerId: Long =
            result.getLong(SelectCustomerFragment.Result.SELECTED_CUSTOMER_ID_LONG.key)
        if (customerId != 0L) {
          createQueueViewModel
              .selectCustomerById(customerId)
              .observe(viewLifecycleOwner, createQueueViewModel::onCustomerChanged)
        } else {
          createQueueViewModel.onCustomerChanged(createQueueViewModel.uiState.safeValue.customer)
        }
      }
      null -> Unit
    }
  }

  private fun _onSelectProductResult(requestKey: String, result: Bundle) {
    when (SelectProductFragment.Request.entries.find { it.key == requestKey }) {
      SelectProductFragment.Request.SELECT_PRODUCT -> {
        val productId: Long =
            result.getLong(SelectProductFragment.Result.SELECTED_PRODUCT_ID_LONG.key)
        val onProductChanged: (ProductModel?) -> Unit = {
          // The dialog should be opened first to avoid overwriting the product.
          createQueueViewModel.makeProductOrderView.onDialogShown(
              createQueueViewModel.makeProductOrderView.uiState.safeValue.productOrderToEdit)
          createQueueViewModel.makeProductOrderView.onProductChanged(it)
        }
        if (productId != 0L) {
          createQueueViewModel
              .selectProductById(productId)
              .observe(viewLifecycleOwner, onProductChanged)
        } else {
          onProductChanged(createQueueViewModel.makeProductOrderView.uiState.safeValue.product)
        }
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
