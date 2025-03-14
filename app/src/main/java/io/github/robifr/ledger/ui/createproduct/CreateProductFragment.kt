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

package io.github.robifr.ledger.ui.createproduct

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.robifr.ledger.R
import io.github.robifr.ledger.databinding.CreateProductFragmentBinding
import io.github.robifr.ledger.ui.common.navigation.FragmentResultKey
import io.github.robifr.ledger.ui.common.navigation.OnBackPressedHandler
import io.github.robifr.ledger.ui.common.state.SnackbarState
import io.github.robifr.ledger.ui.createproduct.viewmodel.CreateProductEvent
import io.github.robifr.ledger.ui.createproduct.viewmodel.CreateProductResultState
import io.github.robifr.ledger.ui.createproduct.viewmodel.CreateProductState
import io.github.robifr.ledger.ui.createproduct.viewmodel.CreateProductViewModel
import io.github.robifr.ledger.util.hideKeyboard

@AndroidEntryPoint
open class CreateProductFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  protected var _fragmentBinding: CreateProductFragmentBinding? = null
  val fragmentBinding: CreateProductFragmentBinding
    get() = _fragmentBinding!!

  open val createProductViewModel: CreateProductViewModel by viewModels()
  private lateinit var _inputName: CreateProductName
  private lateinit var _inputPrice: CreateProductPrice

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = CreateProductFragmentBinding.inflate(inflater, container, false)
    _inputName = CreateProductName(this)
    _inputPrice = CreateProductPrice(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { view, insets ->
      val systemBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      val windowInsets: Insets =
          insets.getInsets(
              WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars())
      view.updatePadding(
          top = systemBarInsets.top, left = windowInsets.left, right = windowInsets.right)
      WindowInsetsCompat.CONSUMED
    }
    requireActivity()
        .onBackPressedDispatcher
        .addCallback(
            viewLifecycleOwner, OnBackPressedHandler { createProductViewModel.onBackPressed() })
    fragmentBinding.toolbar.setNavigationOnClickListener { createProductViewModel.onBackPressed() }
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_edit)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    createProductViewModel.uiEvent.observe(viewLifecycleOwner, ::_onUiEvent)
    createProductViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
  }

  override fun onMenuItemClick(item: MenuItem?): Boolean =
      when (item?.itemId) {
        R.id.save -> {
          createProductViewModel.onSave()
          true
        }
        else -> false
      }

  fun finish() {
    requireView().hideKeyboard()
    findNavController().popBackStack()
  }

  private fun _onUiEvent(event: CreateProductEvent) {
    event.snackbar?.let {
      _onSnackbarState(it.data)
      it.onConsumed()
    }
    event.isFragmentFinished?.let {
      finish()
      it.onConsumed()
    }
    event.isUnsavedChangesDialogShown?.let {
      _onUnsavedChangesDialogState(onDismiss = { it.onConsumed() })
    }
    event.createResult?.let {
      _onResultState(it.data)
      it.onConsumed()
    }
  }

  private fun _onResultState(state: CreateProductResultState) {
    parentFragmentManager.setFragmentResult(
        Request.CREATE_PRODUCT.key(),
        Bundle().apply {
          state.createdProductId?.let { putLong(Result.CREATED_PRODUCT_ID_LONG.key(), it) }
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

  private fun _onUnsavedChangesDialogState(onDismiss: () -> Unit) {
    MaterialAlertDialogBuilder(requireContext())
        .setMessage(R.string.createProduct_unsavedChangesWarning)
        .setNegativeButton(R.string.action_cancel) { _, _ -> }
        .setPositiveButton(R.string.action_leaveWithoutSaving) { _, _ -> finish() }
        .setOnDismissListener { onDismiss() }
        .show()
  }

  private fun _onUiState(state: CreateProductState) {
    _inputName.setInputtedNameText(
        state.name, state.nameErrorMessageRes?.toStringValue(requireContext()))
    _inputPrice.setInputtedPriceText(state.formattedPrice)
  }

  enum class Request : FragmentResultKey {
    CREATE_PRODUCT
  }

  enum class Result : FragmentResultKey {
    CREATED_PRODUCT_ID_LONG
  }
}
