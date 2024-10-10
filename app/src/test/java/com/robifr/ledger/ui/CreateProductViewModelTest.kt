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

package com.robifr.ledger.ui

import com.robifr.ledger.InstantTaskExecutorRuleForJUnit5
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.ext.awaitValue
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductState
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductViewModel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantTaskExecutorRuleForJUnit5::class)
open class CreateProductViewModelTest {
  protected open lateinit var _viewModel: CreateProductViewModel
  protected var _productRepository: ProductRepository = mock()
  protected val _dispatcher = StandardTestDispatcher()

  @BeforeEach
  open fun beforeEach() {
    Dispatchers.setMain(_dispatcher)
    reset(_productRepository)
    _viewModel = CreateProductViewModel(_productRepository)
  }

  @AfterEach
  fun afterEach() {
    Dispatchers.resetMain()
  }

  @Test
  fun `On state changed`() {
    var mirroredUiState: CreateProductState =
        CreateProductState(
            name = _viewModel.uiState.safeValue.name,
            nameErrorMessageRes = _viewModel.uiState.safeValue.nameErrorMessageRes,
            formattedPrice = _viewModel.uiState.safeValue.formattedPrice)

    _viewModel.onNameTextChanged("Apple")
    mirroredUiState = mirroredUiState.copy(name = "Apple")
    assertEquals(
        mirroredUiState, _viewModel.uiState.safeValue, "Preserve all values except for the name")

    _viewModel.onPriceTextChanged("$100")
    mirroredUiState = mirroredUiState.copy(formattedPrice = "$100")
    assertEquals(
        mirroredUiState,
        _viewModel.uiState.safeValue,
        "Preserve all values except for the formatted price")
  }

  @Test
  fun `On name changed with blank name`() {
    _viewModel.onNameTextChanged(" ")
    assertNull(
        _viewModel.uiState.safeValue.nameErrorMessageRes,
        "Remove error when there's no error beforehand")

    // Simulate error when saving with an empty name.
    _viewModel.onSave()
    assertNotNull(
        _viewModel.uiState.safeValue.nameErrorMessageRes,
        "Keep error when there's an error beforehand")
  }

  @Test
  fun `On name changed with filled name`() {
    _viewModel.onNameTextChanged("Apple")
    assertNull(_viewModel.uiState.safeValue.nameErrorMessageRes, "Remove error for a filled name")
  }

  @Test
  open fun `On save with blank name`() {
    _viewModel.onNameTextChanged(" ")
    _viewModel.onSave()
    assertNotNull(_viewModel.uiState.safeValue.nameErrorMessageRes, "Show error for a blank name")
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 123L])
  open fun `On save`(createdProductId: Long) {
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
