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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

@ExperimentalCoroutinesApi
class MainCoroutineExtension(private val _dispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    AfterEachCallback, BeforeEachCallback, ParameterResolver {
  override fun beforeEach(context: ExtensionContext?) {
    Dispatchers.setMain(_dispatcher)
  }

  override fun afterEach(context: ExtensionContext?) {
    Dispatchers.resetMain()
  }

  override fun supportsParameter(
      parameterContext: ParameterContext?,
      extensionContext: ExtensionContext?
  ): Boolean = parameterContext?.parameter?.type == TestDispatcher::class.java

  override fun resolveParameter(
      parameterContext: ParameterContext?,
      extensionContext: ExtensionContext?
  ): TestDispatcher = _dispatcher
}
