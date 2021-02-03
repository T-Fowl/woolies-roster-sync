@file:Suppress("unused")

package com.tfowl.rosters

import com.jakewharton.picnic.Table
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

data class DepartmentInfoHeader(
    val siteCode: Int,
    val siteName: String,
    val name: String,
    val timePeriod: LocalDateRange,
    val executedOn: LocalDate,
)

data class Shift(
    val date: LocalDate,
    // TODO: LocalDateTimeRange to account for night fill etc?
    val period: LocalTimeRange?,
    val department: String,
    val text: String,
)

data class DepartmentJob(
    val title: String,
    val shifts: List<Shift>,
)

data class DepartmentEmployee(
    val name: String,
    val jobs: List<DepartmentJob>,
)

data class DepartmentRoster(
    val department: DepartmentInfoHeader,
    val employees: List<DepartmentEmployee>,
)


internal data class JobBuilder(
    val title: String,
    val shifts: MutableList<Shift> = mutableListOf(),
) {
    fun build(): DepartmentJob = DepartmentJob(title, shifts.toList())
}

internal data class EmployeeBuilder(
    val name: String,
    val jobs: MutableList<JobBuilder> = mutableListOf(),
) {
    fun build(): DepartmentEmployee = DepartmentEmployee(name, jobs.map { it.build() })
}

internal data class DepartmentBuilder(
    val info: DepartmentInfoHeader,
    val employees: MutableList<EmployeeBuilder> = mutableListOf(),
) {
    fun build(): DepartmentRoster = DepartmentRoster(info, employees.map { it.build() })
}

private fun String.tryParseDepartmentHeader(): DepartmentInfoHeader? {
    val localDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val localDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy h.mm a")

    val regex =
        Regex("""Location Schedule - (?<siteid>\d+) (?<sitename>.+?) - (?<department>.+?) Time Period\s?:\s?(?<from>\d+/\d+/\d+) - (?<to>\d+/\d+/\d+) Executed on: (?<executed>\d+/\d+/\d+\s*\d+\.\d+\s*[AP]M)""")
    val match = regex.find(this) ?: return null

    return with(match.groups) {
        DepartmentInfoHeader(
            getValue("siteid").toInt(),
            getValue("sitename"),
            getValue("department"),
            LocalDateRange(
                getValue("from").toLocalDate(localDateFormatter),
                getValue("to").toLocalDate(localDateFormatter)
            ),
            getValue("executed").toLocalDate(localDateTimeFormatter)
        )
    }
}

private fun Table.tryFindDateColumns(): SortedMap<LocalDate, Int>? {
    val localDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    return DayOfWeek.values().map { day ->
        val dayOfWeekCell = firstCellOrNull { it.content.contains(day.name, ignoreCase = true) } ?: return null
        val dateContent = get(origin = dayOfWeekCell, rowOffset = 1).content

        dateContent.toLocalDate(localDateFormatter) to dayOfWeekCell.columnIndex
    }.toMap().toSortedMap()
}

class TableRostersExtractor {
    private val departmentBuilders = mutableListOf<DepartmentBuilder>()
    private var currentDepartment: DepartmentBuilder? = null

    private fun nextDepartment(header: DepartmentInfoHeader) {
        currentDepartment = DepartmentBuilder(header).also { departmentBuilders.add(it) }
    }

    fun extract(tables: List<Table>): Set<DepartmentRoster> {
        for (table in tables) {
            if ("Location Schedule" in table[0, 0].content) {
                val header = table[0, 0].content.tryParseDepartmentHeader() ?: error("Unmatched header")
                nextDepartment(header)
            }

            val dateToColumn = table.tryFindDateColumns() ?: error("Cannot find week days and dates")

            val employeeHeader =
                table.firstCellOrNull { "Employee" in it.content } ?: error("Cannot find Employee header")
            val jobHeader = table.firstCellOrNull { "Job" in it.content } ?: error("Cannot find Job header")

            requireNotNull(currentDepartment) { "No department found for employees" }
            for (row in (employeeHeader.rowIndex + 1) until table.rowCount) {
                val name = table[row, employeeHeader.columnIndex].content
                val jobTitle = table[row, jobHeader.columnIndex].content
                val department = currentDepartment!!.info.name

                val employee = EmployeeBuilder(name).also { currentDepartment!!.employees.add(it) }
                val job = JobBuilder(jobTitle).also { employee.jobs.add(it) }

                for ((date, columnIndex) in dateToColumn) {
                    val rosterText = table[row, columnIndex].content
                    if (rosterText.isNotBlank()) {
                        val times = Regex("""(?<from>\d+:\d+[AP]) - (?<to>\d+:\d+[AP])""").find(rosterText)?.run {
                            val formatter = DateTimeFormatter.ofPattern("h:mma")
                            LocalTimeRange(
                                "${groups.getValue("from")}M".toLocalTime(formatter),
                                "${groups.getValue("to")}M".toLocalTime(formatter)
                            )
                        }

                        job.shifts.add(Shift(date, times, department, rosterText))
                    }
                }
            }
        }
        return departmentBuilders.map { it.build() }.toSet()
    }
}

fun List<Table>.extractDepartmentRosters(): Set<DepartmentRoster> {
    val extractor = TableRostersExtractor()
    return extractor.extract(this)
}