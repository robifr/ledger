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

package com.robifr.ledger.di

import android.content.Context
import com.robifr.ledger.local.LocalDatabase
import com.robifr.ledger.local.TransactionProvider
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductOrderRepository
import com.robifr.ledger.repository.ProductRepository
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ViewModelComponent::class, ActivityComponent::class)
object RepositoryModule {
  @Provides
  fun provideCustomerRepository(@ApplicationContext context: Context): CustomerRepository =
      CustomerRepository.instance(LocalDatabase.instance(context.applicationContext).customerDao())

  @Provides
  fun provideProductRepository(@ApplicationContext context: Context): ProductRepository =
      ProductRepository.instance(LocalDatabase.instance(context.applicationContext).productDao())

  @Provides
  fun provideProductOrderRepository(@ApplicationContext context: Context): ProductOrderRepository =
      ProductOrderRepository.instance(
          LocalDatabase.instance(context.applicationContext).productOrderDao())

  @Provides
  fun provideQueueRepository(
      @ApplicationContext context: Context,
      customerRepository: CustomerRepository,
      productOrderRepository: ProductOrderRepository
  ): QueueRepository {
    val localDb: LocalDatabase = LocalDatabase.instance(context.applicationContext)
    return QueueRepository.instance(
        localDb.queueDao(),
        TransactionProvider(localDb),
        customerRepository,
        productOrderRepository)
  }

  @Provides
  fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
      SettingsRepository.instance(context.applicationContext)
}
