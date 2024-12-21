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

// @ts-ignore
import { Android, d3 } from "libs";

/**
 * @typedef {import("./chart.js").ChartLayout} ChartLayout
 */

/**
 * @typedef {Object} AxisPosition
 * @property {number} value
 * @property {(scale: d3.scaleLinear | d3.scaleBand) => d3.axisBottom | d3.axisLeft} axis
 * @property {(layout: ChartLayout) => number} minRange
 * @property {(layout: ChartLayout) => number} maxRange
 */

/**
 * @type {Readonly<{BOTTOM: AxisPosition, LEFT: AxisPosition}>}
 */
export const AxisPosition = Object.freeze({
  BOTTOM: {
    value: 1,
    axis: (scale) => d3.axisBottom(scale),
    minRange: (layout) => layout.marginLeft,
    maxRange: (layout) => layout.width - layout.marginRight,
  },
  LEFT: {
    value: 2,
    axis: (scale) => d3.axisLeft(scale),
    minRange: (layout) => layout.height - layout.marginBottom,
    maxRange: (layout) => layout.marginTop,
  },
});

/**
 * @param {number} axisPosition
 * @param {number[]} domain
 * @returns {{scale: d3.scaleLinear, axis: d3.axisBottom | d3.axisLeft, axisPosition: AxisPosition}}
 */
export function createLinearScale(axisPosition, domain) {
  domain = d3.extent(domain);
  const ticks = 6;
  const tickStep = (domain[1] - domain[0]) / (ticks - 1);

  const axisPos = _axisPositionOf(axisPosition);
  const scale = d3.scaleLinear().domain(domain);
  const axis = axisPos
    .axis(scale)
    .tickValues(d3.range(domain[0], domain[1] + tickStep, tickStep))
    .tickSizeOuter(0)
    .tickFormat((d) => Android.formatCurrencyWithUnit(d, Android.localeLanguageTag(), ""));
  return { scale: scale, axis: axis, axisPosition: axisPos };
}

/**
 * Same as a linear scale but with support for larger numbers by using percentages.
 * Each domain (0-100) will be replaced with the provided domain strings.
 * @param {number} axisPosition
 * @param {string[]} domain
 * @returns {{scale: d3.scaleLinear, axis: d3.axisBottom | d3.axisLeft, axisPosition: AxisPosition}}
 */
export function createPercentageLinearScale(axisPosition, domain) {
  if (domain.length !== 101) throw new Error("Domain size should contain 101 items");
  const axisPos = _axisPositionOf(axisPosition);
  const scale = d3.scaleLinear().domain([0, 100]);
  const axis = axisPos
    .axis(scale)
    .ticks(6)
    .tickSizeOuter(0)
    .tickFormat((d, i) => {
      if (Math.floor(d) !== d) return ""; // Hide label for decimal numbers.
      return domain[i * 2 * (scale.ticks().length - 1)];
    });
  return { scale: scale, axis: axis, axisPosition: axisPos };
}

/**
 * @param {number} axisPosition
 * @param {string[]} domain
 * @param {boolean} [isAllLabelVisible=true]
 * @returns {{scale: d3.scaleBand, axis: d3.axisBottom | d3.axisLeft, axisPosition: AxisPosition}}
 */
export function createBandScale(axisPosition, domain, isAllLabelVisible = true) {
  const axisPos = _axisPositionOf(axisPosition);
  const scale = d3.scaleBand().domain(domain).padding(0.4);
  const axis = axisPos
    .axis(scale)
    .tickSizeOuter(0)
    // Hide some label to improve readability.
    .tickFormat((d, i) =>
      !isAllLabelVisible && i % _gapBetweenTicks(domain.length) !== 0 ? null : d
    );
  return { scale: scale, axis: axis, axisPosition: axisPos };
}

/**
 * @param {number} totalTicks
 * @returns {number}
 */
function _gapBetweenTicks(totalTicks) {
  return Math.max(1, Math.ceil(totalTicks / 8));
}

/**
 * @param {number} value
 * @returns {AxisPosition}
 * @throws {TypeError}
 */
function _axisPositionOf(value) {
  const axisPosition = Object.values(AxisPosition).find((pos) => pos.value === value) ?? null;
  if (!axisPosition) throw new TypeError(`Invalid AxisPosition value: ${value}`);
  return axisPosition;
}
