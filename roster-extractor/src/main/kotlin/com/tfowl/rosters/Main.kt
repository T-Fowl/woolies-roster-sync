package com.tfowl.rosters

import com.tfowl.rosters.detection.PdfTableExtractor
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

fun main(vararg args: String) {
    require(args.isNotEmpty()) { "Usage: [exec] roster-file" }

    val document = PDDocument.load(File(args[0]))

    val tables = PdfTableExtractor().let { extractor ->
        document.pages.map { extractor.extract(it, NoOpVisualDebugger()) }
    }

    val rosters = TableRostersExtractor().extract(tables)

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
