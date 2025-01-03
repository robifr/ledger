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

package com.robifr.ledger.ui.dashboard.viewmodel

import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.QueueRepository
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class DashboardViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _viewModel: DashboardViewModel

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _queueRepository = mockk()
    _customerRepository = mockk()

    coEvery { _queueRepository.selectAllInRange(any(), any()) } returns listOf()
    coEvery { _customerRepository.selectAllInfoWithBalance() } returns listOf()
    coEvery { _customerRepository.selectAllInfoWithDebt() } returns listOf()
    every { _queueRepository.addModelChangedListener(any()) } just Runs
    every { _customerRepository.addModelChangedListener(any()) } just Runs
    _viewModel = DashboardViewModel(_dispatcher, _queueRepository, _customerRepository)
  }

  @Test
  fun `on cleared`() {
    every { _queueRepository.removeModelChangedListener(any()) } just Runs
    every { _customerRepository.removeModelChangedListener(any()) } just Runs
    _viewModel.onLifecycleOwnerDestroyed()
    assertDoesNotThrow("Remove attached listener from the repository") {
      verify(exactly = 2) { _queueRepository.removeModelChangedListener(any()) }
      verify(exactly = 2) { _customerRepository.removeModelChangedListener(any()) }
    }
  }
}
