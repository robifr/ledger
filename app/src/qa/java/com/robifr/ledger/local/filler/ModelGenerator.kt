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

package com.robifr.ledger.local.filler

import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.collections.random
import kotlin.collections.sumOf
import net.datafaker.Faker

class ModelGenerator(private val _faker: Faker = Faker()) {
  fun generateQueues(
      customers: List<CustomerModel>,
      weightedProducts: List<WeightedProductModel>
  ): List<QueueModel> =
      mutableListOf<QueueModel>().apply {
        for (i in 0 until 150) {
          val customer: CustomerModel = customers.random()
          val date: Instant =
              _faker
                  .timeAndDate()
                  .between(
                      LocalDateTime.now()
                          .minus(1, ChronoUnit.MONTHS)
                          .withDayOfMonth(1)
                          .atZone(ZoneId.systemDefault())
                          .toInstant(),
                      Instant.now())
          val tempQueue: QueueModel =
              QueueModel(
                  customerId = customer.id,
                  customer = customer,
                  status = _randomWeightedOf(_weightedQueueStatus(date)),
                  date = date,
                  // Use account balance as the temporary payment method to check whether
                  // the customer have sufficient balance or not.
                  paymentMethod = QueueModel.PaymentMethod.ACCOUNT_BALANCE,
                  productOrders = _generateProductOrders(weightedProducts))
          add(
              tempQueue.copy(
                  paymentMethod =
                      _randomWeightedOf(
                          _weighedQueuePaymentMethod(
                              tempQueue.status, customer.isBalanceSufficient(null, tempQueue)))))
        }
      }

  fun generateCustomers(): List<CustomerModel> =
      mutableListOf<CustomerModel>().apply {
        for (i in 0 until 30) {
          add(
              CustomerModel(
                  name = _faker.name().fullName(),
                  balance = _faker.number().randomDouble(0, 0, 300).toLong()))
        }
      }

  fun generateWeightedProducts(): List<WeightedProductModel> =
      listOf(
          WeightedProductModel(
              product = ProductModel(name = "Regular Clean & Fold (lb)", price = 2L),
              weight = 20,
              quantityRange = 10 to 30,
              quantityDecimalCount = 1),
          WeightedProductModel(
              product = ProductModel(name = "Express Clean & Fold (lb)", price = 4L),
              weight = 15,
              quantityRange = 5 to 20,
              quantityDecimalCount = 1),
          WeightedProductModel(
              product = ProductModel(name = "Blouse (piece)", price = 6L),
              weight = 12,
              quantityRange = 1 to 5,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Sweater (piece)", price = 8L),
              weight = 8,
              quantityRange = 1 to 4,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Jeans (piece)", price = 9L),
              weight = 10,
              quantityRange = 1 to 5,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Dress (piece)", price = 12L),
              weight = 7,
              quantityRange = 1 to 3,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Light Jacket (piece)", price = 12L),
              weight = 6,
              quantityRange = 1 to 3,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Coat (piece)", price = 15L),
              weight = 5,
              quantityRange = 1 to 2,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Suit (piece)", price = 18L),
              weight = 4,
              quantityRange = 1 to 2,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Curtains (piece)", price = 20L),
              weight = 6,
              quantityRange = 1 to 2,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Bed Sheets (piece)", price = 20L),
              weight = 8,
              quantityRange = 1 to 3,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Rug (piece)", price = 25L),
              weight = 3,
              quantityRange = 1 to 2,
              quantityDecimalCount = 0),
          WeightedProductModel(
              product = ProductModel(name = "Leather Jacket (piece)", price = 35L),
              weight = 3,
              quantityRange = 1 to 2,
              quantityDecimalCount = 0))

  private fun _generateProductOrders(
      weightedProducts: List<WeightedProductModel>
  ): List<ProductOrderModel> =
      mutableListOf<ProductOrderModel>().apply {
        for (i in 0 until (1..5).random()) {
          var weightedProduct: WeightedProductModel
          do {
            weightedProduct = _randomWeightedOf(weightedProducts.map { it to it.weight })
            // Only add products that haven't been previously added.
          } while (weightedProduct.product in map { it.referencedProduct() })

          val quantity: Double =
              _faker
                  .number()
                  .randomDouble(
                      weightedProduct.quantityDecimalCount,
                      weightedProduct.quantityRange.first,
                      weightedProduct.quantityRange.second)
          add(
              ProductOrderModel(
                  productId = weightedProduct.product.id,
                  productName = weightedProduct.product.name,
                  productPrice = weightedProduct.product.price,
                  quantity = quantity,
                  // Apply a discount near 5% or no discount at all. The exact percentage may vary
                  // due to the limitations of the discount field, which can't handle decimals.
                  discount =
                      if (Math.random() < 0.5) {
                        ProductOrderModel.calculateTotalPrice(
                                weightedProduct.product.price, quantity, 0L)
                            .multiply(5.toBigDecimal())
                            .divide(100.toBigDecimal(), 2, RoundingMode.HALF_UP)
                            .toLong()
                      } else {
                        0L
                      }))
        }
      }

  private fun _weightedQueueStatus(date: Instant): List<Pair<QueueModel.Status, Int>> =
      when (Duration.between(date, Instant.now()).toDays()) {
        in 8..Long.MAX_VALUE ->
            listOf(
                QueueModel.Status.IN_QUEUE to 0,
                QueueModel.Status.IN_PROCESS to 0,
                QueueModel.Status.UNPAID to 5,
                QueueModel.Status.COMPLETED to 95)
        in 4..7 ->
            listOf(
                QueueModel.Status.IN_QUEUE to 5,
                QueueModel.Status.IN_PROCESS to 85,
                QueueModel.Status.UNPAID to 1,
                QueueModel.Status.COMPLETED to 9)
        else ->
            listOf(
                QueueModel.Status.IN_QUEUE to 87,
                QueueModel.Status.IN_PROCESS to 10,
                QueueModel.Status.UNPAID to 1,
                QueueModel.Status.COMPLETED to 2)
      }

  private fun _weighedQueuePaymentMethod(
      status: QueueModel.Status,
      isCustomerBalanceSufficient: Boolean
  ): List<Pair<QueueModel.PaymentMethod, Int>> =
      if (status == QueueModel.Status.COMPLETED && isCustomerBalanceSufficient) {
        listOf(QueueModel.PaymentMethod.CASH to 50, QueueModel.PaymentMethod.ACCOUNT_BALANCE to 50)
      } else {
        listOf(QueueModel.PaymentMethod.CASH to 100, QueueModel.PaymentMethod.ACCOUNT_BALANCE to 0)
      }

  private fun <T> _randomWeightedOf(weightedPairs: List<Pair<T, Int>>): T {
    val randomValue: Int = (1..weightedPairs.sumOf { it.second }).random()
    var weightCount: Int = 0
    for ((item, weight) in weightedPairs) {
      weightCount += weight
      if (randomValue <= weightCount) return item
    }
    return weightedPairs.random().first // Should be impossible to reach.
  }
}
