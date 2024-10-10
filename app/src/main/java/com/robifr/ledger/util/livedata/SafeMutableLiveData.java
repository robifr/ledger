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
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.Objects;

@Deprecated
public class SafeMutableLiveData<T> extends MutableLiveData<T> implements SafeLiveData<T> {
  public SafeMutableLiveData(@NonNull T value) {
    super(value);
  }

  /**
   * @return Current value
   */
  @NonNull
  @Override
  public T getValue() {
    return Objects.requireNonNull(super.getValue());
  }

  @Override
  public void setValue(@NonNull T value) {
    Objects.requireNonNull(value);

    super.setValue(value);
  }

  @Override
  public void postValue(@NonNull T value) {
    Objects.requireNonNull(value);

    super.postValue(value);
  }

  @Override
  public void observe(@NonNull LifecycleOwner owner, @NonNull SafeObserver<? super T> observer) {
    Objects.requireNonNull(owner);
    Objects.requireNonNull(owner);

    super.observe(
        owner,
        value -> {
          if (value != null) observer.onChanged(value);
        });
  }

  @Override
  public void observeForever(@NonNull SafeObserver<? super T> observer) {
    Objects.requireNonNull(observer);

    super.observeForever(
        value -> {
          if (value != null) observer.onChanged(value);
        });
  }

  @NonNull
  @Override
  public LiveData<T> toLiveData() {
    return this;
  }
}
