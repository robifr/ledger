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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun <T> LiveData<T>.awaitValue(time: Long = 100L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): T {
  var data: T? = null
  val latch = CountDownLatch(1)
  val observer =
      object : Observer<T> {
        override fun onChanged(value: T) {
          data = value
          latch.countDown()
          removeObserver(this)
        }
      }
  observeForever(observer)

  // Throw when the time up, prevents observing forever.
  if (!latch.await(time, timeUnit)) {
    removeObserver(observer)
    throw TimeoutException("Live data value was never set")
  }
  return data!!
}
