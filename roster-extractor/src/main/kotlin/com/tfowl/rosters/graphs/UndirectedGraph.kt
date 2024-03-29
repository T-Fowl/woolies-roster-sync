package com.tfowl.rosters.graphs

interface UndirectedGraph<N> {
    val nodes: Set<N>
    val edges: Map<N, Set<N>>
}

interface MutableUndirectedGraph<N> : UndirectedGraph<N> {
    fun addNode(node: N)
    fun removeNode(node: N)

    fun addEdge(a: N, b: N)
    fun removeEdge(a: N, b: N)
}

data class MutableUndirectedGraphImpl<N>(
    override val nodes: MutableSet<N> = hashSetOf(),
    override val edges: MutableMap<N, MutableSet<N>> = hashMapOf(),
) : MutableUndirectedGraph<N> {

    override fun addNode(node: N) {
        nodes += node
    }

    override fun removeNode(node: N) {
        nodes -= node

        edges[node]?.forEach { sibling ->
            edges[sibling]?.remove(node)
        }

        edges.remove(node)
    }

    override fun addEdge(a: N, b: N) {
        require(a in nodes) { "$a not a node" }
        require(b in nodes) { "$b not a node" }

        edges.computeIfAbsent(a) { hashSetOf() } += b
        edges.computeIfAbsent(b) { hashSetOf() } += a
    }

    override fun removeEdge(a: N, b: N) {
        edges[a]?.remove(b)
        edges[b]?.remove(a)
    }

    override fun toString(): String = buildString {
        val maxNodeLength = nodes.maxOf { "$it".length }

        append("Graph {\n")
        for (node in nodes) {
            append("\t")
            append("%-${maxNodeLength}s".format(node))
            append(" <---> ")
            edges[node]?.joinTo(this)
            append("\n")
        }
        append("}")
    }
}

fun <N> UndirectedGraph<N>.toMutableGraph(): MutableUndirectedGraph<N> {
    val mutableNodes = nodes.toMutableSet()
    val mutableEdges = edges.mapValuesTo(mutableMapOf()) { (_, siblings) -> siblings.toMutableSet() }

    return MutableUndirectedGraphImpl(mutableNodes, mutableEdges)
}

fun <N> UndirectedGraph<N>.forEachNode(consumer: (N) -> Unit) {
    nodes.forEach(consumer)
}

fun <N> UndirectedGraph<N>.forEachEdge(consumer: (N, N) -> Unit) {
    nodes.forEach { node ->
        edges[node]?.forEach { edge ->
            consumer(node, edge)
        }
    }
}

fun <V> UndirectedGraph<V>.firstPairOrNull(p: (V, V) -> Boolean): Pair<V, V>? {
    return nodes.asSequence().mapNotNull { a ->
        nodes.firstOrNull { b ->
            b !== a && p(a, b)
        }?.let { b -> a to b }
    }.firstOrNull()
}

fun <V> MutableUndirectedGraph<V>.merge(a: V, b: V) {
    require(a in nodes) { "$a not in graph" }
    require(b in nodes) { "$b not in graph" }

    val siblings = hashSetOf<V>()
    edges[a]?.let { siblings += it }
    edges[b]?.let { siblings += it }

    removeNode(a)
    removeNode(b)

    addNode(a)
    siblings.forEach { sib -> addEdge(a, sib) }
}