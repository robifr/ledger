/**
 * Copyright 2025 Robi
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

package com.robifr.ledger.ui.common.state

/**
 * A generic state wrapper for one-time operation like displaying a snackbar, toast, etc.
 *
 * ```kt
 * data class ViewModelEvent(val snackbar: UiEvent<String>? = null)
 *
 * // ViewModel.
 * private val _uiEvent: MutableLiveData<ViewModelEvent> = MutableLiveData(ViewModelEvent())
 * val uiEvent: LiveData<ViewModelEvent>
 *    get() = _uiEvent
 *
 * // Somewhere in ViewModel to notify the event.
 * // Writing all these stuff might seems verbose and error prone.
 * // Consider using `updateEvent()` extension function in the below link.
 * _uiEvent.setValue(
 *    _uiEvent.value?.copy(
 *        snackbar =
 *            UiEvent(
 *                data = "Hello",
 *                onConsumed = {
 *                  _uiEvent.setValue(_uiEvent.value?.copy(snackbar = null))
 *                })))
 *
 * // Fragment.
 * viewModel.uiEvent.observe(viewLifecycleOwner) { event ->
 *    event?.snackbar?.let {
 *      // Received: "Hello"
 *      Snackbar.make(requireView(), it.data, Snackbar.LENGTH_LONG).show()
 *      it.onConsumed() // Consume to prevent the event from reappearing.
 *    }
 * }
 * ```
 *
 * @see updateEvent
 */
data class UiEvent<out T>(val data: T, val onConsumed: () -> Unit)
