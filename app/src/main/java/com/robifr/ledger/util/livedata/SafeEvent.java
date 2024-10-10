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

package com.robifr.ledger.util.livedata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A {@link LiveData} is good for maintaining lifecycle-aware state, like on configuration changes
 * (device rotation). But for events like displaying a snackbar, toast, and navigation, the observer
 * should only observe once and they shouldn't observe after configuration changes. This event will
 * let multiple observers to observe the changes for once.
 *
 * <pre>
 * MainFragment.java
 * {@code
 *  viewModel.snackbar().observe(
 *      this.getViewLifecycleOwner(),
 *      event -> event.handleIfNotHandled(
 *          //Message received: "Hello"
 *          message -> Snackbar.make(this, message, Snackbar.LENGTH_LONG).show());
 * }
 *
 * MainViewModel.java
 * {@code
 *  public class MainViewModel extends ViewModel {
 *    @NonNull private final MutableLiveData<SafeEvent<String>> _snackbar =
 *        new MutableLiveData<>();
 *
 *    @NonNull
 *    public LiveData<SafeEvent<String>> snackbar() {
 *      return this._snackbar;
 *    }
 *    ...
 *  }
 * }
 *
 * Somewhere in MainViewModel.java
 * {@code this._snackbar.setValue(new SafeEvent<>("Hello")); }
 * </pre>
 */
@Deprecated
public class SafeEvent<T> {
  @NonNull private final T _value;
  private boolean _isHandled = false;

  public SafeEvent(@NonNull T value) {
    this._value = Objects.requireNonNull(value);
  }

  public static <T> void observeOnce(
      @NonNull LiveData<T> liveData,
      @NonNull androidx.lifecycle.Observer<T> observer,
      @NonNull Predicate<T> filter) {
    Objects.requireNonNull(liveData);
    Objects.requireNonNull(observer);
    Objects.requireNonNull(filter);

    liveData.observeForever(
        new androidx.lifecycle.Observer<>() {
          @Override
          public void onChanged(@Nullable T data) {
            if (filter.test(data)) {
              observer.onChanged(data);
              liveData.removeObserver(this);
            }
          }
        });
  }

  @Nullable
  public T valueIfNotHandled() {
    if (this._isHandled) return null;

    this._isHandled = true;
    return this._value;
  }

  @NonNull
  public T value() {
    return this._value;
  }

  public boolean isHandled() {
    return this._isHandled;
  }

  public void handleIfNotHandled(@NonNull Consumer<T> handler) {
    Objects.requireNonNull(handler);

    final T value = this.valueIfNotHandled();
    if (value != null) handler.accept(value);
  }
}
