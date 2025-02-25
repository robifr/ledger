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

package com.robifr.ledger.ui.editproduct.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorExtension
import com.robifr.ledger.LifecycleOwnerExtension
import com.robifr.ledger.LifecycleTestOwner
import com.robifr.ledger.MainCoroutineExtension
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.common.state.UiEvent
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductEvent
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductState
import com.robifr.ledger.ui.editproduct.EditProductFragment
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
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
class EditProductViewModelTest(
    private val _dispatcher: TestDispatcher,
    private val _lifecycleOwner: LifecycleTestOwner
) {
  private lateinit var _productRepository: ProductRepository
  private lateinit var _viewModel: EditProductViewModel
  private lateinit var _uiEventObserver: Observer<CreateProductEvent>
  private lateinit var _editResultEventObserver: Observer<UiEvent<EditProductResultState>>

  private val _productToEdit: ProductModel = ProductModel(id = 111L, name = "Apple", price = 100L)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
    _productRepository = mockk()
    _uiEventObserver = mockk(relaxed = true)
    _editResultEventObserver = mockk(relaxed = true)
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en-US"))

    coEvery { _productRepository.add(any()) } returns 0L
    coEvery { _productRepository.selectById(_productToEdit.id) } returns _productToEdit
    _viewModel =
        EditProductViewModel(
            _dispatcher,
            _productRepository,
            SavedStateHandle().apply {
              set(
                  EditProductFragment.Arguments.INITIAL_PRODUCT_ID_TO_EDIT_LONG.key(),
                  _productToEdit.id)
            })
    _viewModel.uiEvent.observe(_lifecycleOwner, _uiEventObserver)
    _viewModel.editResultEvent.observe(_lifecycleOwner, _editResultEventObserver)
  }

  @Test
  fun `on initialize with arguments`() {
    assertThat(_viewModel.uiState.safeValue)
        .describedAs("Match state with the retrieved data from the fragment arguments")
        .isEqualTo(
            CreateProductState(
                name = _productToEdit.name,
                nameErrorMessageRes = null,
                formattedPrice = "$${_productToEdit.price / 100L}"))
  }

  @Test
  fun `on initialize with empty initial product`() {
    coEvery { _productRepository.selectById(null) } returns null
    assertThatThrownBy {
          runTest {
            _viewModel =
                EditProductViewModel(
                    _dispatcher,
                    _productRepository,
                    SavedStateHandle().apply {
                      set(EditProductFragment.Arguments.INITIAL_PRODUCT_ID_TO_EDIT_LONG.key(), null)
                    })
          }
        }
        .describedAs("Can't edit product if there's no product ID provided")
  }

  @Test
  fun `on save with blank name`() {
    _viewModel.onNameTextChanged(" ")

    coEvery { _productRepository.update(any()) } returns 0
    _viewModel.onSave()
    assertSoftly {
      it.assertThat(_viewModel.uiState.safeValue.nameErrorMessageRes)
          .describedAs("Show error for a blank name")
          .isNotNull()
      it.assertThatCode { coVerify(exactly = 0) { _productRepository.update(any()) } }
          .describedAs("Prevent save for a blank name")
          .doesNotThrowAnyException()
    }
  }

  @Test
  fun `on save with edited product result update operation`() {
    // Prevent save with add operation (parent class behavior) instead of update operation.
    coEvery { _productRepository.update(any()) } returns 0
    _viewModel.onSave()
    assertThatCode {
          coVerify(exactly = 0) { _productRepository.add(any()) }
          coVerify(exactly = 1) { _productRepository.update(any()) }
        }
        .describedAs("Editing a product shouldn't result in adding new data")
        .doesNotThrowAnyException()
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `on save with edited product`(effectedRows: Int) {
    coEvery { _productRepository.update(any()) } returns effectedRows
    _viewModel.onSave()
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.snackbar?.data)
          .describedAs("Notify the result via snackbar")
          .isNotNull()
      it.assertThat(_viewModel.editResultEvent.value?.data)
          .describedAs("Return result with the correct ID after success update")
          .isEqualTo(if (effectedRows != 0) EditProductResultState(_productToEdit.id) else null)
      it.assertThatCode {
            verifyOrder {
              _uiEventObserver.onChanged(match { it.snackbar != null })
              if (effectedRows != 0) _editResultEventObserver.onChanged(any())
            }
          }
          .describedAs("Update result event last to finish the fragment")
          .doesNotThrowAnyException()
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `on back pressed`(isProductChanged: Boolean) {
    if (isProductChanged) _viewModel.onNameTextChanged("Banana")

    _viewModel.onBackPressed()
    assertSoftly {
      it.assertThat(_viewModel.uiEvent.safeValue.isUnsavedChangesDialogShown?.data)
          .describedAs("Show unsaved changes dialog when there's a change")
          .isEqualTo(if (isProductChanged) true else null)
      it.assertThat(_viewModel.uiEvent.safeValue.isFragmentFinished?.data)
          .describedAs("Finish fragment when there's no change")
          .isEqualTo(if (!isProductChanged) true else null)
    }
  }
}
