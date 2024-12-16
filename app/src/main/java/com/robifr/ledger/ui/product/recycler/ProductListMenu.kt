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

package com.robifr.ledger.ui.product.recycler

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.text.HtmlCompat
import androidx.navigation.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.robifr.ledger.R
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.databinding.ProductCardDialogMenuBinding
import com.robifr.ledger.ui.editproduct.EditProductFragment

class ProductListMenu(
    holder: ProductListHolder,
    products: () -> List<ProductModel>,
    onDeleteProduct: (ProductModel) -> Unit,
    productIndex: () -> Int
) {
  private val _dialogBinding: ProductCardDialogMenuBinding =
      ProductCardDialogMenuBinding.inflate(LayoutInflater.from(holder.itemView.context)).apply {
        editButton.setOnClickListener {
          val productId: Long = products()[productIndex()].id ?: return@setOnClickListener
          holder.itemView
              .findNavController()
              .navigate(
                  R.id.editProductFragment,
                  Bundle().apply {
                    putLong(
                        EditProductFragment.Arguments.INITIAL_PRODUCT_ID_TO_EDIT_LONG.key(),
                        productId)
                  })
          _dialog.dismiss()
        }
        deleteButton.setOnClickListener {
          MaterialAlertDialogBuilder(holder.itemView.context)
              .setTitle(
                  HtmlCompat.fromHtml(
                      holder.itemView.context.getString(R.string.product_cardMenu_deleteWarning),
                      HtmlCompat.FROM_HTML_MODE_LEGACY))
              .setMessage(
                  HtmlCompat.fromHtml(
                      holder.itemView.context.getString(
                          R.string.product_cardMenu_deleteWarning_description,
                          products()[productIndex()].name),
                      HtmlCompat.FROM_HTML_MODE_LEGACY))
              .setNegativeButton(R.string.action_delete) { _, _ ->
                onDeleteProduct(products()[productIndex()])
                _dialog.dismiss()
              }
              .setPositiveButton(R.string.action_cancel) { _, _ -> }
              .show()
        }
      }
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(holder.itemView.context, R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
      }

  fun openDialog() {
    _dialog.show()
  }
}
