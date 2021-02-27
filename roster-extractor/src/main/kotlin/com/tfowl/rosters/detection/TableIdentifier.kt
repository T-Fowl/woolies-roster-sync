package com.tfowl.rosters.detection

import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.Table
import com.jakewharton.picnic.table
import com.tfowl.rosters.CenteredEllipse
import com.tfowl.rosters.VisualDebugger
import com.tfowl.rosters.draw
import com.tfowl.rosters.visualiseEach
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripperByArea
import java.awt.Color
import java.awt.geom.Rectangle2D
import kotlin.math.abs

internal data class EnclosedArea(
    val lowerX: Double, val upperX: Double,
    val lowerY: Double, val upperY: Double,
) {

    val centerX = 0.5 * (lowerX + upperX)
    val centerY = 0.5 * (lowerY + upperY)
}

internal fun enclosedAreas(detection: IntersectionDetectorResults): Set<EnclosedArea> {
    val areas = mutableSetOf<EnclosedArea>()

    val intersections = detection.intersections
    for (intersection in intersections) {
        val point = intersection.midpoint
        val sameX = intersections.filter { abs(it.midpoint.x - point.x) <= 0.3 && it.midpoint !== point }
            .sortedBy { it.midpoint.y }
        val sameY = intersections.filter { abs(it.midpoint.y - point.y) <= 0.3 && it.midpoint !== point }
            .sortedBy { it.midpoint.x }

        val nextX = sameY.firstOrNull { it.midpoint.x > point.x }?.midpoint?.x ?: continue
        val nextY = sameX.firstOrNull { it.midpoint.y > point.y }?.midpoint?.y ?: continue

        areas.add(EnclosedArea(point.x, nextX, point.y, nextY))
    }

    return areas
}

data class CellLocation(
    val row: Int, val rowSpan: Int,
    val column: Int, val columnSpan: Int,
)

class TableIdentifier {
    fun identify(page: PDPage, detection: IntersectionDetectorResults, debugger: VisualDebugger): Table {
        val areas = enclosedAreas(detection)

        debugger.visualiseEach("grid-areas", areas) { area ->
            draw(Color.PINK, CenteredEllipse(area.centerX, area.centerY, 5.0))
        }

        val cellLocations = mutableSetOf<CellLocation>()
        for (area in areas) {
            val columnStart = detection.verticalGridLines.indexOfLast { it <= area.lowerX }
            val columnEnd = detection.verticalGridLines.indexOfFirst { it >= area.upperX }
            val columnSpan = columnEnd - columnStart

            val rowStart = detection.horizontalGridLines.indexOfLast { it <= area.lowerY }
            val rowEnd = detection.horizontalGridLines.indexOfFirst { it >= area.upperY }
            val rowSpan = rowEnd - rowStart

            cellLocations.add(CellLocation(columnStart, columnSpan, rowStart, rowSpan))
        }

        val stripperByArea = PDFTextStripperByArea()
        val hzList = detection.horizontalGridLines.toList()
        val vtList = detection.verticalGridLines.toList()
        for (location in cellLocations) {
            var x = hzList[location.column]
            val width = hzList[location.column + location.columnSpan] - x

            val y = vtList[location.row]
            val height = vtList[location.row + location.rowSpan] - y

            // TODO: Special case for "Employee | Job" column headers which are not aligned properly in pdf
            if (location.row in 0..2 && location.column in 0..1) {
                x -= 1
            }

            stripperByArea.addRegion("${location.hashCode()}", Rectangle2D.Double(x, y, width, height))
        }
        stripperByArea.extractRegions(page)

        val sortedCellLocations = cellLocations.groupBy { it.row }
            .mapValues { (_, cells) -> cells.sortedBy { it.column } }
            .toSortedMap()


        return table {
            style {
                borderStyle = BorderStyle.Solid
                border = true
            }

            cellStyle {
                border = true
            }

            sortedCellLocations.forEach { (_, rowCells) ->
                row {
                    rowCells.forEach { cell ->
                        val content = stripperByArea.getTextForRegion("${cell.hashCode()}")
                            .replace(Regex("""\s+"""), " ")
                            .trim()
                        cell(content) {
                            rowSpan = cell.rowSpan
                            columnSpan = cell.columnSpan
                        }
                    }
                }
            }
        }
    }
}