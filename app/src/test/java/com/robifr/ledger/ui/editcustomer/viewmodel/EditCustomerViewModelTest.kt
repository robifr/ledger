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

package com.robifr.ledger.ui.editcustomer.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.awaitValue
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.SafeEvent
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerState
import com.robifr.ledger.ui.editcustomer.EditCustomerFragment
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExperimentalCoroutinesApi
@ExtendWith(
    InstantTaskExecutorExtension::class,
    MainCoroutineExtension::class,
    LifecycleOwnerExtension::class)
class EditCustomerViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _resultStateObserver: Observer<SafeEvent<EditCustomerResultState>>
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _viewModel: EditCustomerViewModel

  private val _customerToEdit: CustomerModel =
      CustomerModel(id = 111L, name = "Amy", balance = 100L, debt = (-100).toBigDecimal())

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _resultStateObserver = mockk(relaxed = true)
    _customerRepository = mockk()

    every { _customerRepository.add(any()) } returns CompletableFuture.completedFuture(0L)
    every { _customerRepository.selectById(_customerToEdit.id) } returns
        CompletableFuture.completedFuture(_customerToEdit)
    _viewModel =
        EditCustomerViewModel(
            _customerRepository,
            _dispatcher,
            SavedStateHandle().apply {
              set(
                  EditCustomerFragment.Arguments.INITIAL_CUSTOMER_ID_TO_EDIT_LONG.key,
                  _customerToEdit.id)
            })
    _viewModel.editResultState.observe(_lifecycleOwner, _resultStateObserver)
  }

  @Test
  fun `on initialize with arguments`() {
    assertEquals(
        CreateCustomerState(
            name = _customerToEdit.name,
            nameErrorMessageRes = null,
            balance = _customerToEdit.balance,
            debt = _customerToEdit.debt),
        _viewModel.uiState.safeValue,
        "The state should match the retrieved data based from the provided customer ID")
  }

  @Test
  fun `on save with blank name`() {
    _viewModel.onNameTextChanged(" ")

    every { _customerRepository.update(any()) } returns CompletableFuture.completedFuture(0)
    _viewModel.onSave()
    assertNotNull(_viewModel.uiState.safeValue.nameErrorMessageRes, "Show error for a blank name")
    assertDoesNotThrow("Prevent save for a blank name") {
      verify(exactly = 0) { _customerRepository.add(any()) }
    }
  }

  @Test
  fun `on save with edited customer result update operation`() {
    // Prevent save with add operation (parent class behavior) instead of update operation.
    every { _customerRepository.update(any()) } returns CompletableFuture.completedFuture(0)
    _viewModel.onSave()
    assertDoesNotThrow("Editing a customer shouldn't result in adding new data") {
      verify(exactly = 0) { _customerRepository.add(any()) }
      verify(exactly = 1) { _customerRepository.update(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `on save with edited customer`(effectedRows: Int) {
    every { _customerRepository.update(any()) } returns
        CompletableFuture.completedFuture(effectedRows)
    _viewModel.onSave()
    if (effectedRows == 0) {
      assertDoesNotThrow("Don't return result for a failed save") {
        verify(exactly = 0) { _resultStateObserver.onChanged(any()) }
      }
    } else {
      assertEquals(
          _customerToEdit.id,
          _viewModel.editResultState.awaitValue().valueIfNotHandled?.editedCustomerId,
          "Return result with the correct ID after success save")
    }
  }
}
