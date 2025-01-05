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

package com.robifr.ledger.ui.main

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.local.LocalDatabase
import com.robifr.ledger.local.filler.ModelGenerator
import com.robifr.ledger.local.filler.WeightedProductModel
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.repository.QueueRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QaMainActivity : MainActivity() {
  @Inject lateinit var _localDb: LocalDatabase
  @Inject lateinit var _queueRepository: QueueRepository
  @Inject lateinit var _customerRepository: CustomerRepository
  @Inject lateinit var _productRepository: ProductRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (_permission.isStorageAccessGranted()) _prepopulateDb()
  }

  private fun _prepopulateDb() {
    lifecycleScope.launch(Dispatchers.IO) {
      if (!_queueRepository.isTableEmpty() ||
          !_customerRepository.isTableEmpty() ||
          !_productRepository.isTableEmpty()) {
        return@launch
      }

      val generator: ModelGenerator = ModelGenerator()
      generator.generateCustomers().forEach { _customerRepository.add(it) }
      val generatedWeightedProducts: List<WeightedProductModel> =
          generator.generateWeightedProducts()
      generatedWeightedProducts.forEach { _productRepository.add(it.product) }

      val insertedProducts: List<ProductModel> = _productRepository.selectAll()
      val insertedWeightedProducts: List<WeightedProductModel> =
          generatedWeightedProducts.map { weightedProduct ->
            val insertedProduct: ProductModel? =
                insertedProducts.find {
                  // Compare all fields except their ID. The generated product doesn't have ID.
                  it.copy(id = null) == weightedProduct.product.copy(id = null)
                }
            if (insertedProduct != null) weightedProduct.copy(product = insertedProduct)
            else weightedProduct
          }
      // Pass the inserted customers and products so that their IDs can be attached to the queue.
      generator.generateQueues(_customerRepository.selectAll(), insertedWeightedProducts).forEach {
        _queueRepository.add(it)
      }
    }
  }
}
