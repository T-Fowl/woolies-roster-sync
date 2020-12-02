package com.tfowl.rosters

import org.apache.pdfbox.pdmodel.PDPage
import org.joml.Intersectiond
import org.joml.Vector3d
import java.awt.geom.Point2D
import java.util.*


data class PdfLineIntersectionPoint(
        val midpoint: Point2D,
        val alignedMidpoint: Point2D,
        val lines: HashSet<PdfLine>,
)

data class IntersectionDetectorResults(
        val page: PDPage,
        val lines: Set<PdfLine>,
        val intersections: Set<PdfLineIntersectionPoint>,
        val horizontalGridLines: SortedSet<Double>,
        val verticalGridLines: SortedSet<Double>) {

    val graph = GridGraph(lines, intersections)
}

class IntersectionDetector {
    fun detect(
            page: PDPage,
            tolerance: Double = 0.3,
            combineTolerance: Double = 0.3,
            alignTolerance: Double = 0.3
    ): IntersectionDetectorResults {
        val lines = with(page.getVisualElements()) { lines + rectangles.flatMap { it.lines() } }

        val intersections = lines.findAllIntersections(tolerance)

        val uniqueIntersections = intersections.combineIdentical(combineTolerance)

        val (horizontalLines, verticalLines) = uniqueIntersections.alignToGrid(alignTolerance)


        val linesWhichIntersect = hashSetOf<PdfLine>().apply {
            uniqueIntersections.forEach { addAll(it.lines) }
        }

        return IntersectionDetectorResults(page, linesWhichIntersect.toSet(), uniqueIntersections.toSet(), horizontalLines, verticalLines)
    }

    private fun List<PdfLine>.findAllIntersections(tolerance: Double): List<PdfLineIntersectionPoint> {
        val lines = this
        val intersections = mutableListOf<PdfLineIntersectionPoint>()

        val pointA = Vector3d()
        val pointB = Vector3d()

        // Yeah I know O(n^2) bad..
        for ((i, a) in lines.withIndex()) {
            for ((j, b) in lines.withIndex()) {
                if (j > i) {
                    val distance = Intersectiond.findClosestPointsLineSegments(
                            a.start.x, a.start.y, 0.0,
                            a.end.x, a.end.y, 0.0,
                            b.start.x, b.start.y, 0.0,
                            b.end.x, b.end.y, 0.0,
                            pointA, pointB
                    )

                    if (distance <= tolerance) {
                        val mid = Point2D.Double(
                                0.5 * (pointA.x + pointB.x),
                                0.5 * (pointA.y + pointB.y)
                        )
                        intersections.add(PdfLineIntersectionPoint(mid, mid, hashSetOf(a, b)))
                    }
                }
            }
        }
        return intersections
    }

    private fun List<PdfLineIntersectionPoint>.combineIdentical(combineTolerance: Double): List<PdfLineIntersectionPoint> {
        val clusters = fold(mutableListOf<MutableList<PdfLineIntersectionPoint>>()) { clusters, intersection ->
            val closest = clusters.firstOrNull { cluster ->
                cluster.any { it.midpoint.distance(intersection.midpoint) <= combineTolerance }
            }
            closest?.add(intersection) ?: clusters.add(mutableListOf(intersection))
            clusters
        }

        return clusters.map { cluster ->
            // Round to nearest 1/8th because it looks nicer in debug logs :)
            val avgX = cluster.map { it.midpoint.x }.average().roundToEighth()
            val avgY = cluster.map { it.midpoint.y }.average().roundToEighth()
            val allLines = hashSetOf<PdfLine>()
            cluster.forEach { allLines.addAll(it.lines) }
            val mid = Point2D.Double(avgX, avgY)
            PdfLineIntersectionPoint(mid, mid, allLines)
        }
    }

    private fun List<PdfLineIntersectionPoint>.alignToGrid(tolerance: Double = 0.3): Pair<SortedSet<Double>, SortedSet<Double>> {
        fun alignOnAxis(accessor: Point2D.() -> Double, setter: Point2D.(Double) -> Unit): List<Double> {
            val hasBeenAligned = hashSetOf<PdfLineIntersectionPoint>()
            val resutls = mutableListOf<Double>()

            for (intersection in this) {
                if (intersection in hasBeenAligned) continue

                val similarAxis = mutableListOf(intersection)

                for (other in this) {
                    if (other !== intersection) {
                        if (Math.abs(intersection.midpoint.accessor() - other.midpoint.accessor()) <= tolerance) {
                            similarAxis.add(other)
                        }
                    }
                }

                val avgAxis = similarAxis.map { it.midpoint.accessor() }.average().roundToEighth()
                similarAxis.forEach { it.alignedMidpoint.setter(avgAxis) }
                resutls.add(avgAxis)
            }
            return resutls
        }

        val verticals = alignOnAxis(accessor = { x }, setter = { setLocation(it, y) })
        val horizontals = alignOnAxis(accessor = { y }, setter = { setLocation(x, it) })

        return Pair(horizontals.sorted().toSortedSet(), verticals.sorted().toSortedSet())
    }

}