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

package com.robifr.ledger.ui.editproduct

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.robifr.ledger.R
import com.robifr.ledger.ui.FragmentResultKey
import com.robifr.ledger.ui.createproduct.CreateProductFragment
import com.robifr.ledger.ui.editproduct.viewmodel.EditProductResultState
import com.robifr.ledger.ui.editproduct.viewmodel.EditProductViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProductFragment : CreateProductFragment() {
  override val createProductViewModel: EditProductViewModel by viewModels()

  override fun onViewCreated(view: View, savedInstance: Bundle?) {
    super.onViewCreated(view, savedInstance)
    fragmentBinding.toolbar.setTitle(R.string.editProduct)
    createProductViewModel.editResultState.observe(viewLifecycleOwner) {
      it.handleIfNotHandled(::_onResultState)
    }
  }

  private fun _onResultState(state: EditProductResultState) {
    state.editedProductId?.let {
      parentFragmentManager.setFragmentResult(
          Request.EDIT_PRODUCT.key,
          Bundle().apply { putLong(Result.EDITED_PRODUCT_ID_LONG.key, it) })
    }
    finish()
  }

  enum class Arguments : FragmentResultKey {
    INITIAL_PRODUCT_ID_TO_EDIT_LONG
  }

  enum class Request : FragmentResultKey {
    EDIT_PRODUCT
  }

  enum class Result : FragmentResultKey {
    EDITED_PRODUCT_ID_LONG
  }
}
