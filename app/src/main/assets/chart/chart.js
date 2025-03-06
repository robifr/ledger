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
 * @param {string} color
 */
export function renderBarChart(layout, xScale, yScale, data, color) {
  d3.select("#container").select("svg").remove();
  const svg = d3.create("svg").style("width", layout.width).style("height", layout.height);
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
    .style("font-size", `${layout.fontSize}`)
    .attr("transform", `translate(${layout.marginLeft}, 0)`)
    .call(yScale.axis)
    .call((g) => {
      g.select(".domain").remove(); // Remove y-axis line.
      g.selectAll("line").style("stroke", Android.colorHex("stroke")).style("stroke-width", 0.5);
      g.selectAll("text").style("fill", Android.colorHex("android:textColor"));
    });

  _drawBarChart(svg, layout, xScale, yScale, data, color);

  // Draw x-axis. Ensure that the x-axis is rendered after the bars to prevent overlapping.
  svg
    .append("g")
    .style("font-size", `${layout.fontSize}`)
    .attr("transform", `translate(0, ${layout.height - layout.marginBottom})`)
    .call(xScale.axis)
    .call((g) => {
      g.select(".domain").style("stroke", Android.colorHex("stroke"));
      g.selectAll("line").style("stroke", Android.colorHex("stroke"));
      g.selectAll("text").style("fill", Android.colorHex("android:textColor"));
    });
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
  const svg = d3.create("svg").style("width", layout.width).style("height", layout.height);
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
    .style("font-size", `${layout.fontSize}`)
    .attr("transform", `translate(${layout.marginLeft}, 0)`)
    .call(yScale.axis)
    .call((g) => {
      g.select(".domain").remove(); // Remove y-axis line.
      g.selectAll("line").style("stroke", Android.colorHex("stroke")).style("stroke-width", 0.5);
      g.selectAll("text").style("fill", Android.colorHex("android:textColor"));
    });

  const groupedData = new Map();
  // Group the data so that we can render them in order.
  for (const d of data) {
    if (!groupedData.has(d.group)) groupedData.set(d.group, []);
    groupedData.get(d.group).push(d);
  }
  for (const [i, group] of groupInOrder.entries()) {
    // Skip the group to be drawn at the end if the group doesn't contain any data.
    // If it's not defined in `groupInOrder`, don't draw them completely.
    if (!groupedData.has(group)) continue;
    // Draw the group in order.
    _drawBarChart(svg, layout, xScale, yScale, groupedData.get(group), colors[i]);
  }

  // Draw x-axis. Ensure that the x-axis is rendered after the bars to prevent overlapping.
  svg
    .append("g")
    .style("font-size", `${layout.fontSize}`)
    .attr("transform", `translate(0, ${layout.height - layout.marginBottom})`)
    .call(xScale.axis)
    .call((g) => {
      g.select(".domain").style("stroke", Android.colorHex("stroke"));
      g.selectAll("line").style("stroke", Android.colorHex("stroke"));
      g.selectAll("text").style("fill", Android.colorHex("android:textColor"));
    });
}

/**
 * @param {ChartLayout} layout
 * @param {singleChartData[]} data
 * @param {string[]} colors
 * @param {string} [svgTextInCenter]
 */
export function renderDonutChart(layout, data, colors, svgTextInCenter = "") {
  d3.select("#container").select("svg").remove();
  const svg = d3.create("svg").style("width", layout.width).style("height", layout.height);
  d3.select("#container").append(() => svg.node());
  d3.select("body").style("background-color", layout.backgroundColor);
  _drawDonutChart(svg, layout, data, colors, svgTextInCenter);
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
  return { widest: widest, highest: highest };
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
  svg
    .selectAll(".bar")
    .data(data)
    .enter()
    .append("path")
    .style("fill", color)
    .style("width", xScale.scale.bandwidth())
    .style("height", (d) => layout.height - layout.marginBottom - yScale.scale(d.value))
    .attr("x", (d) => xScale.scale(d.key) ?? null)
    .attr("y", (d) => yScale.scale(d.value))
    .attr("d", (d) => {
      if (d.value === 0) {
        // Draw flat rectangle for zero values to avoid unintended corners below the X-axis.
        return `
          M${xScale.scale(d.key)},${yScale.scale(d.value)}
          h${xScale.scale.bandwidth()}
          v0
          h${-xScale.scale.bandwidth()}Z
        `;
      } else {
        // Draw rounded rectangle.
        return `
          M${xScale.scale(d.key)},${yScale.scale(d.value) + barCornerRadius}
          a${barCornerRadius},${barCornerRadius} 0 0 1 ${barCornerRadius},${-barCornerRadius}
          h${xScale.scale.bandwidth() - 2 * barCornerRadius}
          a${barCornerRadius},${barCornerRadius} 0 0 1 ${barCornerRadius},${barCornerRadius}
          v${layout.height - layout.marginBottom - yScale.scale(d.value) - barCornerRadius}
          h${-xScale.scale.bandwidth()}Z
        `;
      }
    });
}

