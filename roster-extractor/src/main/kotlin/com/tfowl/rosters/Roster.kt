package com.tfowl.rosters

import com.jakewharton.picnic.Table
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class LocalDatePeriod(val start: LocalDate, val end: LocalDate)

data class DepartmentInfoHeader(
    val siteCode: Int,
    val siteName: String,
    val name: String,
    val timePeriod: LocalDatePeriod,
    val executedOn: LocalDate
)

data class ShiftTimes(val from: LocalTime, val to: LocalTime)

data class Shift(val date: LocalDate, val period: ShiftTimes?, val department: String, val text: String)

data class Job(val title: String, val shifts: List<Shift>)

data class Employee(val name: String, val jobs: List<Job>)

data class DepartmentRoster(
    val department: DepartmentInfoHeader,
    val employees: List<Employee>,
)


internal data class JobBuilder(
    val title: String,
    val shifts: MutableList<Shift> = mutableListOf()
) {
    fun build(): Job = Job(title, shifts.toList())
}

internal data class EmployeeBuilder(
    val name: String,
    val jobs: MutableList<JobBuilder> = mutableListOf()
) {
    fun build(): Employee = Employee(name, jobs.map { it.build() })
}


fun List<Table>.extractDepartmentRosters(): Set<DepartmentRoster> {
    val localDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val localDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy h.mm a")

    val departmentRosters = hashSetOf<DepartmentRoster>()
    var section: DepartmentInfoHeader? = null
    val employees = mutableSetOf<EmployeeBuilder>()

    fun nextDepartment() {
        section?.let { department ->
            departmentRosters.add(
                DepartmentRoster(
                    department,
                    employees.map { it.build() }
                )
            )
            employees.clear()
        }
    }

    for (table in this) {
        if ("Location Schedule" in table[0, 0].cell.content) {
            val header = table[0, 0].cell.content

            val regex =
                Regex("""Location Schedule - (?<siteid>\d+) (?<sitename>.+?) - (?<department>.+?) Time Period\s?:\s?(?<from>\d+/\d+/\d+) - (?<to>\d+/\d+/\d+) Executed on: (?<executed>\d+/\d+/\d+\s*\d+\.\d+\s*[AP]M)""")
            val match = regex.find(header) ?: error("Unmatched header")


            nextDepartment()

            section = with(match.groups) {
                DepartmentInfoHeader(
                    getValue("siteid").toInt(),
                    getValue("sitename"),
                    getValue("department"),
                    LocalDatePeriod(
                        LocalDate.parse(getValue("from"), localDateFormatter),
                        LocalDate.parse(getValue("to"), localDateFormatter)
                    ),
                    LocalDate.parse(getValue("executed"), localDateTimeFormatter)
                )
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
            val jobTitle = table[row, jobHeader.columnIndex].cell.content
            val department = section!!.name

            val employee = EmployeeBuilder(name).also { employees.add(it) }
            val job = JobBuilder(jobTitle).also { employee.jobs.add(it) }

            for ((date, columnIndex) in datesColumns) {
                val rosterText = table[row, columnIndex].cell.content
                if (rosterText.isNotBlank()) {
                    val times = Regex("""(?<from>\d+:\d+[AP]) - (?<to>\d+:\d+[AP])""").find(rosterText)?.run {
                        val formatter = DateTimeFormatter.ofPattern("h:mma")
                        ShiftTimes(
                            LocalTime.parse(groups.getValue("from") + "M", formatter),
                            LocalTime.parse(groups.getValue("to") + "M", formatter)
                        )
                    }

                    job.shifts.add(Shift(date, times, department, rosterText))
                }
            }
        }
    }

    nextDepartment()

    return departmentRosters
}