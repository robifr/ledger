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

package com.robifr.ledger.ui.queue.viewmodel;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.robifr.ledger.data.display.QueueDate;
import com.robifr.ledger.data.display.QueueFilterer;
import com.robifr.ledger.data.display.QueueFilters;
import com.robifr.ledger.data.display.QueueSortMethod;
import com.robifr.ledger.data.model.QueueModel;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class QueueFilterViewModel {
  @NonNull private final QueueViewModel _viewModel;
  @NonNull private final QueueFilterer _filterer;

  @NonNull private final MutableLiveData<List<Long>> _inputtedCustomerIds = new MutableLiveData<>();

  @NonNull
  private final MutableLiveData<Boolean> _inputtedIsNullCustomerShown = new MutableLiveData<>();

  @NonNull
  private final MutableLiveData<Set<QueueModel.Status>> _inputtedStatus = new MutableLiveData<>();

  @NonNull
  private final MutableLiveData<String> _inputtedMinTotalPriceText = new MutableLiveData<>();

  @NonNull
  private final MutableLiveData<String> _inputtedMaxTotalPriceText = new MutableLiveData<>();

  @NonNull private final MutableLiveData<QueueDate> _inputtedDate = new MutableLiveData<>();

  public QueueFilterViewModel(@NonNull QueueViewModel viewModel, @NonNull QueueFilterer filterer) {
    this._viewModel = Objects.requireNonNull(viewModel);
    this._filterer = Objects.requireNonNull(filterer);
  }

  @NonNull
  public LiveData<List<Long>> inputtedCustomerIds() {
    return this._inputtedCustomerIds;
  }

  @NonNull
  public LiveData<Boolean> inputtedIsNullCustomerShown() {
    return this._inputtedIsNullCustomerShown;
  }

  @NonNull
  public LiveData<Set<QueueModel.Status>> inputtedStatus() {
    return this._inputtedStatus;
  }

  @NonNull
  public LiveData<String> inputtedMinTotalPriceText() {
    return this._inputtedMinTotalPriceText;
  }

  @NonNull
  public LiveData<String> inputtedMaxTotalPriceText() {
    return this._inputtedMaxTotalPriceText;
  }

  @NonNull
  public LiveData<QueueDate> inputtedDate() {
    return this._inputtedDate;
  }

  /**
   * Get current inputted filter from any corresponding inputted live data. If any live data is set
   * using {@link MutableLiveData#postValue(Object)}, calling this method may not immediately
   * reflect the latest changes. For accurate results in asynchronous operations, consider calling
   * this method inside {@link Observer}.
   */
  @NonNull
  public QueueFilters inputtedFilters() {
    final List<Long> customerIds =
        Objects.requireNonNullElse(
            this._inputtedCustomerIds.getValue(), this._filterer.filters().filteredCustomerIds());
    final boolean isNullCustomerShown =
        Objects.requireNonNullElse(
            this._inputtedIsNullCustomerShown.getValue(),
            this._filterer.filters().isNullCustomerShown());
    final Set<QueueModel.Status> status =
        Objects.requireNonNullElse(
            this._inputtedStatus.getValue(), this._filterer.filters().filteredStatus());
    final QueueDate date =
        Objects.requireNonNullElse(
            this._inputtedDate.getValue(), this._filterer.filters().filteredDate());

    // Nullable value to represent unbounded range.
    BigDecimal minTotalPrice = this._filterer.filters().filteredTotalPrice().first;
    BigDecimal maxTotalPrice = this._filterer.filters().filteredTotalPrice().second;

    try {
      final String minTotalPriceText = this._inputtedMinTotalPriceText.getValue();

      if (minTotalPriceText != null && !minTotalPriceText.isBlank()) {
        minTotalPrice = CurrencyFormat.parse(minTotalPriceText, "id", "ID");
      }

    } catch (ParseException ignore) {
    }

    try {
      final String maxTotalPriceText = this._inputtedMaxTotalPriceText.getValue();

      if (maxTotalPriceText != null && !maxTotalPriceText.isBlank()) {
        maxTotalPrice = CurrencyFormat.parse(maxTotalPriceText, "id", "ID");
      }

    } catch (ParseException ignore) {
    }

    return QueueFilters.toBuilder()
        .setFilteredCustomerIds(customerIds)
        .setNullCustomerShown(isNullCustomerShown)
        .setFilteredStatus(status)
        .setFilteredDate(date)
        .setFilteredTotalPrice(new Pair<>(minTotalPrice, maxTotalPrice))
        .build();
  }

  public void onCustomersIdsChanged(@NonNull List<Long> customerIds) {
    Objects.requireNonNull(customerIds);

    this._inputtedCustomerIds.setValue(Collections.unmodifiableList(customerIds));
  }

  public void onNullCustomerShownEnabled(boolean isShown) {
    this._inputtedIsNullCustomerShown.setValue(isShown);
  }

  public void onStatusChanged(@NonNull Set<QueueModel.Status> status) {
    Objects.requireNonNull(status);

    this._inputtedStatus.setValue(Collections.unmodifiableSet(status));
  }

  public void onMinTotalPriceTextChanged(@NonNull String minTotalPrice) {
    Objects.requireNonNull(minTotalPrice);

    this._inputtedMinTotalPriceText.setValue(minTotalPrice);
  }

  public void onMaxTotalPriceTextChanged(@NonNull String maxTotalPrice) {
    Objects.requireNonNull(maxTotalPrice);

    this._inputtedMaxTotalPriceText.setValue(maxTotalPrice);
  }

  public void onDateChanged(@NonNull QueueDate date) {
    Objects.requireNonNull(date);

    this._inputtedDate.setValue(date);
  }

  public void onFiltersChanged(@NonNull QueueFilters filters) {
    final List<QueueModel> queues =
        Objects.requireNonNullElse(this._viewModel.queues().getValue(), new ArrayList<>());
    this.onFiltersChanged(filters, queues);
  }

  public void onFiltersChanged(@NonNull QueueFilters filters, @NonNull List<QueueModel> queues) {
    Objects.requireNonNull(filters);
    Objects.requireNonNull(queues);

    final String minTotalPrice =
        filters.filteredTotalPrice().first != null
            ? CurrencyFormat.format(filters.filteredTotalPrice().first, "id", "ID")
            : "";
    final String maxTotalPrice =
        filters.filteredTotalPrice().second != null
            ? CurrencyFormat.format(filters.filteredTotalPrice().second, "id", "ID")
            : "";

    this.onCustomersIdsChanged(filters.filteredCustomerIds());
    this.onNullCustomerShownEnabled(filters.isNullCustomerShown());
    this.onStatusChanged(filters.filteredStatus());
    this.onDateChanged(filters.filteredDate());
    this.onMinTotalPriceTextChanged(minTotalPrice);
    this.onMaxTotalPriceTextChanged(maxTotalPrice);
    this._filterer.setFilters(filters);

    final List<QueueModel> filteredQueues = this._filterer.filter(queues);
    final QueueSortMethod sortMethod = this._viewModel.sortMethod().getValue();

    // Re-sort the list, after previously re-populating the list with a new filtered value.
    if (sortMethod != null) this._viewModel.onSortMethodChanged(sortMethod, filteredQueues);
    // Put in the else to prevent multiple call of `QueueFilterViewModel#onCustomersChanged()`
    // from `QueueFilterViewModel#onSortMethodChanged()`.
    else this._viewModel.onQueuesChanged(filteredQueues);
  }
}
