package com.example.reactflow.component

import com.vaadin.flow.component.HasSize
import com.vaadin.flow.component.HasStyle
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.dependency.NpmPackage
import com.vaadin.flow.component.react.ReactAdapterComponent
import com.vaadin.flow.function.SerializableConsumer
import java.io.Serializable

/**
 * Vaadin server-side wrapper around the @xyflow/react canvas.
 *
 * The component owns a Kotlin copy of nodes, edges and configuration options.
 * [syncState] serializes that model into the React adapter, while the adapter
 * sends user-driven changes back through the same `flowState` key.
 */
@NpmPackage(value = "@xyflow/react", version = "12.6.0")
@NpmPackage(value = "html-to-image", version = "1.11.11")
@NpmPackage(value = "@dagrejs/dagre", version = "1.1.4")
@NpmPackage(value = "elkjs", version = "0.9.3")
@JsModule("components/react-flow/react-flow-adapter.tsx")
@Tag("vaadin-react-flow")
class ReactFlow : ReactAdapterComponent(), HasSize, HasStyle {

    /** Mutable server-side graph model mirrored to the browser. */
    private val nodes = mutableListOf<ReactFlowNode>()
    private val edges = mutableListOf<ReactFlowEdge>()
    private var fitView = true
    private var snapToGrid = false
    private var snapGridX = 15
    private var snapGridY = 15
    private var minZoom = 0.5
    private var maxZoom = 2.0
    private var nodesDraggable = true
    private var nodesConnectable = true
    private var elementsSelectable = true
    private var backgroundVariant = "dots"
    private var showMiniMap = true
    private var showControls = true
    private var showBackground = true
    private var edgesReconnectable = true
    private var defaultEdgeType: String? = null

    /** Counters make one-shot client actions observable even when options repeat. */
    private var layoutCounter = 0
    private var exportCounter = 0

    /** Prevents the component from re-importing its own server-originated update. */
    private var syncing = false

    init {
        setWidthFull()
        setHeight("600px")
        syncState()

        // Keep server-side lists in sync with client-side changes
        // (node drags, edge reconnections, layout results, etc.)
        addStateChangeListener("flowState", ReactFlowState::class.java) { state ->
            if (!syncing) {
                nodes.clear()
                nodes.addAll(state.nodes)
                edges.clear()
                edges.addAll(state.edges)
            }
        }
    }

    // --- Node Management ---

    /** Adds a single node and pushes the updated graph to the browser. */
    fun addNode(node: ReactFlowNode) {
        nodes.add(node)
        syncState()
    }

    /** Adds multiple nodes in one state synchronization. */
    fun addNodes(newNodes: List<ReactFlowNode>) {
        nodes.addAll(newNodes)
        syncState()
    }

    /** Removes all nodes matching [nodeId]. Edges are left unchanged intentionally. */
    fun removeNode(nodeId: String) {
        nodes.removeAll { it.id == nodeId }
        syncState()
    }

    /** Replaces the complete node list. */
    fun setNodes(nodes: List<ReactFlowNode>) {
        this.nodes.clear()
        this.nodes.addAll(nodes)
        syncState()
    }

    /** Returns a defensive copy of the current server-side nodes. */
    fun getNodes(): List<ReactFlowNode> = nodes.toList()

    // --- Edge Management ---

    /** Adds a single edge and pushes the updated graph to the browser. */
    fun addEdge(edge: ReactFlowEdge) {
        edges.add(edge)
        syncState()
    }

    /** Adds multiple edges in one state synchronization. */
    fun addEdges(newEdges: List<ReactFlowEdge>) {
        edges.addAll(newEdges)
        syncState()
    }

    /** Removes all edges matching [edgeId]. */
    fun removeEdge(edgeId: String) {
        edges.removeAll { it.id == edgeId }
        syncState()
    }

    /** Replaces the complete edge list. */
    fun setEdges(edges: List<ReactFlowEdge>) {
        this.edges.clear()
        this.edges.addAll(edges)
        syncState()
    }

    /** Returns a defensive copy of the current server-side edges. */
    fun getEdges(): List<ReactFlowEdge> = edges.toList()

