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

package io.github.robifr.ledger.ui.product.filter

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.robifr.ledger.R
import io.github.robifr.ledger.databinding.ProductDialogFilterBinding
import io.github.robifr.ledger.ui.product.ProductFragment

class ProductFilter(private val _fragment: ProductFragment) {
  private val _dialogBinding: ProductDialogFilterBinding =
      ProductDialogFilterBinding.inflate(_fragment.layoutInflater)
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_fragment.requireContext(), R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
        setOnDismissListener { _fragment.productViewModel.filterView.onDialogClosed() }
      }
  val filterPrice: ProductFilterPrice = ProductFilterPrice(_fragment, _dialogBinding)

  init {
    _fragment.fragmentBinding.filtersChip.setText(R.string.product_filters)
    _fragment.fragmentBinding.filtersChip.setOnClickListener {
      _fragment.productViewModel.filterView.onDialogShown()
    }
  }

  fun showDialog() {
    _dialog.show()
  }

  fun dismissDialog() {
    _dialog.dismiss()
    _dialog.currentFocus?.clearFocus()
  }
}
