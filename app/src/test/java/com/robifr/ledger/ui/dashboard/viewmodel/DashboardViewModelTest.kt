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
import com.robifr.ledger.local.access.FakeCustomerDao
import com.robifr.ledger.local.access.FakeProductOrderDao
import com.robifr.ledger.local.access.FakeQueueDao
import com.robifr.ledger.onLifecycleOwnerDestroyed
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductOrderRepository
import com.robifr.ledger.repository.QueueRepository
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class DashboardViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _queueRepository: QueueRepository
  private lateinit var _queueDao: FakeQueueDao
  private lateinit var _productOrderRepository: ProductOrderRepository
  private lateinit var _productOrderDao: FakeProductOrderDao
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _customerDao: FakeCustomerDao
  private lateinit var _viewModel: DashboardViewModel

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _queueDao = FakeQueueDao(data = mutableListOf())
    _productOrderDao = FakeProductOrderDao(data = mutableListOf(), queueData = _queueDao.data)
    _customerDao =
        FakeCustomerDao(
            data = mutableListOf(),
            queueData = _queueDao.data,
            productOrderData = _productOrderDao.data)
    _productOrderRepository = spyk(ProductOrderRepository(_productOrderDao))
    _customerRepository = spyk(CustomerRepository(_customerDao))
    _queueRepository =
        spyk(QueueRepository(_queueDao, mockk(), _customerRepository, _productOrderRepository))
    _viewModel =
        DashboardViewModel(
            _dispatcher, _queueRepository, _productOrderRepository, _customerRepository)
  }

  @Test
  fun `on cleared`() {
    _viewModel.onLifecycleOwnerDestroyed()
    assertThatCode {
          verify(exactly = 2) { _queueRepository.removeModelChangedListener(any()) }
          verify(exactly = 1) { _productOrderRepository.removeModelChangedListener(any()) }
          verify(exactly = 2) { _customerRepository.removeModelChangedListener(any()) }
        }
        .describedAs("Remove attached listener from the repository")
        .doesNotThrowAnyException()
  }
}
