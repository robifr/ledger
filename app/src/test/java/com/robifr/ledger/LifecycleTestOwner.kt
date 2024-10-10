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

package com.robifr.ledger

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class LifecycleTestOwner : LifecycleOwner {
  private val _registry: LifecycleRegistry = LifecycleRegistry(this)
  override val lifecycle: Lifecycle = _registry

  fun onCreate() {
    _registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
  }

  fun onResume() {
    _registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
  }

  fun onDestroy() {
    _registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  }
}
