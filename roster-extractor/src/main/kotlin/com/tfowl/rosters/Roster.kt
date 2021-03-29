@file:Suppress("unused")

package com.tfowl.rosters

import java.time.LocalDate

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