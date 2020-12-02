package com.tfowl.rosters

@Suppress("unused", "MemberVisibilityCanBePrivate", "CanBeParameter")
class GridGraph(val lines: Set<PdfLine>,
                val intersections: Set<PdfLineIntersectionPoint>
) {
    val linesToIntersections = mutableMapOf<PdfLine, HashSet<PdfLineIntersectionPoint>>()
    val intersectionsToLines = mutableMapOf<PdfLineIntersectionPoint, HashSet<PdfLine>>()

    init {

        intersections.forEach { intersection ->
            intersectionsToLines[intersection] = intersection.lines

            intersection.lines.forEach { line ->
                linesToIntersections.computeIfAbsent(line) { hashSetOf() }.add(intersection)
            }
        }
    }
}