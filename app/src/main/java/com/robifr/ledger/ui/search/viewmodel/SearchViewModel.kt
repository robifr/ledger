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

package com.robifr.ledger.ui.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _customerRepository: CustomerRepository,
    private val _productRepository: ProductRepository
) : ViewModel() {
  private var _searchJob: Job? = null

  private val _uiState: SafeMutableLiveData<SearchState> =
      SafeMutableLiveData(SearchState(customers = listOf(), products = listOf(), query = ""))
  val uiState: SafeLiveData<SearchState>
    get() = _uiState

  fun onSearch(query: String) {
    // Remove old job to ensure old query results don't appear in the future.
    _searchJob?.cancel()
    _searchJob =
        viewModelScope.launch(_dispatcher) {
          delay(300L)
          _customerRepository.search(query).take(8).also {
            _uiState.postValue(_uiState.safeValue.copy(query = query, customers = it))
          }
          _productRepository.search(query).take(8).also {
            _uiState.postValue(_uiState.safeValue.copy(query = query, products = it))
          }
        }
  }
}
