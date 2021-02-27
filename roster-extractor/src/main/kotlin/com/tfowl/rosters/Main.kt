package com.tfowl.rosters

import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

fun main(vararg args: String) {
    require(args.isNotEmpty()) { "Usage: [exec] roster-file" }

    val document = PDDocument.load(File(args[0]))
    val extractor = PdfTableExtractor()
    val rosters = extractor.extract(document)

    rosters.forEach { departmentRoster ->
        println(departmentRoster.department)

        departmentRoster.employees.forEach { employee ->
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
