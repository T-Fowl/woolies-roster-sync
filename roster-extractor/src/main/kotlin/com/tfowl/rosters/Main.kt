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
import kotlin.math.abs
import kotlin.math.roundToInt

fun Double.roundToEighth(): Double = (this * 8.0).roundToInt() / 8.0

// Unsafe but convenient
fun MatchGroupCollection.getValue(name: String): String = get(name)!!.value

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

data class EnclosedArea(val lowerX: Double, val upperX: Double,
                        val lowerY: Double, val upperY: Double) {

    val centerX = 0.5 * (lowerX + upperX)
    val centerY = 0.5 * (lowerY + upperY)
}

fun enclosedAreas(detection: IntersectionDetectorResults): Set<EnclosedArea> {
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

private fun VisualDebugger.visualiseDetection(detection: IntersectionDetectorResults) {
    visualiseEach(detection.lines) { line ->
        graphics.draw(Color.BLACK, Line2D.Double(line.start, line.end))
    }

    visualiseEach(detection.intersections) { intersection ->
        graphics.draw(Color.RED, CenteredEllipse(intersection.midpoint.x, intersection.midpoint.y, 4.0))
    }

    visualiseEach(detection.horizontalGridLines) {
        graphics.draw(Color.PINK, Line2D.Double(0.0, it, detection.page.cropBox.width.toDouble(), it))
    }
    visualiseEach(detection.verticalGridLines) {
        graphics.draw(Color.PINK, Line2D.Double(it, 0.0, it, detection.page.cropBox.height.toDouble()))
    }
}

private fun obtainTable(page: PDPage, debugger: VisualDebugger? = null): Table {
    val detection = IntersectionDetector()
            .detect(page, tolerance = 0.275, combineTolerance = 0.275, alignTolerance = 2.0)

    debugger?.visualiseDetection(detection)

    val areas = enclosedAreas(detection)

    debugger?.visualiseEach(areas) { area ->
        graphics.draw(Color.PINK, CenteredEllipse(area.centerX, area.centerY, 5.0))
    }


    return TableExtractor().extract(page, areas, detection)
}

data class LocalDatePeriod(val start: LocalDate, val end: LocalDate)

data class RosterSection(
        val siteCode: Int,
        val siteName: String,
        val department: String,
        val timePeriod: LocalDatePeriod,
        val executedOn: LocalDate
)

data class Employee(val name: String)

data class ShiftTimes(val from: LocalTime, val to: LocalTime)

data class Shift(val date: LocalDate, val period: ShiftTimes?, val department: String, val text: String)

fun main() {
    val document = PDDocument.load(File("3216_week_0.pdf"))

    val localDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val localDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy h.mm a")

    val employees = mutableSetOf<Employee>()
    val employeesToDepartments = mutableMapOf<Employee, MutableSet<String>>()
    val employeesDepartmentsToJobs = mutableMapOf<Pair<Employee, String>, MutableSet<String>>()
    val employeesDepartmentJobsToShifts = mutableMapOf<Triple<Employee, String, String>, MutableSet<Shift>>()

    var section: RosterSection? = null
    for (pageIndex in 0 until document.numberOfPages) {
        val page = document.getPage(pageIndex)
        val table = obtainTable(page, null)

        if ("Location Schedule" in table[0, 0].cell.content) {
            val header = table[0, 0].cell.content

            val regex = Regex("""Location Schedule - (?<siteid>\d+) (?<sitename>.+?) - (?<department>.+?) Time Period\s?:\s?(?<from>\d+/\d+/\d+) - (?<to>\d+/\d+/\d+) Executed on: (?<executed>\d+/\d+/\d+\s*\d+\.\d+\s*[AP]M)""")
            val match = regex.find(header) ?: error("Unmatched header")


            section = with(match.groups) {
                RosterSection(getValue("siteid").toInt(),
                              getValue("sitename"),
                              getValue("department"),
                              LocalDatePeriod(LocalDate.parse(getValue("from"), localDateFormatter),
                                              LocalDate.parse(getValue("to"), localDateFormatter)),
                              LocalDate.parse(getValue("executed"), localDateTimeFormatter))
            }
        }

        val datesColumns = DayOfWeek.values().map { day ->
            val dayOfWeekCell = table.positionedCells.first { it.cell.content.contains(day.name, ignoreCase = true) }
            val dateContent = table[dayOfWeekCell.rowIndex + 1, dayOfWeekCell.columnIndex].cell.content
            LocalDate.parse(dateContent, localDateFormatter) to dayOfWeekCell.columnIndex
        }.toMap().toSortedMap()

        val employeeHeader = table.positionedCells.first { "Employee" in it.cell.content }
        val jobHeader = table.positionedCells.first { "Job" in it.cell.content }
        for (row in (employeeHeader.rowIndex + 1) until table.rowCount) {
            val name = table[row, employeeHeader.columnIndex].cell.content
            val job = table[row, jobHeader.columnIndex].cell.content
            val department = section!!.department

            val employee = Employee(name).also { employees.add(it) }
            employeesToDepartments.computeIfAbsent(employee) { mutableSetOf() }.add(department)
            employeesDepartmentsToJobs.computeIfAbsent(employee to department) { mutableSetOf() }.add(job)


            for ((date, columnIndex) in datesColumns) {
                val rosterText = table[row, columnIndex].cell.content
                if (rosterText.isNotBlank()) {
                    val times = Regex("""(?<from>\d+:\d+[AP]) - (?<to>\d+:\d+[AP])""").find(rosterText)?.run {
                        val formatter = DateTimeFormatter.ofPattern("h:mma")
                        ShiftTimes(LocalTime.parse(groups.getValue("from") + "M", formatter),
                                   LocalTime.parse(groups.getValue("to") + "M", formatter))
                    }

                    employeesDepartmentJobsToShifts.computeIfAbsent(Triple(employee, department, job)) { mutableSetOf() }
                            .add(Shift(date, times, department, rosterText))
                }
            }
        }
    }

    println("Employees:")
    employees.forEach { employee ->
        println(employee)
        employeesToDepartments[employee]?.forEach { department ->
            println("\t$department")

            employeesDepartmentsToJobs[employee to department]?.forEach { job ->
                println("\t\t$job")

                employeesDepartmentJobsToShifts[Triple(employee, department, job)]?.forEach { shift ->
                    println("\t\t\t$shift")
                }
            }
        }
    }
}
