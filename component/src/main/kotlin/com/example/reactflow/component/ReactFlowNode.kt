package com.example.reactflow.component

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

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

    constructor(id: String, x: Double, y: Double) : this(
        id = id,
        position = NodePosition(x, y),
    )

    constructor(id: String, label: String, x: Double, y: Double) : this(id, x, y) {
        data["label"] = label
    }

    constructor(id: String, type: String, label: String, x: Double, y: Double) : this(id, label, x, y) {
        this.type = type
    }

    fun label(label: String): ReactFlowNode = apply {
        data["label"] = label
    }

    fun withData(key: String, value: Any): ReactFlowNode = apply {
        data[key] = value
    }

    fun withType(type: String): ReactFlowNode = apply {
        this.type = type
    }

    fun withParent(parentId: String, extent: String? = null): ReactFlowNode = apply {
        this.parentId = parentId
        this.extent = extent
    }

    fun withExtent(extent: String = "parent"): ReactFlowNode = apply {
        this.extent = extent
    }

    fun withExpandParent(expandParent: Boolean = true): ReactFlowNode = apply {
        this.expandParent = expandParent
    }

    fun withStyle(style: Map<String, Any>): ReactFlowNode = apply {
        this.style = style
    }

    fun withSize(width: Int, height: Int): ReactFlowNode = apply {
        this.width = width
        this.height = height
        this.style = (this.style.orEmpty()) + mapOf("width" to "${width}px", "height" to "${height}px")
    }

    companion object {
        fun group(id: String, label: String, x: Double, y: Double, width: Int, height: Int): ReactFlowNode =
            ReactFlowNode(id, "group", label, x, y).withSize(width, height)
    }

    class NodePosition(var x: Double = 0.0, var y: Double = 0.0) : Serializable
}
