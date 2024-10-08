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

package com.robifr.ledger.ui.customer.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.robifr.ledger.R;
import com.robifr.ledger.data.display.CustomerFilterer;
import com.robifr.ledger.data.display.CustomerSortMethod;
import com.robifr.ledger.data.display.CustomerSorter;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.repository.CustomerRepository;
import com.robifr.ledger.ui.StringResources;
import com.robifr.ledger.util.livedata.SafeEvent;
import com.robifr.ledger.util.livedata.SafeLiveData;
import com.robifr.ledger.util.livedata.SafeMutableLiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

@HiltViewModel
public class CustomerViewModel extends ViewModel {
  @NonNull private final CustomerRepository _customerRepository;

  @NonNull
  private final CustomerChangedListener _customerChangedListener =
      new CustomerChangedListener(this);

  @NonNull private final CustomerFilterViewModel _filterView;
  @NonNull private final CustomerSorter _sorter = new CustomerSorter();

  @NonNull
  private final MutableLiveData<SafeEvent<StringResources>> _snackbarMessage =
      new MutableLiveData<>();

  @NonNull
  private final SafeMutableLiveData<List<CustomerModel>> _customers =
      new SafeMutableLiveData<>(List.of());

  @NonNull
  private final SafeMutableLiveData<CustomerSortMethod> _sortMethod =
      new SafeMutableLiveData<>(new CustomerSortMethod(CustomerSortMethod.SortBy.NAME, true));

  /**
   * Currently expanded customer index from {@link #_customers}. -1 to represent none being
   * expanded.
   */
  @NonNull
  private final SafeMutableLiveData<Integer> _expandedCustomerIndex = new SafeMutableLiveData<>(-1);

  @Inject
  public CustomerViewModel(@NonNull CustomerRepository customerRepository) {
    this._customerRepository = Objects.requireNonNull(customerRepository);
    this._filterView = new CustomerFilterViewModel(this, new CustomerFilterer());

    this._customerRepository.addModelChangedListener(this._customerChangedListener);

    // Setting up initial values inside a fragment is painful. See commit d5604599.
    SafeEvent.observeOnce(
        this.selectAllCustomers(),
        customers ->
            this._filterView.onFiltersChanged(this._filterView.inputtedFilters(), customers),
        Objects::nonNull);
  }

  @Override
  public void onCleared() {
    this._customerRepository.removeModelChangedListener(this._customerChangedListener);
  }

  @NonNull
  public CustomerFilterViewModel filterView() {
    return this._filterView;
  }

  @NonNull
  public LiveData<SafeEvent<StringResources>> snackbarMessage() {
    return this._snackbarMessage;
  }

  @NonNull
  public SafeLiveData<List<CustomerModel>> customers() {
    return this._customers;
  }

  @NonNull
  public SafeLiveData<CustomerSortMethod> sortMethod() {
    return this._sortMethod;
  }

  /**
   * @see #_expandedCustomerIndex
   */
  public SafeLiveData<Integer> expandedCustomerIndex() {
    return this._expandedCustomerIndex;
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
                    new SafeEvent<>(
                        new StringResources.Strings(R.string.customer_fetchAllCustomerError)));
              }

              result.postValue(customers);
            });
    return result;
  }

  public void onDeleteCustomer(@NonNull CustomerModel customer) {
    Objects.requireNonNull(customer);

    this._customerRepository
        .delete(customer)
        .thenAcceptAsync(
            effected -> {
              final StringResources stringRes =
                  effected > 0
                      ? new StringResources.Plurals(
                          R.plurals.customer_deleted_n_customer, effected, effected)
                      : new StringResources.Strings(R.string.customer_deleteCustomerError);
              this._snackbarMessage.postValue(new SafeEvent<>(stringRes));
            });
  }

  public void onCustomersChanged(@NonNull List<CustomerModel> customers) {
    Objects.requireNonNull(customers);

    this._customers.setValue(Collections.unmodifiableList(customers));
  }

  public void onSortMethodChanged(@NonNull CustomerSortMethod sortMethod) {
    this.onSortMethodChanged(sortMethod, this._customers.getValue());
  }

  public void onSortMethodChanged(
      @NonNull CustomerSortMethod sortMethod, @NonNull List<CustomerModel> customers) {
    Objects.requireNonNull(sortMethod);
    Objects.requireNonNull(customers);

    this._sortMethod.setValue(sortMethod);
    this._sorter.setSortMethod(sortMethod);
    this.onCustomersChanged(this._sorter.sort(customers));
  }

  /**
   * @see #onSortMethodChanged(CustomerSortMethod.SortBy, List)
   */
  public void onSortMethodChanged(@NonNull CustomerSortMethod.SortBy sortBy) {
    this.onSortMethodChanged(sortBy, this._customers.getValue());
  }

  /**
   * Sort {@link #_customers} based on specified {@link CustomerSortMethod.SortBy} type. Doing so
   * will reverse the order — Ascending becomes descending and vice versa. Use {@link
   * #onSortMethodChanged(CustomerSortMethod)} if you want to apply the order by yourself.
   */
  public void onSortMethodChanged(
      @NonNull CustomerSortMethod.SortBy sortBy, @NonNull List<CustomerModel> customers) {
    Objects.requireNonNull(sortBy);
    Objects.requireNonNull(customers);

    // Reverse sort order when selecting same sort option.
    final boolean isAscending =
        this._sortMethod.getValue().sortBy() == sortBy
            ? !this._sortMethod.getValue().isAscending()
            : this._sortMethod.getValue().isAscending();

    this.onSortMethodChanged(new CustomerSortMethod(sortBy, isAscending), customers);
  }

  public void onExpandedCustomerIndexChanged(int index) {
    this._expandedCustomerIndex.setValue(index);
  }
}
