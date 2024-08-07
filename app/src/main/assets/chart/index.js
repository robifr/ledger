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

import { ChartLayout, renderBarChart, renderStackedBarChart } from "./chart.js";
import { createLinearScale, createPercentageLinearScale, createBandScale } from "./scale.js";

// Web view only works in global scope.
// @ts-ignore
window.chart = {
  ChartLayout,
  renderBarChart,
  renderStackedBarChart,
  createLinearScale,
  createPercentageLinearScale,
  createBandScale,
};
