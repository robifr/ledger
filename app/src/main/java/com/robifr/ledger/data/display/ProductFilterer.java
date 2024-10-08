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

package com.robifr.ledger.data.display;

import androidx.annotation.NonNull;
import com.robifr.ledger.data.model.ProductModel;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProductFilterer {
  @NonNull private ProductFilters _filters = ProductFilters.toBuilder().build();

  @NonNull
  public ProductFilters filters() {
    return this._filters;
  }

  public void setFilters(@NonNull ProductFilters filters) {
    this._filters = Objects.requireNonNull(filters);
  }

  @NonNull
  public List<ProductModel> filter(@NonNull List<ProductModel> products) {
    Objects.requireNonNull(products);

    return products.stream()
        .filter(product -> !this._shouldFilteredOutByPrice(product))
        .collect(Collectors.toList());
  }

  private boolean _shouldFilteredOutByPrice(@NonNull ProductModel product) {
    Objects.requireNonNull(product);

    final Long first = this._filters.filteredPrice().first;
    final Long second = this._filters.filteredPrice().second;

    return (first != null && product.price() < first)
        || (second != null && product.price() > second);
  }
}
