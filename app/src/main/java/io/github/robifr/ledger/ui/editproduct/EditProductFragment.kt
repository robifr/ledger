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

package io.github.robifr.ledger.ui.editproduct

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.robifr.ledger.R
import io.github.robifr.ledger.ui.common.navigation.FragmentResultKey
import io.github.robifr.ledger.ui.common.state.UiEvent
import io.github.robifr.ledger.ui.createproduct.CreateProductFragment
import io.github.robifr.ledger.ui.editproduct.viewmodel.EditProductResultState
import io.github.robifr.ledger.ui.editproduct.viewmodel.EditProductViewModel

@AndroidEntryPoint
class EditProductFragment : CreateProductFragment() {
  override val createProductViewModel: EditProductViewModel by viewModels()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    fragmentBinding.toolbar.setTitle(R.string.editProduct)
    createProductViewModel.editResultEvent.observe(viewLifecycleOwner) {
        event: UiEvent<EditProductResultState>? ->
      event?.let {
        _onResultState(it.data)
        it.onConsumed()
      }
    }
  }

  private fun _onResultState(state: EditProductResultState) {
    parentFragmentManager.setFragmentResult(
        Request.EDIT_PRODUCT.key(),
        Bundle().apply {
          state.editedProductId?.let { putLong(Result.EDITED_PRODUCT_ID_LONG.key(), it) }
        })
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
