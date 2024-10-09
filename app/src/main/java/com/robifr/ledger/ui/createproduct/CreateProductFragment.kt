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

package com.robifr.ledger.ui.createproduct

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
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.robifr.ledger.R
import com.robifr.ledger.databinding.CreateProductFragmentBinding
import com.robifr.ledger.ui.FragmentResultKey
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductResultState
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductState
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductViewModel
import com.robifr.ledger.util.Compats
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class CreateProductFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  protected var _fragmentBinding: CreateProductFragmentBinding? = null
  val fragmentBinding: CreateProductFragmentBinding
    get() = _fragmentBinding!!

  protected open val _createProductViewModel: CreateProductViewModel by viewModels()
  val createProductViewModel: CreateProductViewModel
    get() = _createProductViewModel

  private lateinit var _inputName: CreateProductName
  private lateinit var _inputPrice: CreateProductPrice
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstance: Bundle?
  ): View? {
    this._fragmentBinding = CreateProductFragmentBinding.inflate(inflater, container, false)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstance: Bundle?) {
    this._inputName = CreateProductName(this)
    this._inputPrice = CreateProductPrice(this)
    this._onBackPressed = OnBackPressedHandler(this)
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_edit)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    with(_createProductViewModel) {
      resultState.observe(viewLifecycleOwner) { it.handleIfNotHandled(::_onResultState) }
      snackbarState.observe(viewLifecycleOwner) { it.handleIfNotHandled(::_onErrorState) }
      uiState.observe(viewLifecycleOwner, ::_onUiState)
    }
  }

  override fun onMenuItemClick(item: MenuItem?): Boolean =
      when (item?.itemId) {
        R.id.save -> {
          _createProductViewModel.onSave()
          true
        }
        else -> false
      }

  fun finish() {
    Compats.hideKeyboard(requireContext(), requireView().findFocus())
    Navigation.findNavController(fragmentBinding.root).popBackStack()
  }

  private fun _onResultState(state: CreateProductResultState) {
    state.createdProductId?.let { id ->
      parentFragmentManager.setFragmentResult(
          Request.CREATE_PRODUCT.key(),
          Bundle().apply { putLong(Result.CREATED_PRODUCT_ID_LONG.key(), id) })
    }
    finish()
  }

  private fun _onErrorState(state: SnackbarState) {
    Snackbar.make(
            (fragmentBinding.root.parent as View),
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onUiState(state: CreateProductState) {
    _inputName.setInputtedNameText(state.name)
    _inputName.setError(state.nameErrorMessageRes?.toStringValue(requireContext()))
    _inputPrice.setInputtedPriceText(state.formattedPrice)
  }

  enum class Request : FragmentResultKey {
    CREATE_PRODUCT
  }

  enum class Result : FragmentResultKey {
    CREATED_PRODUCT_ID_LONG
  }
}

private class OnBackPressedHandler(private val _fragment: CreateProductFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    MaterialAlertDialogBuilder(_fragment.requireContext())
        .setMessage(R.string.createProduct_unsavedChangesWarning)
        .setNegativeButton(R.string.action_discardAndLeave) { _, _ -> _fragment.finish() }
        .setPositiveButton(R.string.action_cancel) { dialog: DialogInterface?, _ ->
          dialog?.dismiss()
        }
        .show()
  }
}
