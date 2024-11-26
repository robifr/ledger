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

import android.util.TypedValue
import androidx.core.text.HtmlCompat
import com.robifr.ledger.R
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.databinding.ListableListTextBinding
import com.robifr.ledger.ui.RecyclerViewHolder

class ProductHeaderHolder(
    private val _textBinding: ListableListTextBinding,
    private val _products: () -> List<ProductModel>
) : RecyclerViewHolder(_textBinding.root) {
  init {
    _textBinding.text.setTextSize(
        TypedValue.COMPLEX_UNIT_PX, itemView.context.resources.getDimension(R.dimen.text_small))
  }

  override fun bind(itemIndex: Int) {
    _textBinding.text.text =
        HtmlCompat.fromHtml(
            itemView.context.resources.getQuantityString(
                R.plurals.product_displaying_n_product, _products().size, _products().size),
            HtmlCompat.FROM_HTML_MODE_LEGACY)
  }
}