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
import io.github.robifr.ledger.data.display.QueueDate
import io.github.robifr.ledger.data.display.QueueFilters
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.local.LocalDatabase
import java.time.Instant
import org.junit.After
import org.junit.Before

abstract class QueueDaoBaseTest {
  protected lateinit var _database: LocalDatabase
  protected lateinit var _queueDao: QueueDao

  protected val _queue: QueueModel =
      QueueModel(
          id = null,
          customerId = null,
          customer = null,
          status = QueueModel.Status.IN_QUEUE,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders = listOf())
  protected val _filters: QueueFilters =
      QueueFilters(
          filteredCustomerIds = listOf(),
          isNullCustomerShown = true,
          filteredStatus = QueueModel.Status.entries.toSet(),
          filteredTotalPrice = null to null,
          filteredDate = QueueDate(QueueDate.Range.ALL_TIME))

  @Before
  open fun before() {
    _database =
        Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                LocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    _queueDao = _database.queueDao()
  }

  @After
  open fun after() {
    _database.close()
  }
}
