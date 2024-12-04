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
import com.robifr.ledger.local.access.CustomerDao
import com.robifr.ledger.local.access.ProductDao
import com.robifr.ledger.local.access.ProductOrderDao
import com.robifr.ledger.local.access.QueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class DaoModule {
  @Provides
  fun provideCustomerDao(@ApplicationContext context: Context): CustomerDao =
      LocalDatabase.instance(context.applicationContext).customerDao()

  @Provides
  fun provideProductDao(@ApplicationContext context: Context): ProductDao =
      LocalDatabase.instance(context.applicationContext).productDao()

  @Provides
  fun provideProductOrderDao(@ApplicationContext context: Context): ProductOrderDao =
      LocalDatabase.instance(context.applicationContext).productOrderDao()

  @Provides
  fun provideQueueDao(@ApplicationContext context: Context): QueueDao =
      LocalDatabase.instance(context.applicationContext).queueDao()
}

