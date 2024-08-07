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

import * as d3 from "../libs/d3.js";
import { Android } from "./typedefs.js";

/**
 * @typedef {ReturnType<typeof import("./scale.js").createLinearScale>} linearScale
 * @typedef {ReturnType<typeof import("./scale.js").createPercentageLinearScale>} percentageLinearScale
 * @typedef {ReturnType<typeof import("./scale.js").createBandScale>} bandScale
 * @typedef {{ key: string, value: number }} singleChartData
 * @typedef {{ key: string, value: number, group: string }} multipleChartData
 */

export class ChartLayout {
  /**
   * @param {number} width
   * @param {number} height
   * @param {number} marginTop
   * @param {number} marginBottom
   * @param {number} marginLeft
   * @param {number} marginRight
   * @param {number} fontSize
   * @param {string} backgroundColor
   */
  constructor(
    width,
    height,
    marginTop,
    marginBottom,
    marginLeft,
    marginRight,
    fontSize,
    backgroundColor
  ) {
    /** @type {number} */
    this.width = width;
    // There's a bug where a vertical scroll will appear no matter what. Subtracting by
    // the font width is the most reliable solution for every user with different font sizes.
    /** @type {number} */
    this.height = height - fontSize;

    // Extra margin for the ticks.
    /** @type {number} */
    this.marginTop = marginTop + 10;
    /** @type {number} */
    this.marginBottom = marginBottom + 10;
    /** @type {number} */
    this.marginLeft = marginLeft + 10;
    /** @type {number} */
    this.marginRight = marginRight + 10;
    /** @type {number} */
    this.fontSize = fontSize;
    /** @type {string} */
    this.backgroundColor = backgroundColor;

    Object.seal(this);
  }
}

/**
 * @param {ChartLayout} layout
 * @param {bandScale} xScale
 * @param {linearScale | percentageLinearScale} yScale
 * @param {singleChartData[]} data
 */
export function renderBarChart(layout, xScale, yScale, data, color) {
  d3.select("#container").select("svg").remove();

  const svg = d3.create("svg").attr("width", layout.width).attr("height", layout.height);

  d3.select("#container").append(() => svg.node());
  d3.select("body").style("background-color", layout.backgroundColor);

  layout.marginBottom = layout.marginBottom + _measureSpaceForTicksLabel(svg, xScale.axis).highest;
  layout.marginLeft = layout.marginLeft + _measureSpaceForTicksLabel(svg, yScale.axis).widest;
  xScale.scale.range([xScale.axisPosition.minRange(layout), xScale.axisPosition.maxRange(layout)]);
  yScale.scale.range([yScale.axisPosition.minRange(layout), yScale.axisPosition.maxRange(layout)]);
  yScale.axis.tickSize(-layout.width + layout.marginRight + layout.marginLeft);

  // Draw y-axis.
  svg
    .append("g")
    .attr("transform", `translate(${layout.marginLeft}, 0)`)
    .style("font-size", `${layout.fontSize}`)
    .call(yScale.axis)
    .call((g) => g.select(".domain").remove()) // Remove y-axis line.
    .selectAll("line")
    .style("stroke", Android.colorHex("stroke"))
    .attr("stroke-width", 0.3);

  _drawBarChart(svg, layout, xScale, yScale, data, color);

  // Draw x-axis. Ensure that the x-axis is rendered after the bars to prevent overlapping.
  svg
    .append("g")
    .attr("transform", `translate(0, ${layout.height - layout.marginBottom})`)
    .style("font-size", `${layout.fontSize}`)
    .call(xScale.axis);
}

/**
 * @param {ChartLayout} layout
 * @param {bandScale} xScale
 * @param {linearScale | percentageLinearScale} yScale
 * @param {multipleChartData[]} data
 * @param {string[]} colors
 * @param {string[]} groupInOrder
 * @throws {RangeError}
 */
