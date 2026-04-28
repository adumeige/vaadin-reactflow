package com.example.reactflow.component

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

/**
 * Server-side representation of a React Flow edge.
 *
 * Properties are named after @xyflow/react's Edge type so they can be passed
 * through Vaadin's JSON state without a translation layer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ReactFlowEdge(
    var id: String? = null,
    var source: String? = null,
    var target: String? = null,
    var sourceHandle: String? = null,
    var targetHandle: String? = null,
    var type: String? = null,
    var label: String? = null,
    var isAnimated: Boolean = false,
    var isSelectable: Boolean = true,
    var isDeletable: Boolean = true,
    var data: MutableMap<String, Any> = mutableMapOf(),
    var markerEnd: Map<String, Any>? = null,
    var markerStart: Map<String, Any>? = null,
    var style: String? = null,
    var className: String? = null,
    var reconnectable: String? = null,
    var isSelected: Boolean = false,
    var zIndex: Int? = null,
) : Serializable {

    /** Creates an edge with an explicit id between two node ids. */
    constructor(id: String, source: String, target: String) : this(
        id = id,
        source = source,
        target = target,
        data = mutableMapOf(),
    )

    /** Creates an edge with a generated id based on source and target ids. */
    constructor(source: String, target: String) : this(
        id = "$source-$target",
        source = source,
        target = target,
    )

    /** Sets the React Flow edge type, e.g. "default", "smoothstep" or "floating". */
    fun withType(type: String): ReactFlowEdge = apply {
        this.type = type
    }

    /** Adds a text label rendered along the edge. */
    fun withLabel(label: String): ReactFlowEdge = apply {
        this.label = label
    }

    /** Toggles React Flow's built-in animated edge styling. */
    fun withAnimated(animated: Boolean): ReactFlowEdge = apply {
        this.isAnimated = animated
    }

    /** Adds custom serializable data consumed by custom React edge renderers. */
    fun withData(key: String, value: Any): ReactFlowEdge = apply {
        data[key] = value
    }

    /** Adds a closed arrow marker at the target side of the edge. */
    fun withArrowEnd(): ReactFlowEdge = apply {
        markerEnd = mapOf("type" to "arrowclosed")
    }

    /** Adds a closed arrow marker at the source side of the edge. */
    fun withArrowStart(): ReactFlowEdge = apply {
        markerStart = mapOf("type" to "arrowclosed")
    }

    /** Controls whether and how this edge can be reconnected in the browser. */
    fun withReconnectable(mode: String = "true"): ReactFlowEdge = apply {
        reconnectable = mode
    }
}
