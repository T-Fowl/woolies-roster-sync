package com.tfowl.rosters

import com.tfowl.rosters.detection.getVisualElements
import com.tfowl.rosters.graphs.*
import com.tfowl.rosters.pdf.Point
import org.apache.pdfbox.pdmodel.PDDocument
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Color.RED
import java.awt.Color.WHITE
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun <N> UndirectedGraph<N>.toGraphViz(): String = buildString {
    append("graph {\n")

    forEachNode { node ->
        append("\tN${node.hashCode()} [label=\"$node\"]\n")
    }

    forEachEdge { a, b ->
        append("\tN${a.hashCode()} -- N${b.hashCode()}\n")
    }

    append("}")
}

// Consider a spatial-partitioning algorithm if this becomes too slow
fun MutableUndirectedGraph<Point>.combineClosePoints(delta: Double = 5.0): MutableUndirectedGraph<Point> {
    while (true) {
        val (a, b) = firstPairOrNull { a, b -> a.distance(b) <= delta } ?: break

        println("Merge($a, $b)")

        // Problem: Lines become non-orthogonal
        merge(a, b)
    }
    return this
}

fun main(vararg args: String) {
    require(args.isNotEmpty()) { "Usage: [exec] roster-file" }

    val pdf = PDDocument.load(File(args[0]))
    val elements = pdf.pages[0].getVisualElements(NoOpVisualDebugger())
        .toMutableGraph()
        .combineClosePoints()


    val grid = elements.subgraphs()
        .filter { it.nodes.size > 2 } // Filter out single lines (assumed underline)
        .single().toMutableGraph()

    val maxX = grid.nodes.maxOf { it.x } + 10
    val maxY = grid.nodes.maxOf { it.y } + 10

    val scale = 10.0

    val img = BufferedImage((maxX * scale).toInt(), (maxY * scale).toInt(), BufferedImage.TYPE_INT_RGB)

    val graphics = img.createGraphics()
    graphics.color = WHITE
    graphics.background = WHITE
    graphics.clearRect(0, 0, img.width, img.height)


    grid.forEachNode { point ->
        graphics.color = Color.BLACK
        graphics.fill(CenteredEllipse(point.x * scale, point.y * scale, 20.0))
    }

    grid.forEachEdge { a, b ->
        graphics.color = RED
        graphics.stroke = BasicStroke(10.0f)
        graphics.draw(
            Line2D.Double(
                a.x * scale, a.y * scale,
                b.x * scale, b.y * scale
            )
        )
    }

    ImageIO.write(img, "PNG", File("test.png"))

//    println(grid.toGraphViz())
}