export function renderStackedBarChart(layout, xScale, yScale, data, colors, groupInOrder) {
  if (groupInOrder.length !== colors.length) {
    throw new RangeError(`The sizes of 'colors' and 'groupInOrder' have to be equals`);
  }

  d3.select("#container").select("svg").remove();

  const svg = d3.create("svg").attr("width", layout.width).attr("height", layout.height);

  d3.select("#container").append(() => svg.node());
  d3.select("body").style("background-color", layout.backgroundColor);

  layout.marginBottom = layout.marginBottom + _measureSpaceForTicksLabel(svg, xScale.axis).highest;
  layout.marginLeft = layout.marginLeft + _measureSpaceForTicksLabel(svg, yScale.axis).widest;
  xScale.scale.range([xScale.axisPosition.minRange(layout), xScale.axisPosition.maxRange(layout)]);
  yScale.scale.range([yScale.axisPosition.minRange(layout), yScale.axisPosition.maxRange(layout)]);
  yScale.axis.tickSize(-layout.width + layout.marginRight + layout.marginLeft);

  const groupedData = new Map();

  // Group the data so that we can render them in order.
  for (const d of data) {
    if (!groupedData.has(d.group)) groupedData.set(d.group, []);
    groupedData.get(d.group).push(d);
  }

  // Draw y-axis.
  svg
    .append("g")
    .attr("transform", `translate(${layout.marginLeft}, 0)`)
    .style("font-size", `${layout.fontSize}`)
    .call(yScale.axis)
    .call((g) => g.select(".domain").remove()) // Remove y-axis line.
    .selectAll("line")
    .style("stroke", Android.colorHex("stroke"))
    .attr("stroke-width", 0.3);

  for (let i = 0; i < groupInOrder.length; i++) {
    // Skip the group to be drawn at the end if the group doesn't contain any data.
    // If it's not defined in `groupInOrder`, don't draw them completely.
    if (!groupedData.has(groupInOrder[i])) continue;

    // Draw the group in order.
    _drawBarChart(svg, layout, xScale, yScale, groupedData.get(groupInOrder[i]), colors[i]);
  }

  // Draw x-axis. Ensure that the x-axis is rendered after the bars to prevent overlapping.
  svg
    .append("g")
    .attr("transform", `translate(0, ${layout.height - layout.marginBottom})`)
    .style("font-size", `${layout.fontSize}`)
    .call(xScale.axis);
}

/**
 * This function works correctly only if called after the SVG is rendered,
 * as `getBBox()` requires the SVG to be drawn first.
 * @param {d3.select<SVGElement>} svg
 * @param {d3.axisBottom | d3.axisLeft} axis
 * @returns {{ widest: number, highest: number }}
 */
function _measureSpaceForTicksLabel(svg, axis) {
  const tempGroup = svg.append("g").call(axis);
  const textNodes = tempGroup.selectAll("text").nodes();
  const widest = d3.max(textNodes.map((node) => node.getBBox().width));
  const highest = d3.max(textNodes.map((node) => node.getBBox().height));

  tempGroup.remove();
  return { widest, highest };
}

/**
 * @param {d3.select<SVGElement>} svg
 * @param {ChartLayout} layout
 * @param {bandScale} xScale
 * @param {linearScale | percentageLinearScale} yScale
 * @param {singleChartData[] | multipleChartData[]} data
 * @param {string} color
 */
function _drawBarChart(svg, layout, xScale, yScale, data, color) {
  // Set the corner radius to 20% of the bar width.
  const barCornerRadius = Math.min(5, Math.max(2, xScale.scale.bandwidth() * 0.2));

  // Draw bar with rounded corners.
  svg
    .selectAll(".bar-top")
    .data(data)
    .enter()
    .append("rect")
    .style("fill", color)
    .attr("x", (d) => xScale.scale(d.key) ?? null)
    .attr("y", (d) => yScale.scale(d.value))
    .attr("width", xScale.scale.bandwidth())
    .attr("height", (d) => layout.height - layout.marginBottom - yScale.scale(d.value))
    .attr("rx", barCornerRadius)
    .attr("ry", barCornerRadius);
  // Draw bottom bar to cover the bottom corners.
  svg
    .selectAll(".bar-bottom")
    .data(data)
    .enter()
    .append("rect")
    .style("fill", color)
    .attr("x", (d) => xScale.scale(d.key) ?? null)
    .attr(
      "y",
      (d) => yScale.scale(d.value) + (yScale.scale(0) - yScale.scale(d.value) - barCornerRadius)
    )
    .attr("width", xScale.scale.bandwidth())
    .attr("height", (d) =>
      layout.height - layout.marginBottom - yScale.scale(d.value) > barCornerRadius
        ? barCornerRadius
        : 0
    );
}
