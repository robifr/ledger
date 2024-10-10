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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [LiveData] is good for maintaining lifecycle-aware state, like on configuration changes (device
 * rotation). But for events like displaying a snackbar, toast, and navigation, the observer should
 * only observe once and they shouldn't observe after configuration changes. This event will let
 * multiple observers to observe the changes for once.
 *
 * ```kt
 * // MainFragment.kt
 * viewModel.state.observe(viewLifecycleOwner) {
 *    //Message received: "Hello"
 *    Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
 * }
 *
 * // MainViewModel.kt
 * class MainViewModel : ViewModel() {
 *    private val _state: SingleLiveEvent<String> = SingleLiveEvent()
 *    val state: LiveData<String>
 *      get() = _state
 * }
 *
 * // Somewhere in MainViewModel.kt
 * _state.setValue("Hello")
 * ```
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
  private val _isPending: AtomicBoolean = AtomicBoolean(false)

  override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
    super.observe(owner) { if (_isPending.compareAndSet(true, false)) observer.onChanged(it) }
  }

  override fun setValue(value: T) {
    _isPending.set(true)
    super.setValue(value)
  }

  override fun postValue(value: T) {
    _isPending.set(true)
    super.postValue(value)
  }
}