    /** Returns the first edge currently selected in the browser, if any. */
    fun getSelectedEdge(): ReactFlowEdge? = edges.firstOrNull { it.isSelected }

    /** Returns the first node currently selected in the browser, if any. */
    fun getSelectedNode(): ReactFlowNode? = nodes.firstOrNull { it.isSelected }

    /** Mutates one edge in place and synchronizes when it exists. */
    fun updateEdge(edgeId: String, updater: (ReactFlowEdge) -> Unit) {
        val edge = edges.firstOrNull { it.id == edgeId } ?: return
        updater(edge)
        syncState()
    }

    /** Mutates one node in place and synchronizes when it exists. */
    fun updateNode(nodeId: String, updater: (ReactFlowNode) -> Unit) {
        val node = nodes.firstOrNull { it.id == nodeId } ?: return
        updater(node)
        syncState()
    }

    /** Registers a listener invoked whenever browser state may affect selection. */
    fun addSelectionChangeListener(listener: SerializableConsumer<Unit>) {
        addStateChangeListener("flowState", ReactFlowState::class.java) { _ ->
            listener.accept(Unit)
        }
    }

    // --- Configuration ---

    /** Enables React Flow's initial viewport fitting. */
    fun setFitView(fitView: Boolean) {
        this.fitView = fitView
        syncState()
    }

    /** Toggles grid snapping for node movement. */
    fun setSnapToGrid(snapToGrid: Boolean) {
        this.snapToGrid = snapToGrid
        syncState()
    }

    /** Sets the snapping grid dimensions in pixels. */
    fun setSnapGrid(x: Int, y: Int) {
        this.snapGridX = x
        this.snapGridY = y
        syncState()
    }

    /** Sets the minimum zoom allowed by the canvas. */
    fun setMinZoom(minZoom: Double) {
        this.minZoom = minZoom
        syncState()
    }

    /** Sets the maximum zoom allowed by the canvas. */
    fun setMaxZoom(maxZoom: Double) {
        this.maxZoom = maxZoom
        syncState()
    }

    /** Controls whether users can drag nodes. */
    fun setNodesDraggable(nodesDraggable: Boolean) {
        this.nodesDraggable = nodesDraggable
        syncState()
    }

    /** Controls whether users can create connections from nodes. */
    fun setNodesConnectable(nodesConnectable: Boolean) {
        this.nodesConnectable = nodesConnectable
        syncState()
    }

    /** Controls whether nodes and edges can be selected. */
    fun setElementsSelectable(elementsSelectable: Boolean) {
        this.elementsSelectable = elementsSelectable
        syncState()
    }

    /** Sets the background variant: "dots", "lines" or "cross". */
    fun setBackgroundVariant(variant: String) {
        this.backgroundVariant = variant
        syncState()
    }

    /** Shows or hides the React Flow minimap. */
    fun setShowMiniMap(showMiniMap: Boolean) {
        this.showMiniMap = showMiniMap
        syncState()
    }

    /** Shows or hides the React Flow control buttons. */
    fun setShowControls(showControls: Boolean) {
        this.showControls = showControls
        syncState()
    }

    /** Shows or hides the canvas background. */
    fun setShowBackground(showBackground: Boolean) {
        this.showBackground = showBackground
        syncState()
    }

    /** Controls whether users can reconnect existing edges. */
    fun setEdgesReconnectable(edgesReconnectable: Boolean) {
        this.edgesReconnectable = edgesReconnectable
        syncState()
    }

    /** Sets the default type assigned to new browser-created edges. */
    fun setDefaultEdgeType(type: String?) {
        this.defaultEdgeType = type
        syncState()
    }

    // --- Relative placement ---

