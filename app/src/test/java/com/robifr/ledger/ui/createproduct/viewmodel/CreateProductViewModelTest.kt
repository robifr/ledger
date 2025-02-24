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

import androidx.lifecycle.Observer
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.repository.ProductRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(
    InstantTaskExecutorExtension::class,
    MainCoroutineExtension::class,
    LifecycleOwnerExtension::class)
class CreateProductViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _productRepository: ProductRepository
  private lateinit var _viewModel: CreateProductViewModel
  private lateinit var _uiEventObserver: Observer<CreateProductEvent>

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productRepository = mockk()
    _uiEventObserver = mockk(relaxed = true)
    _viewModel = CreateProductViewModel(_dispatcher, _productRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on state changed`() {
    _viewModel.onNameTextChanged("Apple")
    _viewModel.onPriceTextChanged("$1.00")
    assertEquals(
        CreateProductState(name = "Apple", nameErrorMessageRes = null, formattedPrice = "$1.00"),
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
      assertAll(
          {
            assertNull(
                _viewModel.uiState.safeValue.nameErrorMessageRes,
                "Remove error when there's no error beforehand")
          },
          {
            // Simulate error when editing with a blank name.
            _viewModel.onSave()
            _viewModel.onNameTextChanged(name)
            assertNotNull(
                _viewModel.uiState.safeValue.nameErrorMessageRes,
                "Keep error when there's an error beforehand")
          })
    }
  }

  @Test
  fun `on save with blank name`() {
    _viewModel.onNameTextChanged(" ")

    coEvery { _productRepository.add(any()) } returns 0L
    _viewModel.onSave()
    assertAll(
        {
          assertNotNull(
              _viewModel.uiState.safeValue.nameErrorMessageRes, "Show error for a blank name")
        },
        {
          assertDoesNotThrow("Prevent save for a blank name") {
            coVerify(exactly = 0) { _productRepository.add(any()) }
          }
        })
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 111L])
  fun `on save with created product`(createdProductId: Long) {
    _viewModel.onNameTextChanged("Apple")
    _viewModel.onPriceTextChanged("$1.00")

    coEvery { _productRepository.add(any()) } returns createdProductId
    _viewModel.onSave()
    assertAll(
        {
          assertNotNull(
              _viewModel.uiEvent.safeValue.snackbar?.data, "Notify the result via snackbar")
        },
        {
          assertEquals(
              if (createdProductId != 0L) CreateProductResultState(createdProductId) else null,
              _viewModel.uiEvent.safeValue.createResult?.data,
              "Return result with the correct ID after success save")
        },
        {
          assertDoesNotThrow("Update result event last to finish the fragment") {
            verifyOrder {
              _uiEventObserver.onChanged(match { it.snackbar != null && it.createResult == null })
              if (createdProductId != 0L) {
                _uiEventObserver.onChanged(match { it.createResult != null })
              }
            }
          }
        })
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on back pressed`(isProductChanged: Boolean) {
    if (isProductChanged) _viewModel.onNameTextChanged("Apple")

    _viewModel.onBackPressed()
    assertAll(
        {
          assertEquals(
              if (isProductChanged) true else null,
              _viewModel.uiEvent.safeValue.isUnsavedChangesDialogShown?.data,
              "Show unsaved changes dialog when there's a change")
        },
        {
          assertEquals(
              if (!isProductChanged) true else null,
              _viewModel.uiEvent.safeValue.isFragmentFinished?.data,
              "Finish fragment when there's no change")
        })
  }
}
