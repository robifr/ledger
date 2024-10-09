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

import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorRuleForJUnit5
import com.robifr.ledger.MainCoroutineRule
import com.robifr.ledger.awaitValue
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductState
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductViewModelTest
import com.robifr.ledger.ui.editproduct.EditProductFragment
import com.robifr.ledger.util.CurrencyFormat
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
class EditProductViewModelTest : CreateProductViewModelTest() {
  private val _editProductViewModel: EditProductViewModel
    get() = _viewModel as EditProductViewModel

  private val _productToEdit: ProductModel = ProductModel(id = 123L, name = "Apple", price = 100)

  @BeforeEach
  override fun beforeEach() {
    reset(_productRepository)
    whenever(_productRepository.selectById(_productToEdit.id))
        .thenReturn(CompletableFuture.completedFuture(_productToEdit))
    _viewModel =
        EditProductViewModel(
            _productRepository,
            SavedStateHandle().apply {
              set(
                  EditProductFragment.Arguments.INITIAL_PRODUCT_ID_TO_EDIT_LONG.key,
                  _productToEdit.id)
            })
  }

  @Test
  fun `on initialize with arguments`() {
    assertEquals(
        CreateProductState(
            name = _productToEdit.name,
            nameErrorMessageRes = null,
            formattedPrice = CurrencyFormat.format(_productToEdit.price.toBigDecimal(), "")),
        _editProductViewModel.uiState.toLiveData().awaitValue(),
        "The UI state should match the retrieved data based from the provided product ID")
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `on save with edited product`(effectedRows: Int) = runTest {
    _editProductViewModel.onNameTextChanged(_productToEdit.name)
    _editProductViewModel.onPriceTextChanged(
        CurrencyFormat.format(_productToEdit.price.toBigDecimal(), ""))

    whenever(_productRepository.update(_productToEdit))
        .thenReturn(CompletableFuture.completedFuture(effectedRows))
    _editProductViewModel.onSave()
    if (effectedRows == 0) {
      assertThrows(
          TimeoutException::class.java,
          { _viewModel.resultState.awaitValue() },
          "Don't return result for a failed save")
    } else {
      _editProductViewModel.editResultState.awaitValue().handleIfNotHandled {
        assertEquals(
            _productToEdit.id,
            it.editedProductId,
            "Return result with the correct ID after success save")
      }
    }
  }

  @ParameterizedTest
  @ValueSource(longs = [0L])
  override fun `on save`(createdProductId: Long) {
    // Prevent save with add operation (parent class behavior) instead of update operation.
    whenever(_productRepository.add(_productToEdit))
        .thenReturn(CompletableFuture.completedFuture(createdProductId))
    whenever(_productRepository.update(any())).thenReturn(CompletableFuture.completedFuture(0))
    _editProductViewModel.onSave()
    assertDoesNotThrow("Editing a product shouldn't result in adding new data") {
      verify(_productRepository, never()).add(_productToEdit)
      verify(_productRepository, atLeastOnce()).update(_productToEdit)
    }
  }
}
