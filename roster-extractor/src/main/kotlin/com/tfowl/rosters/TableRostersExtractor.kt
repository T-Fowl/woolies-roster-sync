package com.tfowl.rosters

import com.jakewharton.picnic.Table
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

private data class JobBuilder(
    val title: String,
    val shifts: MutableList<Shift> = mutableListOf(),
) {
    fun build(): DepartmentJob = DepartmentJob(title, shifts.toList())
}

private data class EmployeeBuilder(
    val name: String,
    val jobs: MutableList<JobBuilder> = mutableListOf(),
) {
    fun build(): DepartmentEmployee = DepartmentEmployee(name, jobs.map { it.build() })
}

private data class DepartmentHeader(
    val siteCode: Int,
    val siteName: String,
    val name: String,
    val timePeriod: LocalDateRange,
    val executedOn: LocalDate,
)

private data class DepartmentBuilder(
    val info: DepartmentInfo,
    val timePeriod: LocalDateRange,
    val executedOn: LocalDate,
    val employees: MutableList<EmployeeBuilder> = mutableListOf(),
) {
    fun build(): DepartmentRoster = DepartmentRoster(info, timePeriod, executedOn, employees.map { it.build() })
}


// Locale.US for capitalised AM/PM
private val localDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)
private val timeFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.US)
private val localDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy h.mm a", Locale.US)

private fun String.tryParseDepartmentHeader(): DepartmentHeader? {

    val regex =
        Regex("""Location Schedule - (?<siteid>\d+) (?<sitename>.+?) - (?<department>.+?) Time Period\s?:\s?(?<from>\d+/\d+/\d+) - (?<to>\d+/\d+/\d+) Executed on: (?<executed>\d+/\d+/\d+\s*\d+\.\d+\s*[AP]M)""")
    val match = regex.find(this) ?: return null

    return with(match.groups) {
        DepartmentHeader(
            getValue("siteid").toInt(),
            getValue("sitename"),
            getValue("department"),
            LocalDateRange(
                getValue("from").toLocalDate(localDateFormatter),
                getValue("to").toLocalDate(localDateFormatter)
            ),
            getValue("executed").toLocalDateTime(localDateTimeFormatter).toLocalDate()
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

    private fun nextDepartment(header: DepartmentHeader) {
        currentDepartment = DepartmentBuilder(
            DepartmentInfo(header.siteCode, header.siteName, header.name),
            header.timePeriod, header.executedOn
        ).also { departmentBuilders.add(it) }
    }

    fun extract(tables: List<Table>): Roster {
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
                            LocalTimeRange(
                                "${groups.getValue("from")}M".toLocalTime(timeFormatter),
                                "${groups.getValue("to")}M".toLocalTime(timeFormatter)
                            )
                        }

                        job.shifts.add(Shift(date, times, department, rosterText))
                    }
                }
            }
        }
        return Roster(departmentBuilders.map { it.build() })
    }
}

