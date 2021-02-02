package com.tfowl.rosters.graphs

import java.util.ArrayDeque

internal interface GraphTraversal<N> {
    fun queue(node: N)
    fun retrieve(): N?
}

internal class DepthFirstSearch<N> : GraphTraversal<N> {
    private val stack = ArrayDeque<N>()

    override fun queue(node: N) = stack.addFirst(node)

    override fun retrieve(): N? = if (stack.isEmpty()) null else stack.removeFirst()
}

internal class BreadthFirstSearch<N> : GraphTraversal<N> {
    private val queue = ArrayDeque<N>()

    override fun queue(node: N) = queue.addLast(node)

    override fun retrieve(): N? = if (queue.isEmpty()) null else queue.removeFirst()
}

private fun <N> UndirectedGraph<N>.traverse(
    root: N,
    strategy: GraphTraversal<N>,
): Sequence<N> = sequence {
    val visited = mutableSetOf<N>()

    strategy.queue(root)

    while (true) {
        val node = strategy.retrieve() ?: break

        if (visited.add(node)) {
            yield(node)

            edges[node]?.forEach { sibling ->
                if (sibling !in visited) {
                    strategy.queue(sibling)
                }
            }
        }
    }
}

fun <N> UndirectedGraph<N>.dfs(root: N): Sequence<N> =
    traverse(root, DepthFirstSearch())

fun <N> UndirectedGraph<N>.bfs(root: N): Sequence<N> =
    traverse(root, BreadthFirstSearch())
