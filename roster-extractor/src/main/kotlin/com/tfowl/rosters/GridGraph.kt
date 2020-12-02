package com.tfowl.rosters

@Suppress("unused", "MemberVisibilityCanBePrivate", "CanBeParameter")
class GridGraph(val lines: Set<PdfLine>,
                val intersections: Set<PdfLineIntersection>
) {
    val linesToIntersections = mutableMapOf<PdfLine, HashSet<PdfLineIntersection>>()
    val intersectionsToLines = mutableMapOf<PdfLineIntersection, HashSet<PdfLine>>()

    init {

        intersections.forEach { intersection ->
            intersectionsToLines[intersection] = intersection.lines

            intersection.lines.forEach { line ->
                linesToIntersections.computeIfAbsent(line) { hashSetOf() }.add(intersection)
            }
        }
    }
}