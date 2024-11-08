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

package com.robifr.ledger.ui.search.viewmodel

import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    every { _customerRepository.search(any()) } returns CompletableFuture.completedFuture(listOf())
    every { _productRepository.search(any()) } returns CompletableFuture.completedFuture(listOf())
    _viewModel.onSearch("A")
    _viewModel.onSearch("B")
    _viewModel.onSearch("C")
    advanceUntilIdle()
    assertDoesNotThrow("Prevent search from triggering multiple times when typing quickly") {
      verify(atMost = 1) { _customerRepository.search(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["A", "Amy", "Cal", "Apple", "Banana", "  "])
  fun `on search with complete query`(query: String) = runTest {
    val customers: List<CustomerModel> =
        if (query.contains("A", ignoreCase = true)) {
          listOf(CustomerModel(id = 111L, name = "Amy"), CustomerModel(id = 222L, name = "Cal"))
        } else {
          listOf()
        }
    val products: List<ProductModel> =
        if (query.contains("A", ignoreCase = true)) {
          listOf(ProductModel(id = 111L, name = "Apple"), ProductModel(id = 222L, name = "Banana"))
        } else {
          listOf()
        }
    every { _customerRepository.search(query) } returns CompletableFuture.completedFuture(customers)
    every { _productRepository.search(query) } returns CompletableFuture.completedFuture(products)
    _viewModel.onSearch(query)
    advanceUntilIdle()
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            query = query, customers = customers, products = products),
        _viewModel.uiState.safeValue,
        "Update customers and products based from the queried search result")
  }

  @ParameterizedTest
  @ValueSource(ints = [8, 10])
  fun `on search result a lot data`(totalData: Int) = runTest {
    val customers: List<CustomerModel> =
        List(totalData) { i -> CustomerModel(id = (i + 1) * 111L, name = "Customer ${i + 1}") }
    val products: List<ProductModel> =
        List(totalData) { i -> ProductModel(id = (i + 1) * 111L, name = "Product ${i + 1}") }
    every { _customerRepository.search(any()) } returns CompletableFuture.completedFuture(customers)
    every { _productRepository.search(any()) } returns CompletableFuture.completedFuture(products)
    _viewModel.onSearch("A")
    advanceUntilIdle()
    assertEquals(
        _viewModel.uiState.safeValue.copy(
            query = "A", customers = customers.take(8), products = products.take(8)),
        _viewModel.uiState.safeValue,
        "Limit queried data from the search up to 8")
  }
}
