///**
// * Copyright 2024 Robi
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.robifr.ledger.repository;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import com.robifr.ledger.data.model.Model;
//import com.robifr.ledger.data.model.QueueModel;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
///**
// * @implNote Every method with ID as a parameter should be using an object instead of its primitive
// *     type. So that we can easily query a foreign-key (nullable). Like when querying {@link
// *     QueueModel#customerId()}.
// */
//interface QueryReadable<M extends Model> {
//  /**
//   * @return List of selected models. Empty list for a failed operation.
//   */
//  @NonNull
//  public CompletableFuture<List<M>> selectAll();
//
//  /**
//   * @return Selected model. Null for a failed operation.
//   */
//  @NonNull
//  public CompletableFuture<M> selectById(@Nullable Long id);
//
//  /**
//   * @return List of selected models. Empty list for a failed operation.
//   */
//  @NonNull
//  public CompletableFuture<List<M>> selectById(@NonNull List<Long> ids);
//
//  @NonNull
//  public CompletableFuture<Boolean> isExistsById(@Nullable Long id);
//}