    /**
     * Places [newNode] around an existing anchor node.
     *
     * When [connect] is true, a matching edge is created. Non-floating edges are
     * attached to the handle pair that visually matches [direction].
     */
    fun addNodeRelativeTo(
        anchorNodeId: String,
        newNode: ReactFlowNode,
        direction: Direction,
        distance: Double = 200.0,
        connect: Boolean = false,
        edgeType: String? = null,
    ) {
        val anchor = nodes.firstOrNull { it.id == anchorNodeId }
            ?: throw IllegalArgumentException("Node '$anchorNodeId' not found")
        val anchorPos = anchor.position
            ?: throw IllegalStateException("Node '$anchorNodeId' has no position")

        val (dx, dy) = when (direction) {
            Direction.NORTH -> 0.0 to -distance
            Direction.SOUTH -> 0.0 to distance
            Direction.EAST -> distance to 0.0
            Direction.WEST -> -distance to 0.0
        }
        newNode.position = ReactFlowNode.NodePosition(anchorPos.x + dx, anchorPos.y + dy)

        // Upgrade anchor to multihandle if it uses a built-in type with limited handles
        if (anchor.type in listOf("input", "output", "default", null)) {
            anchor.type = "multihandle"
        }
        // New node always gets multihandle so edges can connect from any side
        newNode.type = "multihandle"

        nodes.add(newNode)
        if (connect) {
            val edgeId = "$anchorNodeId-${newNode.id}-${edges.size}"
            val edge = ReactFlowEdge(edgeId, anchorNodeId, newNode.id!!)
            if (edgeType != null) edge.type = edgeType
            // Floating edges compute their own path — don't constrain to handles
            if (edgeType != "floating") {
                val (srcHandle, tgtHandle) = when (direction) {
                    Direction.NORTH -> "top-src" to "bottom-tgt"
                    Direction.SOUTH -> "bottom-src" to "top-tgt"
                    Direction.EAST -> "right-src" to "left-tgt"
                    Direction.WEST -> "left-src" to "right-tgt"
                }
                edge.sourceHandle = srcHandle
                edge.targetHandle = tgtHandle
            }
            edges.add(edge)
        }
        syncState()
    }

    // --- Layout ---

    /** Requests a client-side automatic layout pass. */
    fun applyLayout(
        algorithm: LayoutAlgorithm = LayoutAlgorithm.DAGRE,
        direction: LayoutDirection = LayoutDirection.TB,
    ) {
        setState("layoutAction", LayoutAction(algorithm.name, direction.name, ++layoutCounter))
    }

    // --- Export ---

    /** Requests the browser to export the current viewport as a PNG image. */
    fun exportImage() {
        setState("exportAction", ExportAction(++exportCounter))
    }

    // --- Event Listeners ---

    /** Registers a listener for any browser-originated node state update. */
    fun addNodesChangeListener(listener: SerializableConsumer<ReactFlowState>) {
        addStateChangeListener("flowState", ReactFlowState::class.java, listener)
    }

    /** Registers a listener for any browser-originated edge state update. */
    fun addEdgesChangeListener(listener: SerializableConsumer<ReactFlowState>) {
        addStateChangeListener("flowState", ReactFlowState::class.java, listener)
    }

    // --- Internal ---

    /** Serializes all server-side fields into Vaadin adapter state. */
    private fun syncState() {
        syncing = true
        try {
            val state = ReactFlowState(
                nodes = ArrayList(nodes),
                edges = ArrayList(edges),
                fitView = fitView,
                snapToGrid = snapToGrid,
                snapGridX = snapGridX,
                snapGridY = snapGridY,
                minZoom = minZoom,
                maxZoom = maxZoom,
                nodesDraggable = nodesDraggable,
                nodesConnectable = nodesConnectable,
                elementsSelectable = elementsSelectable,
                backgroundVariant = backgroundVariant,
                showMiniMap = showMiniMap,
                showControls = showControls,
                showBackground = showBackground,
                edgesReconnectable = edgesReconnectable,
                defaultEdgeType = defaultEdgeType,
            )
            setState("flowState", state)
        } finally {
            syncing = false
        }
    }

    // --- Enums & action types ---

    enum class Direction { NORTH, SOUTH, EAST, WEST }

    enum class LayoutAlgorithm { DAGRE, ELK }

    enum class LayoutDirection { TB, BT, LR, RL }

    data class LayoutAction(
        val algorithm: String,
        val direction: String,
        val counter: Int,
    ) : Serializable

    data class ExportAction(
        val counter: Int,
    ) : Serializable
}
