package com.example.reactflow.component

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

/**
 * Server-side representation of a React Flow node.
 *
 * The property names intentionally mirror @xyflow/react's Node shape so Vaadin
 * can serialize instances directly to the TypeScript adapter. Unknown fields
 * coming back from the browser are ignored to keep the component compatible with
 * React Flow adding client-only metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ReactFlowNode(
    var id: String? = null,
    var type: String? = null,
    var position: NodePosition? = null,
    var data: MutableMap<String, Any> = mutableMapOf(),
    var parentId: String? = null,
    var extent: String? = null,
    var expandParent: Boolean = false,
    var sourcePosition: String? = null,
    var targetPosition: String? = null,
    var isDraggable: Boolean = true,
    var isSelectable: Boolean = true,
    var isConnectable: Boolean = true,
    var isDeletable: Boolean = true,
    var style: Map<String, Any>? = null,
    var className: String? = null,
    var width: Int? = null,
    var height: Int? = null,
    var isSelected: Boolean = false,
) : Serializable {

    /** Creates a node at the given canvas coordinates. */
    constructor(id: String, x: Double, y: Double) : this(
        id = id,
        position = NodePosition(x, y),
    )

    /** Creates a node and stores its display label in the React Flow data map. */
    constructor(id: String, label: String, x: Double, y: Double) : this(id, x, y) {
        data["label"] = label
    }

    /** Creates a labeled node using one of React Flow's built-in or custom types. */
    constructor(id: String, type: String, label: String, x: Double, y: Double) : this(id, label, x, y) {
        this.type = type
    }

    /** Fluent helper for changing the display label. */
    fun label(label: String): ReactFlowNode = apply {
        data["label"] = label
    }

    /** Adds custom serializable data consumed by custom React node renderers. */
    fun withData(key: String, value: Any): ReactFlowNode = apply {
        data[key] = value
    }

    /** Sets the React Flow node type. */
    fun withType(type: String): ReactFlowNode = apply {
        this.type = type
    }

    /** Makes this node a child of a group/parent node. */
    fun withParent(parentId: String, extent: String? = null): ReactFlowNode = apply {
        this.parentId = parentId
        this.extent = extent
    }

    /** Restricts dragging to the given extent, commonly "parent" for group nodes. */
    fun withExtent(extent: String = "parent"): ReactFlowNode = apply {
        this.extent = extent
    }

    /** Enables parent expansion when this child is dragged beyond current bounds. */
    fun withExpandParent(expandParent: Boolean = true): ReactFlowNode = apply {
        this.expandParent = expandParent
    }

    /** Applies inline CSS style properties to the node. */
    fun withStyle(style: Map<String, Any>): ReactFlowNode = apply {
        this.style = style
    }

    /** Stores dimensions both as React Flow metadata and CSS style values. */
    fun withSize(width: Int, height: Int): ReactFlowNode = apply {
        this.width = width
        this.height = height
        this.style = (this.style.orEmpty()) + mapOf("width" to "${width}px", "height" to "${height}px")
    }

    companion object {
        /** Convenience factory for React Flow group nodes with fixed dimensions. */
        fun group(id: String, label: String, x: Double, y: Double, width: Int, height: Int): ReactFlowNode =
            ReactFlowNode(id, "group", label, x, y).withSize(width, height)
    }

    /** Position of the node's top-left corner in React Flow coordinates. */
    class NodePosition(var x: Double = 0.0, var y: Double = 0.0) : Serializable
}
