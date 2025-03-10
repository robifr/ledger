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

package io.github.robifr.ledger.ui.search.viewmodel

import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.ProductModel

data class SearchState(
    val customers: List<CustomerModel>,
    val products: List<ProductModel>,
    val query: String = ""
) {
  val isTabLayoutVisible: Boolean
    get() = query.isNotEmpty() && (customers.isNotEmpty() || products.isNotEmpty())

  val isNoResultFoundIllustrationVisible: Boolean
    get() = query.isNotEmpty() && customers.isEmpty() && products.isEmpty()

  val isViewPagerVisible: Boolean
    get() = query.isNotEmpty() && (customers.isNotEmpty() || products.isNotEmpty())

  val shouldSearchCustomerFragmentLoaded: Boolean
    get() = query.isEmpty() || customers.isNotEmpty()

  val shouldSearchProductFragmentLoaded: Boolean
    get() = query.isEmpty() || products.isNotEmpty()
}
