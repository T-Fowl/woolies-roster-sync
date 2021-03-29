package com.tfowl.rosters

import com.tfowl.rosters.detection.PdfTableExtractor
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

class RosterReader {
    fun read(file: File): Set<DepartmentRoster> {
        val document = PDDocument.load(file)
        val tables = PdfTableExtractor().let { extractor ->
            document.pages.map { extractor.extract(it, NoOpVisualDebugger()) }
        }
        return TableRostersExtractor().extract(tables)
    }
}