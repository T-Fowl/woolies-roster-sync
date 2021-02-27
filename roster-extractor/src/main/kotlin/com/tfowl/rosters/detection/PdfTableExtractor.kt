package com.tfowl.rosters.detection

import com.jakewharton.picnic.Table
import com.tfowl.rosters.VisualDebugger
import org.apache.pdfbox.pdmodel.PDPage

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

class PdfTableExtractor(
    val detectionTolerance: Double = 0.275,
    val combineTolerance: Double = 0.275,
    val alignTolerance: Double = 2.0,
) {
    fun extract(page: PDPage, debugger: VisualDebugger): Table {
        val detection = IntersectionDetector(detectionTolerance, combineTolerance, alignTolerance)
            .detect(page, debugger)

        return TableIdentifier().identify(page, detection, debugger)
    }
}