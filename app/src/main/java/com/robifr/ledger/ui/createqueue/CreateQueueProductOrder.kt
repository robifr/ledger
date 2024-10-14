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

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ActionMode
import com.google.android.material.card.MaterialCardView
import com.robifr.ledger.R
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.databinding.ProductOrderCardBinding
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.util.TreeSet

class CreateQueueProductOrder(private val _fragment: CreateQueueFragment) {
  val makeProductOrder: CreateQueueMakeProductOrder = CreateQueueMakeProductOrder(_fragment)
  private var _contextualMode: ActionMode? = null
  @ColorInt
  private val _normalStatusBarColor: Int = _fragment.requireActivity().window.statusBarColor

  init {
    _fragment.fragmentBinding.productOrder.addButton.setOnClickListener {
      _fragment.createQueueViewModel.makeProductOrderView.onShowDialog()
    }
  }

  fun setSelectedProductOrderByIndexes(selectedIndexes: Set<Int>) {
    _contextualMode?.setTitle(selectedIndexes.size.toString())

    val indexes: TreeSet<Int> = TreeSet(selectedIndexes)
    for (i in _fragment.fragmentBinding.productOrder.listLayout.childCount - 1 downTo 0) {
      val shouldCheck: Boolean = indexes.isNotEmpty() && indexes.last() == i
      with(
          ProductOrderCardBinding.bind(
              _fragment.fragmentBinding.productOrder.listLayout.getChildAt(i))) {
            cardView.setChecked(shouldCheck)
            checkbox.setChecked(shouldCheck)
            checkbox.setVisibility(if (shouldCheck) View.VISIBLE else View.GONE)
            productImage.text.setVisibility(if (shouldCheck) View.GONE else View.VISIBLE)
          }
      if (shouldCheck) indexes.pollLast()
    }
  }

  fun setContextualMode(isActive: Boolean) {
    // The null check on contextual mode meant to prevent them from restarting.
    if (isActive && _fragment.requireActivity() is AppCompatActivity && _contextualMode == null) {
      // Match status bar color with the contextual toolbar background color.
      _fragment
          .requireActivity()
          .window
          .setStatusBarColor(_fragment.requireContext().getColor(R.color.surface))
      _contextualMode =
          (_fragment.requireActivity() as AppCompatActivity).startSupportActionMode(
              SelectProductOrderActionModeCallback(_fragment))
    } else if (!isActive && _contextualMode != null) {
      // Re-apply original status bar color.
      _fragment.requireActivity().window.setStatusBarColor(_normalStatusBarColor)
      _contextualMode?.finish()
      _contextualMode = null
    }
  }

  fun setGrandTotalPrice(grandTotalPrice: BigDecimal) {
    _fragment.fragmentBinding.productOrder.grandTotalPrice.setText(
        CurrencyFormat.format(
            grandTotalPrice, AppCompatDelegate.getApplicationLocales().toLanguageTags()))
  }

  fun setTotalDiscount(totalDiscount: BigDecimal) {
    _fragment.fragmentBinding.productOrder.totalDiscount.setText(
        CurrencyFormat.format(
            totalDiscount, AppCompatDelegate.getApplicationLocales().toLanguageTags()))
  }

  fun setCustomerBalanceAfterPaymentTitle(customerName: String?, isVisible: Boolean) {
    _fragment.fragmentBinding.productOrder.customerBalanceTitle.setText(
        _fragment.getString(R.string.createQueue_productOrders_x_balance, customerName))
    _fragment.fragmentBinding.productOrder.customerBalanceTitle.setVisibility(
        if (isVisible) View.VISIBLE else View.GONE)
  }

  fun setCustomerBalanceAfterPayment(balance: Long?, isVisible: Boolean) {
    _fragment.fragmentBinding.productOrder.customerBalance.setText(
        balance?.let {
          CurrencyFormat.format(
              it.toBigDecimal(), AppCompatDelegate.getApplicationLocales().toLanguageTags())
        })
    _fragment.fragmentBinding.productOrder.customerBalance.setVisibility(
        if (isVisible) View.VISIBLE else View.GONE)
  }

