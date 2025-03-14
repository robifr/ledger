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

package io.github.robifr.ledger

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class InstantTaskExecutorExtension : AfterEachCallback, BeforeEachCallback {
  override fun beforeEach(context: ExtensionContext?) {
    ArchTaskExecutor.getInstance()
        .setDelegate(
            object : TaskExecutor() {
              override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
              }

              override fun postToMainThread(runnable: Runnable) {
                runnable.run()
              }

              override fun isMainThread(): Boolean = true
            })
  }

  override fun afterEach(context: ExtensionContext?) {
    ArchTaskExecutor.getInstance().setDelegate(null)
  }
}
