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

"use strict";

/**
 * @typedef {Object} AndroidInterface
 * @property {(colorName: string) => string} colorHex
 * @property {(
 *     amount: number,
 *     languageTag: string,
 *     symbol: string) => string} formatCurrencyWithUnit
 * @property {() => string} localeLanguageTag
 */

/**
 * @type {AndroidInterface}
 */
// @ts-ignore
export let Android = window.Android;

// @ts-ignore
export let d3 = window.d3;