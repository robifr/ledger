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

package com.robifr.ledger.ui.createproduct.viewmodel

import com.robifr.ledger.InstantTaskExecutorRuleForJUnit5
import com.robifr.ledger.MainCoroutineRule
import com.robifr.ledger.awaitValue
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.repository.ProductRepository
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorRuleForJUnit5::class, MainCoroutineRule::class)
open class CreateProductViewModelTest {
  protected open lateinit var _viewModel: CreateProductViewModel
  protected val _productRepository: ProductRepository = mock()

  @BeforeEach
  open fun beforeEach() {
    reset(_productRepository)
    whenever(_productRepository.add(any())).thenReturn(CompletableFuture.completedFuture(0L))
    _viewModel = CreateProductViewModel(_productRepository)
  }

  @Test
  fun `on state changed`() {
    _viewModel.onNameTextChanged("Apple")
    _viewModel.onPriceTextChanged("$100")
    assertEquals(
        CreateProductState(name = "Apple", nameErrorMessageRes = null, formattedPrice = "$100"),
        _viewModel.uiState.safeValue,
        "Preserve all values except for the one changed")
  }

  @ParameterizedTest
  @ValueSource(strings = ["", " ", "Apple"])
  fun `on name changed`(name: String) {
    _viewModel.onNameTextChanged(name)
    if (name.isNotBlank()) {
      assertNull(_viewModel.uiState.safeValue.nameErrorMessageRes, "Remove error for a filled name")
    } else {
      assertNull(
          _viewModel.uiState.safeValue.nameErrorMessageRes,
          "Remove error when there's no error beforehand")

      // Simulate error when editing with a blank name.
      _viewModel.onSave()
      _viewModel.onNameTextChanged(name)
      assertNotNull(
          _viewModel.uiState.safeValue.nameErrorMessageRes,
          "Keep error when there's an error beforehand")
    }
  }

  @Test
  fun `on save with blank name`() {
    _viewModel.onNameTextChanged(" ")
    _viewModel.onSave()
    assertNotNull(_viewModel.uiState.safeValue.nameErrorMessageRes, "Show error for a blank name")
    assertDoesNotThrow("Prevent save for a blank name") {
      verify(_productRepository, never()).add(any())
    }
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 123L])
  open fun `on save`(createdProductId: Long) = runTest {
    val product: ProductModel = ProductModel(id = null, name = "Apple", price = 100)
    _viewModel.onNameTextChanged("Apple")
    _viewModel.onPriceTextChanged("$100")

    whenever(_productRepository.add(product))
        .thenReturn(CompletableFuture.completedFuture(createdProductId))
    _viewModel.onSave()
    if (createdProductId == 0L) {
      assertThrows(
          TimeoutException::class.java,
          { _viewModel.resultState.awaitValue() },
          "Don't return result for a failed save")
    } else {
      _viewModel.resultState.awaitValue().handleIfNotHandled {
        assertEquals(
            createdProductId,
            it.createdProductId,
            "Return result with the correct ID after success save")
      }
    }
  }
}
