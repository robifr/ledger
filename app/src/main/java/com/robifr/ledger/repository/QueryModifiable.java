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
//import com.robifr.ledger.data.model.Model;
//import java.util.concurrent.CompletableFuture;
//
//interface QueryModifiable<M extends Model> {
//  /**
//   * @return Inserted model ID. 0 for a failed operation.
//   */
//  @NonNull
//  public CompletableFuture<Long> add(@NonNull M model);
//
//  /**
//   * @return Number of row effected. 0 for a failed operation.
//   */
//  @NonNull
//  public CompletableFuture<Integer> update(@NonNull M model);
//
//  /**
//   * @return Number of row effected. 0 for a failed operation.
//   */
//  @NonNull
//  public CompletableFuture<Integer> delete(@NonNull M model);
//}
