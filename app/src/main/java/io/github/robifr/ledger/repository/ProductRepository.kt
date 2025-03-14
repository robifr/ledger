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

package io.github.robifr.ledger.repository

import io.github.robifr.ledger.data.display.ProductFilters
import io.github.robifr.ledger.data.display.ProductSortMethod
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductPaginatedInfo
import io.github.robifr.ledger.local.access.ProductDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(private val _localDao: ProductDao) : Queryable<ProductModel> {
  private val _modelChangedListeners: HashSet<ModelChangedListener<ProductModel>> = hashSetOf()

  fun addModelChangedListener(listener: ModelChangedListener<ProductModel>) {
    _modelChangedListeners.add(listener)
  }

  fun removeModelChangedListener(listener: ModelChangedListener<ProductModel>) {
    _modelChangedListeners.remove(listener)
  }

  override suspend fun selectAll(): List<ProductModel> = _localDao.selectAll()

  override suspend fun selectById(id: Long?): ProductModel? = _localDao.selectById(id)

  override suspend fun selectById(ids: List<Long>): List<ProductModel> = _localDao.selectById(ids)

  override suspend fun isExistsById(id: Long?): Boolean = _localDao.isExistsById(id)

  override suspend fun isTableEmpty(): Boolean = _localDao.isTableEmpty()

  override suspend fun add(model: ProductModel): Long =
      _localDao
          .insert(model)
          .let { rowId -> _localDao.selectIdByRowId(rowId) }
          .also { insertedId -> selectById(insertedId)?.let { _notifyModelAdded(listOf(it)) } }

  override suspend fun update(model: ProductModel): Int =
      _localDao.update(model).also { effectedRows ->
        if (effectedRows > 0) selectById(model.id)?.let { _notifyModelUpdated(listOf(it)) }
      }

  override suspend fun delete(id: Long?): Int {
    // Note: Referenced product ID on product order table will automatically set
    //    to null upon product deletion.
    val deletedProduct: ProductModel = selectById(id) ?: return 0
    return _localDao.delete(id).also { effectedRows ->
      if (effectedRows > 0) _notifyModelDeleted(listOf(deletedProduct))
    }
  }

  suspend fun selectPaginatedInfoByOffset(
      pageNumber: Int,
      itemPerPage: Int,
      limit: Int,
      sortMethod: ProductSortMethod,
      filters: ProductFilters
  ): List<ProductPaginatedInfo> =
      _localDao.selectPaginatedInfoByOffset(
          pageNumber = pageNumber,
          itemPerPage = itemPerPage,
          limit = limit,
          sortBy = sortMethod.sortBy,
          isAscending = sortMethod.isAscending,
          filteredMinPrice = filters.filteredPrice.first,
          filteredMaxPrice = filters.filteredPrice.second)

  suspend fun countFilteredProducts(filters: ProductFilters): Long =
      _localDao.countFilteredProducts(
          filteredMinPrice = filters.filteredPrice.first,
          filteredMaxPrice = filters.filteredPrice.second)

  suspend fun search(query: String): List<ProductModel> = _localDao.search(query)

  private suspend fun _notifyModelAdded(models: List<ProductModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelAdded(models) } }
  }

  private suspend fun _notifyModelUpdated(models: List<ProductModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelUpdated(models) } }
  }

  private suspend fun _notifyModelDeleted(models: List<ProductModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelDeleted(models) } }
  }

  companion object {
    @Volatile private var _instance: ProductRepository? = null

    @Synchronized
    fun instance(productDao: ProductDao): ProductRepository =
        _instance ?: ProductRepository(productDao).apply { _instance = this }
  }
}
