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
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.ui.UiEvent
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerEvent
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerState
import com.robifr.ledger.ui.editcustomer.EditCustomerFragment
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
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
  private lateinit var _customerRepository: CustomerRepository
  private lateinit var _viewModel: EditCustomerViewModel
  private lateinit var _uiEventObserver: Observer<CreateCustomerEvent>
  private lateinit var _editResultEventObserver: Observer<UiEvent<EditCustomerResultState>>

  private val _customerToEdit: CustomerModel =
      CustomerModel(id = 111L, name = "Amy", balance = 100L, debt = (-100).toBigDecimal())

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _customerRepository = mockk()
    _uiEventObserver = mockk(relaxed = true)
    _editResultEventObserver = mockk(relaxed = true)

    coEvery { _customerRepository.add(any()) } returns 0L
    coEvery { _customerRepository.selectById(_customerToEdit.id) } returns _customerToEdit
    _viewModel =
        EditCustomerViewModel(
            _dispatcher,
            _customerRepository,
            SavedStateHandle().apply {
              set(
                  EditCustomerFragment.Arguments.INITIAL_CUSTOMER_ID_TO_EDIT_LONG.key(),
                  _customerToEdit.id)
            })
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
    _viewModel.editResultEvent.observe(_lifecycleOwner, _editResultEventObserver)
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
        "Match state with the retrieved data from the fragment arguments")
  }

  @Test
  fun `on initialize with empty initial customer`() {
    coEvery { _customerRepository.selectById(null) } returns null
    assertThrows<NullPointerException>("Can't edit customer if there's no customer ID provided") {
      runTest {
        _viewModel =
            EditCustomerViewModel(
                _dispatcher,
                _customerRepository,
                SavedStateHandle().apply {
                  set(EditCustomerFragment.Arguments.INITIAL_CUSTOMER_ID_TO_EDIT_LONG.key(), null)
                })
      }
    }
  }

  @Test
  fun `on save with blank name`() {
    _viewModel.onNameTextChanged(" ")

    coEvery { _customerRepository.update(any()) } returns 0
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

  @Test
  fun `on save with edited customer result update operation`() {
    // Prevent save with add operation (parent class behavior) instead of update operation.
    coEvery { _customerRepository.update(any()) } returns 0
    _viewModel.onSave()
    assertDoesNotThrow("Editing a customer shouldn't result in adding new data") {
      coVerify(exactly = 0) { _customerRepository.add(any()) }
      coVerify(exactly = 1) { _customerRepository.update(any()) }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `on save with edited customer`(effectedRows: Int) {
    coEvery { _customerRepository.update(any()) } returns effectedRows
    _viewModel.onSave()
    assertAll(
        {
          assertNotNull(
              _viewModel.uiEvent.safeValue.snackbar?.data, "Notify the result via snackbar")
        },
        {
          assertEquals(
              if (effectedRows != 0) EditCustomerResultState(_customerToEdit.id) else null,
              _viewModel.editResultEvent.value?.data,
              "Return result with the correct ID after success update")
        },
        {
          assertDoesNotThrow("Update result event last to finish the fragment") {
            verifyOrder {
              _uiEventObserver.onChanged(match { it.snackbar != null })
              if (effectedRows != 0) _editResultEventObserver.onChanged(any())
            }
          }
        })
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on back pressed`(isCustomerChanged: Boolean) {
    if (isCustomerChanged) _viewModel.onNameTextChanged("Ben")

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
