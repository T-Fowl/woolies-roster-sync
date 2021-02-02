package com.tfowl.rosters.graphs


fun <N> UndirectedGraph<N>.subgraphs(): Sequence<UndirectedGraph<N>> = sequence {
    val unvisited = mutableSetOf<N>().also { it.addAll(nodes) }

    while (unvisited.isNotEmpty()) {
        val root = unvisited.first()

        val subgraph = MutableUndirectedGraphImpl<N>()

        dfs(root).forEach { node ->
            unvisited.remove(node)

            subgraph.addNode(node)
            edges[node]?.forEach { sibling ->
                subgraph.addNode(sibling)
                subgraph.addEdge(node, sibling)
            }
        }

        yield(subgraph)
    }
}