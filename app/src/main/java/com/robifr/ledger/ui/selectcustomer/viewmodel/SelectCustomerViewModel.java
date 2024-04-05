/**
 * Copyright (c) 2024 Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package com.robifr.ledger.ui.selectcustomer.viewmodel;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.robifr.ledger.R;
import com.robifr.ledger.data.CustomerSortMethod;
import com.robifr.ledger.data.CustomerSorter;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.repository.CustomerRepository;
import com.robifr.ledger.ui.LiveDataEvent;
import com.robifr.ledger.ui.LiveDataModelUpdater;
import com.robifr.ledger.ui.StringResources;
import com.robifr.ledger.ui.selectcustomer.SelectCustomerFragment;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

@HiltViewModel
public class SelectCustomerViewModel extends ViewModel {
  @NonNull private final CustomerRepository _customerRepository;
  @NonNull private final CustomersUpdater _customersUpdater;
  @NonNull private final CustomerSorter _sorter = new CustomerSorter();

  @Nullable private final CustomerModel _initialSelectedCustomer;

  @NonNull
  private final MutableLiveData<LiveDataEvent<StringResources>> _snackbarMessage =
      new MutableLiveData<>();

  @NonNull
  private final MutableLiveData<LiveDataEvent<Long>> _selectedCustomerId = new MutableLiveData<>();

  @NonNull private final MutableLiveData<List<CustomerModel>> _customers = new MutableLiveData<>();

  @Inject
  public SelectCustomerViewModel(
      @NonNull CustomerRepository customerRepository, @NonNull SavedStateHandle savedStateHandle) {
    Objects.requireNonNull(savedStateHandle);

    this._customerRepository = Objects.requireNonNull(customerRepository);
    this._customersUpdater = new CustomersUpdater(this._customers);
    this._initialSelectedCustomer =
        savedStateHandle.get(SelectCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER.key());

    this._sorter.setSortMethod(new CustomerSortMethod(CustomerSortMethod.SortBy.NAME, true));
    this._customerRepository.addModelChangedListener(this._customersUpdater);
  }

  @Override
  public void onCleared() {
    this._customerRepository.removeModelChangedListener(this._customersUpdater);
  }

  @Nullable
  public CustomerModel initialSelectedCustomer() {
    return this._initialSelectedCustomer;
  }

  @NonNull
  public LiveData<LiveDataEvent<StringResources>> snackbarMessage() {
    return this._snackbarMessage;
  }

  @NonNull
  public LiveData<LiveDataEvent<Long>> selectedCustomerId() {
    return this._selectedCustomerId;
  }

  @NonNull
  public LiveData<List<CustomerModel>> customers() {
    return this._customers;
  }

  public void onCustomersChanged(@NonNull List<CustomerModel> customers) {
    Objects.requireNonNull(customers);

    this._customers.setValue(Collections.unmodifiableList(this._sorter.sort(customers)));
  }

  public void onCustomerSelected(@Nullable CustomerModel customer) {
    final Long customerId = customer != null && customer.id() != null ? customer.id() : null;
    this._selectedCustomerId.setValue(new LiveDataEvent<>(customerId));
  }

  @NonNull
  public LiveData<List<CustomerModel>> selectAllCustomers() {
    final MutableLiveData<List<CustomerModel>> result = new MutableLiveData<>();

    this._customerRepository
        .selectAll()
        .thenAcceptAsync(
            customers -> {
              if (customers == null) {
                this._snackbarMessage.postValue(
                    new LiveDataEvent<>(
                        new StringResources.Strings(
                            R.string.text_error_unable_to_retrieve_all_customers)));
              }

              result.postValue(customers);
            });
    return result;
  }

  private class CustomersUpdater extends LiveDataModelUpdater<CustomerModel> {
    public CustomersUpdater(@NonNull MutableLiveData<List<CustomerModel>> customers) {
      super(customers);
    }

    @Override
    @MainThread
    public void onUpdateLiveData(@NonNull List<CustomerModel> customers) {
      SelectCustomerViewModel.this.onCustomersChanged(customers);
    }
  }
}
