package com.tfowl.rosters

import com.jakewharton.picnic.Table
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import java.awt.Color
import java.awt.geom.Line2D
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/*

Grid Detection Algorithm:

1) Extract all lines
2) Identify intersection points
3) Align intersection points along horizontals and verticals
4) Define grid areas as rectangles bounded by 4 intersection points
5) Define a cell as a grid area with some (row,col) position and (rowSpan,colSpan) extents
6) Assign each text element into its grid area / cell reference
7) Profit

Things to consider:
1) Multiple tables? [Not applicable in this domain]
    Essentially boils down to finding sub-graphs of lines & intersections

 */

private fun VisualDebugger.visualiseDetection(detection: IntersectionDetectorResults) {
    visualiseEach("intersection-detection", detection.lines) { line ->
        draw(Color.BLACK, Line2D.Double(line.start, line.end))
    }

    visualiseEach("intersection-detection", detection.intersections) { intersection ->
        draw(Color.RED, CenteredEllipse(intersection.midpoint.x, intersection.midpoint.y, 4.0))
    }

    visualiseEach("intersection-detection", detection.horizontalGridLines) {
        draw(Color.PINK, Line2D.Double(0.0, it, detection.page.cropBox.width.toDouble(), it))
    }

    visualiseEach("intersection-detection", detection.verticalGridLines) {
        draw(Color.PINK, Line2D.Double(it, 0.0, it, detection.page.cropBox.height.toDouble()))
    }
}

private fun obtainTable(page: PDPage, debugger: VisualDebugger): Table {
    val detection = CombinatorialIntersectionDetector()
        .detect(page, detectionTolerance = 0.275, combineTolerance = 0.275, alignmentTolerance = 2.0)

    debugger.visualiseDetection(detection)

//    debugger.visualiseEach("grid-areas", detection.aread) { area ->
//        graphics.draw(Color.PINK, CenteredEllipse(area.centerX, area.centerY, 5.0))
//    }

    return TableExtractor().extract(page, detection)
}

fun main(vararg args: String) {
    require(args.isNotEmpty()) { "Usage: [exec] roster-file" }

    val document = PDDocument.load(File(args[0]))
    val visualiser: VisualDebugger = NoOpVisualDebugger()


    val tables = document.pages.map { obtainTable(it, visualiser) }
    val rosters = tables.extractDepartmentRosters()

    rosters.forEach { department ->
        println(department.department)

        department.employees.forEach { employee ->
            println("\t${employee.name}")

            employee.jobs.forEach { job ->
                println("\t\t${job.title}")

                job.shifts.forEach { shift ->
                    println("\t\t\t$shift")
                }
            }
        }
    }
}
