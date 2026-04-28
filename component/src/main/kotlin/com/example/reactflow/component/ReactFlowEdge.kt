package com.example.reactflow.component

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

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

    constructor(id: String, source: String, target: String) : this(
        id = id,
        source = source,
        target = target,
        data = mutableMapOf(),
    )

    constructor(source: String, target: String) : this(
        id = "$source-$target",
        source = source,
        target = target,
    )

    fun withType(type: String): ReactFlowEdge = apply {
        this.type = type
    }

    fun withLabel(label: String): ReactFlowEdge = apply {
        this.label = label
    }

    fun withAnimated(animated: Boolean): ReactFlowEdge = apply {
        this.isAnimated = animated
    }

    fun withData(key: String, value: Any): ReactFlowEdge = apply {
        data[key] = value
    }

    fun withArrowEnd(): ReactFlowEdge = apply {
        markerEnd = mapOf("type" to "arrowclosed")
    }

    fun withArrowStart(): ReactFlowEdge = apply {
        markerStart = mapOf("type" to "arrowclosed")
    }

    fun withReconnectable(mode: String = "true"): ReactFlowEdge = apply {
        reconnectable = mode
    }
}
