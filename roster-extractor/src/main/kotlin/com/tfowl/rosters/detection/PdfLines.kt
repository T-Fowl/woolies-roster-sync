package com.tfowl.rosters.detection

import com.tfowl.rosters.*
import com.tfowl.rosters.graphs.*
import com.tfowl.rosters.pdf.*
import org.apache.pdfbox.pdmodel.PDPage
import org.joml.primitives.AABBd
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.GeneralPath
import java.awt.geom.Line2D
import java.util.*

data class PdfLine(
    val start: Point,
    val finish: Point,
)

private class GridAppender(val graph: MutableUndirectedGraph<Point>) {

    fun appendPath(path: ArrayDeque<Point>) {
        path.forEach(graph::addNode)
        path.forEachWithNext(graph::addEdge)
    }

    fun appendRectangle(p0: Point, p1: Point, p2: Point, p3: Point) {
        graph.addNode(p0)
        graph.addNode(p1)
        graph.addNode(p2)
        graph.addNode(p3)

        graph.addEdge(p0, p1)
        graph.addEdge(p1, p2)
        graph.addEdge(p2, p3)
        graph.addEdge(p3, p0)
    }
}

class VisualElementsExtractor(val debugger: VisualDebugger) {
    fun extract(page: PDPage): UndirectedGraph<Point> {
        val events = page.graphicsEvents()

        val points = GridAppender(MutableUndirectedGraphImpl())

        val currentPath = ArrayDeque<Point>()

        events.forEach { event ->
            when (event) {
                is MoveToEvent          -> {
                    currentPath.clear()
                    currentPath.add(event.point)
                }
                is LineToEvent          -> {
                    currentPath.add(event.point)
                }
                is AppendRectangleEvent -> {
                    points.appendRectangle(event.p0, event.p1, event.p2, event.p3)
                }
                is ShowTextStringEvent  -> {
                }
                is FillPathEvent        -> {
                    currentPath.clear()
                }
                is StrokePathEvent      -> {
                    points.appendPath(currentPath)
                    currentPath.clear()
                }
            }
        }

        return points.graph
    }
}

data class GridWithAABB(
    val grid: MutableUndirectedGraph<Point>,
    val boundingBox: AABBCollectiond,
)

private fun Graphics2D.draw(
    colour: Color,
    grid: UndirectedGraph<Point>,
) {
    val shape = GeneralPath()

    grid.forEachEdge { start, end ->
        shape.append(Line2D.Double(start.x, start.y, end.x, end.y), false)
    }

    grid.forEachNode { point ->
        shape.append(CenteredEllipse(point.x, point.y, 5.0), false)
    }

    draw(colour, shape)
}

fun UndirectedGraph<Point>.aabb(margin: Double): AABBd {
    val aabb = nodes.fold(AABBd()) { aabb, point -> aabb.union(point.x, point.y, 0.0) }
    aabb.union(aabb.minX - margin, aabb.minY - margin, 0.0)
    aabb.union(aabb.maxX + margin, aabb.maxY + margin, 0.0)
    return aabb
}

fun PDPage.getVisualElements(debugger: VisualDebugger): UndirectedGraph<Point> {
    val extractor = VisualElementsExtractor(debugger)
    val pagePoints = extractor.extract(this)

    return pagePoints

//    val tolerance = 5.0
//
//    val subgrids = pagePoints.subgraphs().map { subgraph ->
//        GridWithAABB(subgraph.toMutableGraph(),
//                     AABBCollectiond(subgraph.aabb(margin = tolerance))
//        )
//    }.toMutableList()
//
//    println("${subgrids.size} subgraphs")
//    subgrids.forEachIndexed { i, (subgraph, aabb) ->
//        println("Subgraph #$i:\n$subgraph")
//        println("AABB: $aabb")
//    }
//
//    subgrids[1].let { (subgrid, aabb) ->
//        subgrid.nodes.forEachIndexed { i, a ->
//            subgrid.nodes.forEachIndexed { j, b ->
//                if (j > i) {
//                    val distance = a.distance(b)
//                    if (distance < 2) {
//                        val mid = a.lerp(b, t = 0.5).roundToEighth()
//                        println(mid)
//                    }
//                }
//            }
//        }
//    }
//
//    subgrids.forEachIndexed { i, (graph, aabb) ->
//        debugger.visualise("subgrids") {
//            draw(DistinctColours[i], graph)
//        }
//
//        debugger.visualise("subgrids-$i") {
//            draw(DistinctColours[i], graph)
//            draw(Color.BLACK, aabb)
//        }
//    }
//
//    debugger.images().forEach { (layer, img) ->
//        ImageIO.write(img, "PNG", File("debug-$layer.png"))
//    }
//
//    exitProcess(0)
}
