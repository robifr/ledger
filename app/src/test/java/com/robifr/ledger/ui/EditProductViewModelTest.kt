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

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.SavedStateHandle
import com.robifr.ledger.InstantTaskExecutorRuleForJUnit5
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.ext.awaitValue
import com.robifr.ledger.ui.createproduct.viewmodel.CreateProductState
import com.robifr.ledger.ui.editproduct.EditProductFragment
import com.robifr.ledger.ui.editproduct.viewmodel.EditProductViewModel
import com.robifr.ledger.util.CurrencyFormat
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantTaskExecutorRuleForJUnit5::class)
class EditProductViewModelTest : CreateProductViewModelTest() {
  private val _savedStateHandle: SavedStateHandle = mock()
  private val _productToEdit: ProductModel = ProductModel(id = 123L, name = "Apple", price = 100)

  @BeforeEach
  override fun beforeEach() {
    Dispatchers.setMain(_dispatcher)
    reset(_productRepository)

    whenever(
            _savedStateHandle.get<Long>(
                EditProductFragment.Arguments.INITIAL_PRODUCT_ID_TO_EDIT_LONG.key()))
        .thenReturn(_productToEdit.id)
    whenever(_productRepository.selectById(_productToEdit.id))
        .thenReturn(CompletableFuture.completedFuture(_productToEdit))
    _viewModel = EditProductViewModel(_productRepository, _savedStateHandle)
  }

  @Test
  fun `On initialize with arguments`() {
    assertEquals(
        CreateProductState(
            name = _productToEdit.name,
            nameErrorMessageRes = null,
            formattedPrice =
                CurrencyFormat.format(
                    _productToEdit.price.toBigDecimal(),
                    AppCompatDelegate.getApplicationLocales().toLanguageTags())),
        _viewModel.uiState.safeValue,
        "The UI state should match the retrieved data based from the provided product ID")
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `On save with edited product`(effectedRows: Int) {
    _viewModel.onNameTextChanged(_productToEdit.name)
    _viewModel.onPriceTextChanged(
        CurrencyFormat.format(
            _productToEdit.price.toBigDecimal(),
            AppCompatDelegate.getApplicationLocales().toLanguageTags()))

    whenever(_productRepository.update(_productToEdit))
        .thenReturn(CompletableFuture.completedFuture(effectedRows))
    _viewModel.onSave()
    if (effectedRows == 0) {
      assertThrows(
          TimeoutException::class.java,
          { (_viewModel as EditProductViewModel).editResultState.awaitValue() },
          "Don't return result for a failed save")
    } else {
      (_viewModel as EditProductViewModel).editResultState.awaitValue().handleIfNotHandled {
        assertEquals(
            _productToEdit.id,
            it.editedProductId,
            "Return result with the correct ID after success save")
      }
    }
  }

  @ParameterizedTest
  @ValueSource(longs = [0L])
  override fun `On save`(createdProductId: Long) {
    whenever(_productRepository.update(_productToEdit))
        .thenReturn(CompletableFuture.completedFuture(0))

    // Prevent save with add operation (parent class behavior) instead of update operation.
    whenever(_productRepository.add(_productToEdit))
        .thenReturn(CompletableFuture.completedFuture(createdProductId))
    _viewModel.onSave()
    assertDoesNotThrow("Editing a product shouldn't result in adding new data") {
      verify(_productRepository, never()).add(_productToEdit)
    }
  }
}
