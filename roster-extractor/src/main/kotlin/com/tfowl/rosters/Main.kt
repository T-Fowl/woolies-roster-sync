package com.tfowl.rosters

import com.jakewharton.picnic.Table
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import java.io.File

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

private fun obtainTable(page: PDPage, debugger: VisualDebugger): Table {
    val detection = CombinatorialIntersectionDetector()
        .detect(
            page,
            detectionTolerance = 0.275,
            combineTolerance = 0.275,
            alignmentTolerance = 2.0,
            debugger
        )

    return TableExtractor().extract(page, detection, debugger)
}

fun main(vararg args: String) {
    require(args.isNotEmpty()) { "Usage: [exec] roster-file" }

    val document = PDDocument.load(File(args[0]))

    val tables = document.pages.map { page ->
        val visualiser = NoOpVisualDebugger()
        obtainTable(page, visualiser)
    }
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
