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

package com.robifr.ledger.ui.editqueue.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.robifr.ledger.R;
import com.robifr.ledger.data.model.CustomerModel;
import com.robifr.ledger.data.model.QueueModel;
import com.robifr.ledger.repository.CustomerRepository;
import com.robifr.ledger.repository.ProductRepository;
import com.robifr.ledger.repository.QueueRepository;
import com.robifr.ledger.ui.StringResources;
import com.robifr.ledger.ui.createqueue.viewmodel.CreateQueueViewModel;
import com.robifr.ledger.ui.editqueue.EditQueueFragment;
import com.robifr.ledger.util.livedata.SafeEvent;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

@HiltViewModel
public class EditQueueViewModel extends CreateQueueViewModel {
  @Nullable private QueueModel _initialQueueToEdit = null;

  @NonNull
  private final MutableLiveData<SafeEvent<Optional<Long>>> _resultEditedQueueId =
      new MutableLiveData<>();

  @Inject
  public EditQueueViewModel(
      @NonNull QueueRepository queueRepository,
      @NonNull CustomerRepository customerRepository,
      @NonNull ProductRepository productRepository,
      @NonNull SavedStateHandle savedStateHandle) {
    super(queueRepository, customerRepository, productRepository);
    Objects.requireNonNull(savedStateHandle);

    // Setting up initial values inside a fragment is painful. See commit d5604599.
    SafeEvent.observeOnce(
        // Shouldn't be null when editing data.
        this.selectQueueById(
            Objects.requireNonNull(
                savedStateHandle.get(
                    EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key()))),
        queue -> {
          this._initialQueueToEdit = queue;
          this.onCustomerChanged(queue.customer());
          this.onDateChanged(queue.date().atZone(ZoneId.systemDefault()));
          this.onStatusChanged(queue.status());
          this.onPaymentMethodChanged(queue.paymentMethod());
          this.onProductOrdersChanged(queue.productOrders());
        },
        Objects::nonNull);
  }

  @Override
  @NonNull
  public QueueModel inputtedQueue() {
    final Long id =
        this._initialQueueToEdit != null && this._initialQueueToEdit.id() != null
            ? this._initialQueueToEdit.id()
            : null;
    return super.inputtedQueue().withId(id);
  }

  @Override
  public void onSave() {
    final QueueModel inputtedQueue = this.inputtedQueue();

    if (inputtedQueue.productOrders().isEmpty()) {
      this._snackbarMessage.setValue(
          new SafeEvent<>(
              new StringResources.Strings(R.string.createQueue_includeOneProductOrderError)));
      return;
    }

    this._updateQueue(inputtedQueue);
  }

  @NonNull
  public LiveData<SafeEvent<Optional<Long>>> resultEditedQueueId() {
    return this._resultEditedQueueId;
  }

  @NonNull
  public LiveData<QueueModel> selectQueueById(@Nullable Long queueId) {
    final MutableLiveData<QueueModel> result = new MutableLiveData<>();

    this._queueRepository
        .selectById(queueId)
        .thenAcceptAsync(
            queue -> {
              if (queue == null) {
                this._snackbarMessage.postValue(
                    new SafeEvent<>(
                        new StringResources.Strings(R.string.createQueue_fetchQueueError)));
              }

              result.postValue(queue);
            });
    return result;
  }

  @Override
  protected void _onUpdateAllowedPaymentMethod() {
    final QueueModel inputtedQueue = this.inputtedQueue();
    final HashSet<QueueModel.PaymentMethod> allowedPaymentMethods =
        new HashSet<>(this._allowedPaymentMethods.getValue());
    final boolean isBalanceSufficient =
        this._inputtedCustomer
            .getValue()
            .filter(
                customer ->
                    this._initialQueueToEdit != null
                        && inputtedQueue.status() == QueueModel.Status.COMPLETED
                        && customer.isBalanceSufficient(this._initialQueueToEdit, inputtedQueue))
            .isPresent();
    final boolean isTemporalBalancePositive =
        // Compare with the account balance payment option
        // as if the user does it before they actually do.
        this._inputtedCustomer
            .getValue()
            .filter(
                customer ->
                    this._initialQueueToEdit != null
                        && customer.balanceOnUpdatedPayment(
                                this._initialQueueToEdit,
                                inputtedQueue.withPaymentMethod(
                                    QueueModel.PaymentMethod.ACCOUNT_BALANCE))
                            >= 0L)
            .isPresent();

    if (isBalanceSufficient && isTemporalBalancePositive) {
      allowedPaymentMethods.add(QueueModel.PaymentMethod.ACCOUNT_BALANCE);
    } else {
      allowedPaymentMethods.remove(QueueModel.PaymentMethod.ACCOUNT_BALANCE);
    }

    this._allowedPaymentMethods.setValue(Collections.unmodifiableSet(allowedPaymentMethods));

    // Change payment method to cash when current selected one marked as not allowed.
    if (!allowedPaymentMethods.contains(inputtedQueue.paymentMethod())) {
      this.onPaymentMethodChanged(QueueModel.PaymentMethod.CASH);
    }
  }

  @Override
  protected void _onUpdateTemporalInputtedCustomer() {
    final QueueModel inputtedQueue = this.inputtedQueue();
    final CustomerModel customer =
        this._inputtedCustomer
            .getValue()
            .filter(model -> this._initialQueueToEdit != null)
            .map(
                model ->
                    model
                        .withBalance(
                            model.balanceOnUpdatedPayment(this._initialQueueToEdit, inputtedQueue))
                        .withDebt(
                            model.debtOnUpdatedPayment(this._initialQueueToEdit, inputtedQueue)))
            .orElse(null);

    this._temporalInputtedCustomer.setValue(Optional.ofNullable(customer));
  }

  private void _updateQueue(@NonNull QueueModel queue) {
    Objects.requireNonNull(queue);

    this._queueRepository
        .update(queue)
        .thenAcceptAsync(
            effected -> {
              if (effected > 0) {
                this._resultEditedQueueId.postValue(
                    new SafeEvent<>(Optional.ofNullable(queue.id())));
              }

              final StringResources stringRes =
                  effected > 0
                      ? new StringResources.Plurals(
                          R.plurals.createQueue_added_n_queue, effected, effected)
                      : new StringResources.Strings(R.string.createQueue_addQueueError);
              this._snackbarMessage.postValue(new SafeEvent<>(stringRes));
            });
  }
}
