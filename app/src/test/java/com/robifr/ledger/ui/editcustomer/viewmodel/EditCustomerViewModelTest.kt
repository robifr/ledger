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

import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorRuleForJUnit5
import com.robifr.ledger.MainCoroutineRule
import com.robifr.ledger.awaitValue
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerState
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerViewModelTest
import com.robifr.ledger.ui.editcustomer.EditCustomerFragment
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorRuleForJUnit5::class, MainCoroutineRule::class)
class EditCustomerViewModelTest : CreateCustomerViewModelTest() {
  private val _editCustomerViewModel: EditCustomerViewModel
    get() = _viewModel as EditCustomerViewModel

  private val _customerToEdit: CustomerModel =
      CustomerModel(id = 123L, name = "Amy", balance = 100L, debt = (-100).toBigDecimal())

  @BeforeEach
  override fun beforeEach() {
    reset(_customerRepository)
    whenever(_customerRepository.selectById(_customerToEdit.id))
        .thenReturn(CompletableFuture.completedFuture(_customerToEdit))
    _viewModel =
        EditCustomerViewModel(
            _customerRepository,
            SavedStateHandle().apply {
              set(
                  EditCustomerFragment.Arguments.INITIAL_CUSTOMER_ID_TO_EDIT_LONG.key,
                  _customerToEdit.id)
            })
  }

  @Test
  fun `on initialize with arguments`() {
    assertEquals(
        CreateCustomerState(
            name = _customerToEdit.name,
            nameErrorMessageRes = null,
            balance = _customerToEdit.balance,
            debt = _customerToEdit.debt),
        _editCustomerViewModel.uiState.toLiveData().awaitValue(),
        "The UI state should match the retrieved data based from the provided customer ID")
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `on save with edited customer`(effectedRows: Int) = runTest {
    _editCustomerViewModel.onNameTextChanged(_customerToEdit.name)
    _editCustomerViewModel.onBalanceChanged(_customerToEdit.balance)
    _editCustomerViewModel.onDebtChanged(_customerToEdit.debt)

    whenever(_customerRepository.update(_customerToEdit))
        .thenReturn(CompletableFuture.completedFuture(effectedRows))
    _editCustomerViewModel.onSave()
    if (effectedRows == 0) {
      assertThrows(
          TimeoutException::class.java,
          { _viewModel.resultState.awaitValue() },
          "Don't return result for a failed save")
    } else {
      _editCustomerViewModel.editResultState.awaitValue().handleIfNotHandled {
        assertEquals(
            _customerToEdit.id,
            it.editedCustomerId,
            "Return result with the correct ID after success save")
      }
    }
  }

  @ParameterizedTest
  @ValueSource(longs = [0L])
  override fun `on save`(createdCustomerId: Long) {
    // Prevent save with add operation (parent class behavior) instead of update operation.
    whenever(_customerRepository.add(_customerToEdit))
        .thenReturn(CompletableFuture.completedFuture(createdCustomerId))
    whenever(_customerRepository.update(any())).thenReturn(CompletableFuture.completedFuture(0))
    _editCustomerViewModel.onSave()
    assertDoesNotThrow("Editing a customer shouldn't result in adding new data") {
      verify(_customerRepository, never()).add(_customerToEdit)
      verify(_customerRepository, atLeastOnce()).update(_customerToEdit)
    }
  }
}
