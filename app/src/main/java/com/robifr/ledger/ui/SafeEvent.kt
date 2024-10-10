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

import androidx.lifecycle.LiveData
import java.util.function.Consumer

/**
 * A [LiveData] is good for maintaining lifecycle-aware state, like on configuration changes (device
 * rotation). But for events like displaying a snackbar, toast, and navigation, the observer should
 * only observe once and they shouldn't observe after configuration changes. This event will let
 * multiple observers to observe the changes for once.
 *
 * ```kt
 * // MainFragment.kt
 * viewModel.state.observe(viewLifecycleOwner) {
 *    it.handleIfNotHandled {
 *      //Message received: "Hello"
 *      msg -> Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show()
 *    }
 * }
 *
 * // MainViewModel.kt
 * class MainViewModel : ViewModel {
 *    private val _state: MutableLiveData<SafeEvent<String>> = MutableLiveData()
 *    val state:  LiveData<SafeEvent<String>>
 *      get() = _state
 * }
 *
 * // Somewhere in MainViewModel.kt
 * _state.setValue(SafeEvent("Hello"))
 * ```
 */
class SafeEvent<T>(private val _value: T) {
  val valueIfNotHandled: T?
    get() =
        if (!_isHandled) {
          _isHandled = true
          _value
        } else {
          null
        }

  private var _isHandled: Boolean = false

  fun handleIfNotHandled(handler: Consumer<T>) {
    valueIfNotHandled?.let { handler.accept(it) }
  }
}
