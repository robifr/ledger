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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Preserve all values except for the changed field")
        .isEqualTo(
            CreateCustomerState(
                name = "Amy",
                nameErrorMessageRes = null,
                balance = 100L,
                debt = (-100).toBigDecimal()))
  }

  @ParameterizedTest
  @ValueSource(strings = ["", " ", "Amy"])
  fun `on name changed`(name: String) {
    _viewModel.onNameTextChanged(name)
    if (name.isNotBlank()) {
      assertThat(_viewModel.uiState.safeValue.nameErrorMessageRes)
          .describedAs("Remove error for a filled name")
          .isNull()
    } else {
      assertSoftly {
        it.assertThat(_viewModel.uiState.safeValue.nameErrorMessageRes)
            .describedAs("Remove error when there's no error beforehand")
            .isNull()

        // Simulate error when editing with a blank name.
        _viewModel.onSave()
        _viewModel.onNameTextChanged(name)
        it.assertThat(_viewModel.uiState.safeValue.nameErrorMessageRes)
            .describedAs("Keep error when there's an error beforehand")
            .isNotNull()
      }
    }
  }

  @Test
  fun `on save with blank name`() {
    _viewModel.onNameTextChanged(" ")

    coEvery { _customerRepository.add(any()) } returns 0L
    _viewModel.onSave()
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.nameErrorMessageRes)
          .describedAs("Show error for a blank name")
          .isNotNull()
      it.assertThatCode { coVerify(exactly = 0) { _customerRepository.add(any()) } }
          .describedAs("Prevent save for a blank name")
          .doesNotThrowAnyException()
    }
  }

  @ParameterizedTest
  @ValueSource(longs = [0L, 111L])
  fun `on save with created customer`(createdCustomerId: Long) {
    _viewModel.onNameTextChanged("Amy")
    _viewModel.onBalanceChanged(100L)
    _viewModel.onDebtChanged((-100).toBigDecimal())

    coEvery { _customerRepository.add(any()) } returns createdCustomerId
    _viewModel.onSave()
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
          .describedAs("Notify the result via snackbar")
          .isNotNull()
      it.assertThat(_viewModel.uiEvent.safeValue.createResult?.data)
          .describedAs("Return result with the correct ID after success save")
          .isEqualTo(
              if (createdCustomerId != 0L) CreateCustomerResultState(createdCustomerId) else null)
      it.assertThatCode {
            verifyOrder {
              _uiEventObserver.onChanged(match { it.snackbar != null && it.createResult == null })
              if (createdCustomerId != 0L) {
                _uiEventObserver.onChanged(match { it.createResult != null })
              }
            }
          }
          .describedAs("Update result event last to finish the fragment")
          .doesNotThrowAnyException()
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on back pressed`(isCustomerChanged: Boolean) {
    if (isCustomerChanged) _viewModel.onNameTextChanged("Amy")

    _viewModel.onBackPressed()
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.isUnsavedChangesDialogShown?.data)
          .describedAs("Show unsaved changes dialog when there's a change")
          .isEqualTo(if (isCustomerChanged) true else null)
      it.assertThat(_viewModel.uiEvent.safeValue.isFragmentFinished?.data)
          .describedAs("Finish fragment when there's no change")
          .isEqualTo(if (!isCustomerChanged) true else null)
    }
  }
}
