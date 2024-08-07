/**
 * Copyright (c) 2024 Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

"use strict";

/**
 * @typedef {Object} AndroidInterface
 * @property {(colorName: string) => string} colorHex
 * @property {(
 *      amount: number, 
 *      language: string, 
 *      country: string, 
 *      symbol: string) => string} formatCurrencyWithUnit
 */

/**
 * @type {AndroidInterface}
 */
// @ts-ignore
export let Android = window.Android;
