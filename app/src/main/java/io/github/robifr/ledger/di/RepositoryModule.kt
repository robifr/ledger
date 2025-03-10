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

package io.github.robifr.ledger.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.robifr.ledger.local.LocalDatabase
import io.github.robifr.ledger.local.TransactionProvider
import io.github.robifr.ledger.repository.CustomerRepository
import io.github.robifr.ledger.repository.ProductOrderRepository
import io.github.robifr.ledger.repository.ProductRepository
import io.github.robifr.ledger.repository.QueueRepository
import io.github.robifr.ledger.repository.SettingsRepository

@Module
@InstallIn(ViewModelComponent::class, ActivityComponent::class)
object RepositoryModule {
  @Provides
  fun provideCustomerRepository(localDb: LocalDatabase): CustomerRepository =
      CustomerRepository.instance(localDb.customerDao())

  @Provides
  fun provideProductRepository(localDb: LocalDatabase): ProductRepository =
      ProductRepository.instance(localDb.productDao())

  @Provides
  fun provideProductOrderRepository(localDb: LocalDatabase): ProductOrderRepository =
      ProductOrderRepository.instance(localDb.productOrderDao())

  @Provides
  fun provideQueueRepository(
      localDb: LocalDatabase,
      customerRepository: CustomerRepository,
      productOrderRepository: ProductOrderRepository
  ): QueueRepository =
      QueueRepository.instance(
          localDb.queueDao(),
          TransactionProvider(localDb),
          customerRepository,
          productOrderRepository)

  @Provides
  fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
      SettingsRepository.instance(context.applicationContext)
}
