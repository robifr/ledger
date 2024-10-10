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

package com.robifr.ledger.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

sealed interface SafeLiveData<T> {
  val safeValue: T

  fun toLiveData(): LiveData<T>

  fun observe(owner: LifecycleOwner, observer: Observer<in T>)

  fun observeForever(observer: Observer<in T>)

  fun removeObserver(observer: Observer<in T>)
}

class SafeMediatorLiveData<T>(safeValue: T) : MediatorLiveData<T>(safeValue), SafeLiveData<T> {
  override val safeValue
    get() = super.getValue()!!

  override fun toLiveData(): LiveData<T> = this

  override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
    super.observe(owner) { value: T? -> value?.let { observer.onChanged(it) } }
  }

  override fun observeForever(observer: Observer<in T>) {
    super.observeForever { value: T? -> value?.let { observer.onChanged(it) } }
  }

  override fun removeObserver(observer: Observer<in T>) {
    super.removeObserver(observer)
  }

  override fun <S> addSource(source: LiveData<S>, observer: Observer<in S>) {
    super.addSource(source) { value: S? -> value?.let { observer.onChanged(it) } }
  }
}

class SafeMutableLiveData<T>(safeValue: T) : MutableLiveData<T>(safeValue), SafeLiveData<T> {
  override val safeValue
    get() = super.getValue()!!

  override fun toLiveData(): LiveData<T> = this

  override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
    super.observe(owner) { value: T? -> value?.let { observer.onChanged(it) } }
  }

  override fun observeForever(observer: Observer<in T>) {
    super.observeForever { value: T? -> value?.let { observer.onChanged(it) } }
  }

  override fun removeObserver(observer: Observer<in T>) {
    super.removeObserver(observer)
  }

  override fun setValue(value: T) {
    super.setValue(value)
  }

  override fun postValue(value: T) {
    super.postValue(value)
  }
}
