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

package com.robifr.ledger.ui.selectcustomer;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.ui.searchcustomer.SearchCustomerFragment;
import com.robifr.ledger.ui.selectcustomer.viewmodel.SelectCustomerViewModel;
import java.util.Arrays;
import java.util.Objects;

public class SelectCustomerResultHandler {
  @NonNull private final SelectCustomerFragment _fragment;

  public SelectCustomerResultHandler(@NonNull SelectCustomerFragment fragment) {
    this._fragment = Objects.requireNonNull(fragment);

    this._fragment
        .getParentFragmentManager()
        .setFragmentResultListener(
            SearchCustomerFragment.Request.SELECT_CUSTOMER.key(),
            this._fragment.getViewLifecycleOwner(),
            this::_onSearchCustomerResult);
  }

  private void _onSearchCustomerResult(@NonNull String requestKey, @NonNull Bundle result) {
    Objects.requireNonNull(requestKey);
    Objects.requireNonNull(result);

    final SearchCustomerFragment.Request request =
        Arrays.stream(SearchCustomerFragment.Request.values())
            .filter(e -> e.key().equals(requestKey))
            .findFirst()
            .orElse(null);
    if (request == null) return;

    switch (request) {
      case SELECT_CUSTOMER -> {
        final SelectCustomerViewModel viewModel =
            SelectCustomerResultHandler.this._fragment.selectCustomerViewModel();
        final Long customerId =
            result.getLong(SearchCustomerFragment.Result.SELECTED_CUSTOMER_ID_LONG.key());
        final CustomerModel selectedCustomer =
            !customerId.equals(0L)
                ? viewModel.customers().getValue().stream()
                    .filter(customer -> customer.id() != null && customer.id().equals(customerId))
                    .findFirst()
                    .orElse(null)
                : null;

        if (selectedCustomer != null) viewModel.onCustomerSelected(selectedCustomer);
      }
    }
  }
}
