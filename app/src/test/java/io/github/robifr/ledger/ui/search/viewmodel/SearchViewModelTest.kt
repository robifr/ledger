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

package io.github.robifr.ledger.ui.search.viewmodel

import io.github.robifr.ledger.InstantTaskExecutorExtension
import io.github.robifr.ledger.MainCoroutineExtension
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.repository.CustomerRepository
import io.github.robifr.ledger.repository.ProductRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class, MainCoroutineExtension::class)
class SearchViewModelTest(private val _dispatcher: TestDispatcher) {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _productRepository: ProductRepository
  private lateinit var _viewModel: SearchViewModel

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()
    _productRepository = mockk()
    _viewModel = SearchViewModel(_dispatcher, _customerRepository, _productRepository)
  }

  @Test
  fun `on search with fast input`() = runTest {
    coEvery { _customerRepository.search(any()) } returns listOf()
    coEvery { _productRepository.search(any()) } returns listOf()
    _viewModel.onSearch("A")
    _viewModel.onSearch("B")
    _viewModel.onSearch("C")
    advanceUntilIdle()
    assertThatCode { coVerify(atMost = 1) { _customerRepository.search(any()) } }
        .describedAs("Prevent search from triggering multiple times when typing quickly")
        .doesNotThrowAnyException()
  }

  @Test
  fun `on search with complete query`() = runTest {
    val customer: CustomerModel = CustomerModel(id = 111L, name = "Amy")
    val product: ProductModel = ProductModel(id = 111L, name = "Apple")
    coEvery { _customerRepository.search(any()) } returns listOf(customer)
    coEvery { _productRepository.search(any()) } returns listOf(product)
    _viewModel.onSearch("A")
    advanceUntilIdle()
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Update customers and products based from the queried search result")
        .isEqualTo(
            _viewModel.uiState.safeValue.copy(
                query = "A", customers = listOf(customer), products = listOf(product)))
  }
}
