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

package com.robifr.ledger.ui.createcustomer.viewmodel

import androidx.lifecycle.Observer
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.repository.CustomerRepository
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
class CreateCustomerViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _viewModel: CreateCustomerViewModel
  private lateinit var _uiEventObserver: Observer<CreateCustomerEvent>

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()
    _uiEventObserver = mockk(relaxed = true)
    _viewModel = CreateCustomerViewModel(_dispatcher, _customerRepository)
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
  }

  @Test
  fun `on state changed`() {
    _viewModel.onNameTextChanged("Amy")
    _viewModel.onBalanceChanged(100L)
    _viewModel.onDebtChanged((-100).toBigDecimal())
    assertEquals(
        CreateCustomerState(
            name = "Amy", nameErrorMessageRes = null, balance = 100L, debt = (-100).toBigDecimal()),
        _viewModel.uiState.safeValue,
        "Preserve all values except for the changed field")
  }

  @ParameterizedTest
  @ValueSource(strings = ["", " ", "Amy"])
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

    coEvery { _customerRepository.add(any()) } returns 0L
    _viewModel.onSave()
    assertAll(
        {
          assertNotNull(
              _viewModel.uiState.safeValue.nameErrorMessageRes, "Show error for a blank name")
        },
        {
          assertDoesNotThrow("Prevent save for a blank name") {
            coVerify(exactly = 0) { _customerRepository.add(any()) }
          }
        })
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 111L])
  fun `on save with created customer`(createdCustomerId: Long) {
    _viewModel.onNameTextChanged("Amy")
    _viewModel.onBalanceChanged(100L)
    _viewModel.onDebtChanged((-100).toBigDecimal())

    coEvery { _customerRepository.add(any()) } returns createdCustomerId
    _viewModel.onSave()
    assertAll(
        {
          assertNotNull(
              _viewModel.uiEvent.safeValue.snackbar?.data, "Notify the result via snackbar")
        },
        {
          assertEquals(
              if (createdCustomerId != 0L) CreateCustomerResultState(createdCustomerId) else null,
              _viewModel.uiEvent.safeValue.createResult?.data,
              "Return result with the correct ID after success save")
        },
        {
          assertDoesNotThrow("Update result event last to finish the fragment") {
            verifyOrder {
              _uiEventObserver.onChanged(match { it.snackbar != null && it.createResult == null })
              if (createdCustomerId != 0L) {
                _uiEventObserver.onChanged(match { it.createResult != null })
              }
            }
          }
        })
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on back pressed`(isCustomerChanged: Boolean) {
    if (isCustomerChanged) _viewModel.onNameTextChanged("Amy")

    _viewModel.onBackPressed()
    assertAll(
        {
          assertEquals(
              if (isCustomerChanged) true else null,
              _viewModel.uiEvent.safeValue.isUnsavedChangesDialogShown?.data,
              "Show unsaved changes dialog when there's a change")
        },
        {
          assertEquals(
              if (!isCustomerChanged) true else null,
              _viewModel.uiEvent.safeValue.isFragmentFinished?.data,
              "Finish fragment when there's no change")
        })
  }
}