/**
 * @param {d3.select<SVGElement>} svg
 * @param {ChartLayout} layout
 * @param {singleChartData[]} data
 * @param {string[]} colors
 * @param {string} [svgTextInCenter]
 */
function _drawDonutChart(svg, layout, data, colors, svgTextInCenter = "") {
  const usableWidth = layout.width - layout.marginLeft - layout.marginRight;
  const usableHeight = layout.height - layout.marginTop - layout.marginBottom;
  // The maximum chart width is 2/3 of the layout, while the remaining 1/3 is used by the legend.
  const radius = Math.min((usableWidth * 2) / 3, usableHeight) / 2;
  // This will place the chart in the center of the layout,
  // slightly moved to the left for the legend.
  const chartXPosition = layout.marginLeft + usableWidth / 2 - radius / 2;
  const chartYPosition = layout.marginTop + radius;

  // A placeholder key, used when the total data value is zero. It ensures
  // the donut chart displays a gray color instead of being completely invisible.
  // This placeholder should be excluded from the chart legends.
  const NO_DATA_KEY = "_noData";
  // Add a placeholder if the sum of data values is zero.
  if (data.reduce((acc, d) => acc + d.value, 0) === 0) {
    data.push({ key: NO_DATA_KEY, value: 100 });
    colors.push(Android.colorHex("secondary_disabled"));
  }

  const pie = d3
    .pie()
    .sort(null)
    .value((d) => d.value);
  svg
    .append("g")
    .attr("transform", `translate(${chartXPosition}, ${chartYPosition})`)
    .call((g) => {
      g.selectAll(".slice")
        .data(pie(data))
        .join("path")
        .style("fill", (d, i) => colors[i])
        .style("stroke", layout.backgroundColor)
        .style("stroke-width", (d) => (d.data.value === 0 ? "0px" : "1px"))
        .attr(
          "d",
          d3
            .arc()
            .innerRadius(radius * 0.7)
            .outerRadius(radius)
        );
      g.append("g")
        .style("fill", Android.colorHex("android:textColor"))
        .style("text-anchor", "middle")
        .style("dominant-baseline", "middle")
        .html(svgTextInCenter);
    });

  const legendRectSize = 15;
  const legendItemPadding = 5;
  svg
    .selectAll(".legend")
    .data(pie(data))
    .enter()
    .append("g")
    .call((g) => {
      g.append("rect")
        .style("width", legendRectSize)
        .style("height", legendRectSize)
        .style("fill", (d, i) => (d.data.key !== NO_DATA_KEY ? colors[i] : "none"))
        .attr("rx", 5)
        .attr("ry", 5);
      g.append("text")
        .text((d) => (d.data.key !== NO_DATA_KEY ? d.data.key : ""))
        .style("fill", Android.colorHex("android:textColor"))
        .style("font-size", 12)
        .attr("x", legendRectSize + legendItemPadding)
        .attr("y", legendRectSize / 2)
        .attr("dy", "0.35em"); // Adjust for vertical alignment of text baseline.
      g.append("text")
        .text((d) =>
          d.data.key !== NO_DATA_KEY
            ? Android.formatCurrencyWithUnit(d.value, Android.localeLanguageTag(), "")
            : ""
        )
        .style("fill", Android.colorHex("android:textColor"))
        .style("font-size", 14)
        .style("font-weight", "bold")
        .attr("x", legendRectSize + legendItemPadding)
        .attr("y", legendRectSize / 2 + 12 + legendItemPadding)
        .attr("dy", "0.35em"); // Adjust for vertical alignment of text baseline.

      const legendXPosition = chartXPosition + radius + 20;
      let legendYPosition = layout.marginTop + 20;
      // Dynamically set the legend y-position depending on how much space is occupied by the text.
      g.each(function () {
        d3.select(this).attr("transform", `translate(${legendXPosition}, ${legendYPosition})`);
        legendYPosition += this.getBBox().height + 15;
      });
    });
}
