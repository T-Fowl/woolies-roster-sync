package com.tfowl.rosters.detection

import com.tfowl.rosters.*
import com.tfowl.rosters.graphs.UndirectedGraph
import com.tfowl.rosters.graphs.forEachEdge
import com.tfowl.rosters.pdf.Point
import org.apache.pdfbox.pdmodel.PDPage
import org.joml.Intersectiond
import org.joml.Vector3d
import java.awt.Color
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.util.*
import kotlin.math.abs


data class PdfLineIntersection(
    val midpoint: Point2D,
    val alignedMidpoint: Point2D,
    val lines: HashSet<PdfLine>,
)

data class IntersectionDetectorResults(
    val page: PDPage,
    val lines: Set<PdfLine>,
    val intersections: Set<PdfLineIntersection>,
    val horizontalGridLines: SortedSet<Double>,
    val verticalGridLines: SortedSet<Double>,
)

private fun VisualDebugger.visualiseDetection(detection: IntersectionDetectorResults) {
    visualiseEach("intersection-detection", detection.lines) { line ->
        draw(Color.BLACK, Line2D.Double(line.start.x, line.start.y, line.finish.x, line.finish.y))
    }

    visualiseEach("intersection-detection", detection.intersections) { intersection ->
        draw(Color.RED, CenteredEllipse(intersection.midpoint.x, intersection.midpoint.y, 4.0))
    }

    visualiseEach("intersection-detection", detection.horizontalGridLines) {
        draw(Color.PINK, Line2D.Double(0.0, it, detection.page.cropBox.width.toDouble(), it))
    }

    visualiseEach("intersection-detection", detection.verticalGridLines) {
        draw(Color.PINK, Line2D.Double(it, 0.0, it, detection.page.cropBox.height.toDouble()))
    }
}

class BasicLineIntersectionDetector(val tolerance: Double) {
    fun detect(lines: List<PdfLine>): List<PdfLineIntersection> {
        val intersections = mutableListOf<PdfLineIntersection>()

        val pointA = Vector3d()
        val pointB = Vector3d()

        // Yeah I know O(n^2) bad..
        for ((i, a) in lines.withIndex()) {
            for ((j, b) in lines.withIndex()) {
                if (j > i) {
                    val distance = Intersectiond.findClosestPointsLineSegments(
                        a.start.x, a.start.y, 0.0,
                        a.finish.x, a.finish.y, 0.0,
                        b.start.x, b.start.y, 0.0,
                        b.finish.x, b.finish.y, 0.0,
                        pointA, pointB
                    )

                    if (distance <= tolerance) {
                        val mid = Point2D.Double(
                            0.5 * (pointA.x + pointB.x),
                            0.5 * (pointA.y + pointB.y)
                        )
                        intersections.add(PdfLineIntersection(mid, mid, hashSetOf(a, b)))
                    }
                }
            }
        }
        return intersections
    }
}

class IntersectionDeduplicator(val tolerance: Double) {
    fun deduplicate(intersections: List<PdfLineIntersection>): List<PdfLineIntersection> {
        val clusters = intersections.fold(mutableListOf<MutableList<PdfLineIntersection>>()) { clusters, intersection ->
            val closest = clusters.firstOrNull { cluster ->
                cluster.any { it.midpoint.distance(intersection.midpoint) <= tolerance }
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
            PdfLineIntersection(mid, mid, allLines)
        }
    }
}


class IntersectionGridAlignment(val tolerance: Double) {

    data class AlignmentInfo(
        val horizontals: SortedSet<Double>,
        val verticals: SortedSet<Double>,
    )

    private fun alignOnAnAxis(
        intersections: List<PdfLineIntersection>,
        axisAccessor: Point2D.() -> Double,
        axisSetter: Point2D.(Double) -> Unit,
    ): SortedSet<Double> {
        val hasBeenAligned = hashSetOf<PdfLineIntersection>()
        val axisLines = sortedSetOf<Double>()

        for (intersection in intersections) {
            if (intersection in hasBeenAligned) continue

            val similarAxis = mutableListOf(intersection)

            for (other in intersections) {
                if (other !== intersection) {
                    if (abs(intersection.midpoint.axisAccessor() - other.midpoint.axisAccessor()) <= tolerance) {
                        similarAxis.add(other)
                    }
                }
            }

            val averageInAxis = similarAxis.map { it.midpoint.axisAccessor() }.average().roundToEighth()
            similarAxis.forEach { it.alignedMidpoint.axisSetter(averageInAxis) }
            axisLines.add(averageInAxis)
        }

        return axisLines
    }

    fun align(intersections: List<PdfLineIntersection>): Pair<AlignmentInfo, List<PdfLineIntersection>> {
        val verticals = alignOnAnAxis(intersections, axisAccessor = { x }, axisSetter = { setLocation(it, y) })
        val horizontals = alignOnAnAxis(intersections, axisAccessor = { y }, axisSetter = { setLocation(x, it) })

        return Pair(AlignmentInfo(horizontals, verticals), intersections)
    }
}

class IntersectionDetector {

    // TODO: Duplicates? && This is temporary bridge between new detection and old
    private fun UndirectedGraph<Point>.linesForOldDetection(): List<PdfLine> {
        val uniquePdfLines = mutableSetOf<PdfLine>()
        forEachEdge { a, b ->
            val x = PdfLine(a, b)
            val y = PdfLine(b, a)
            if (x !in uniquePdfLines && y !in uniquePdfLines)
                uniquePdfLines.add(x)
        }
        return uniquePdfLines.toList()
    }

    fun detect(
        page: PDPage,
        detectionTolerance: Double = 0.3,
        combineTolerance: Double = 0.3,
        alignmentTolerance: Double = 0.3,
        debugger: VisualDebugger,
    ): IntersectionDetectorResults {
        val elements = page.getVisualElements(debugger)
        // TODO: Bridge from old to new
        val lines = elements.linesForOldDetection()

        val intersectionDetector = BasicLineIntersectionDetector(detectionTolerance)
        val intersections = intersectionDetector.detect(lines)

        val deduplicator = IntersectionDeduplicator(combineTolerance)
        val uniqueIntersections = deduplicator.deduplicate(intersections)

        val aligner = IntersectionGridAlignment(alignmentTolerance)
        val (alignment, _) = aligner.align(uniqueIntersections)
        val (horizontalLines, verticalLines) = alignment


        val linesWhichIntersect = hashSetOf<PdfLine>().apply {
            uniqueIntersections.forEach { addAll(it.lines) }
        }

        return IntersectionDetectorResults(
            page,
            linesWhichIntersect.toSet(),
            uniqueIntersections.toSet(),
            horizontalLines,
            verticalLines
        ).also { debugger.visualiseDetection(it) }
    }
}