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

package io.github.robifr.ledger.ui.common.state

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Convenient utility extension function to update UI events (which are states), from:
 * ```kt
 * _uiEvent.setValue(
 *    _uiEvent.value?.copy(
 *        snackbar =
 *            UiEvent(
 *                data = "Hello",
 *                onConsumed = {
 *                  _uiEvent.setValue(_uiEvent.value?.copy(snackbar = null))
 *                })))
 * ```
 *
 * to:
 * ```kt
 * _uiEvent.updateEvent(
 *    data = "Hello",
 *    onSet = { this?.copy(snackbar = it) },
 *    onReset = { this?.copy(snackbar = null) })
 * ```
 *
 * @param T Data in the UI state.
 * @param S UI state.
 * @see UiEvent
 */
suspend inline fun <T, S> MutableLiveData<S>.updateEvent(
    data: T,
    crossinline onSet: S?.(UiEvent<T>) -> S?,
    crossinline onReset: S?.() -> S?
) {
  val newEvent: UiEvent<T> = UiEvent(data) { value = onReset(value) }
  withContext(Dispatchers.Main.immediate) { value = onSet(value, newEvent) }
}