  fun setCustomerDebtAfterPaymentTitle(customerName: String?, isVisible: Boolean) {
    _fragment.fragmentBinding.productOrder.customerDebtTitle.setText(
        _fragment.getString(R.string.createQueue_productOrders_x_debt, customerName))
    _fragment.fragmentBinding.productOrder.customerDebtTitle.setVisibility(
        if (isVisible) View.VISIBLE else View.GONE)
  }

  fun setCustomerDebtAfterPayment(debt: BigDecimal?, @ColorRes textColor: Int, isVisible: Boolean) {
    _fragment.fragmentBinding.productOrder.customerDebt.setText(
        debt?.let {
          CurrencyFormat.format(it, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        })
    _fragment.fragmentBinding.productOrder.customerDebt.setTextColor(
        _fragment.requireContext().getColor(textColor))
    _fragment.fragmentBinding.productOrder.customerDebt.setVisibility(
        if (isVisible) View.VISIBLE else View.GONE)
  }

  fun setInputtedProductOrders(productOrders: List<ProductOrderModel>) {
    val listLayout: LinearLayout =
        _fragment.fragmentBinding.productOrder.listLayout.apply {
          if (childCount > productOrders.size) {
            // Remove all the extra views.
            removeViews(productOrders.size, childCount - productOrders.size)
          }
        }
    for ((i, productOrder) in productOrders.withIndex()) {
      val cardBinding: ProductOrderCardBinding =
          if (listLayout.getChildAt(i) is MaterialCardView) {
                // Reuse already inflated view to reduce overhead.
                ProductOrderCardBinding.bind(listLayout.getChildAt(i))
              } else {
                ProductOrderCardBinding.inflate(_fragment.layoutInflater, listLayout, false)
              }
              .apply {
                checkbox.setChecked(false)
                checkbox.setVisibility(View.GONE)
                productImage.text.setVisibility(View.VISIBLE)
                cardView.setOnClickListener {
                  val cardIndex: Int = listLayout.indexOfChild(cardView)
                  if (_fragment.createQueueViewModel.selectProductOrderView.uiState.safeValue
                      .isContextualModeActive) {
                    _fragment.createQueueViewModel.selectProductOrderView
                        .onProductOrderCheckedChanged(cardIndex, !checkbox.isChecked)
                  } else {
                    _fragment.createQueueViewModel.makeProductOrderView.onShowDialog(
                        _fragment.createQueueViewModel.uiState.safeValue.productOrders[cardIndex])
                  }
                }
                cardView.setOnLongClickListener {
                  _fragment.createQueueViewModel.selectProductOrderView
                      .onProductOrderCheckedChanged(
                          listLayout.indexOfChild(cardView), !checkbox.isChecked)
                  true
                }
              }
      ProductOrderCardComponent(_fragment.requireContext(), cardBinding)
          .setProductOrder(productOrder)
      // Add to layout until the amount of child view match the product orders size.
      if (listLayout.childCount <= i) listLayout.addView(cardBinding.root)
    }
  }
}

private class SelectProductOrderActionModeCallback(private val _fragment: CreateQueueFragment) :
    ActionMode.Callback {

  override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
    mode?.menuInflater?.inflate(R.menu.createqueue_contextualtoolbar, menu)
    return true
  }

  override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

  override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean =
      when (item?.itemId) {
        R.id.createqueue_contextualtoolbar_delete_item -> {
          _fragment.createQueueViewModel.selectProductOrderView.onDeleteSelectedProductOrder()
          true
        }
        else -> false
      }

  override fun onDestroyActionMode(mode: ActionMode?) {
    _fragment.createQueueViewModel.selectProductOrderView.onDisableContextualMode()
  }
}
