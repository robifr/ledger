/**
 * Copyright 2025 Robi
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

package io.github.robifr.ledger.local.access

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.robifr.ledger.data.display.ProductFilters
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.local.LocalDatabase
import org.junit.After
import org.junit.Before

abstract class ProductDaoBaseTest {
  protected lateinit var _database: LocalDatabase
  protected lateinit var _productDao: ProductDao

  protected val _product: ProductModel = ProductModel(id = null, name = "", price = 0L)
  protected val _filters: ProductFilters = ProductFilters(filteredPrice = null to null)

  @Before
  open fun before() {
    _database =
        Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                LocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    _productDao = _database.productDao()
  }

  @After
  open fun after() {
    _database.close()
  }
}
