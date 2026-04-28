package com.example.reactflow.component

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

/**
 * Complete state object exchanged between the Kotlin component and the React
 * adapter. Vaadin observes this state key and transports updates in both
 * directions, allowing browser interactions to update the server-side model.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ReactFlowState(
    var nodes: List<ReactFlowNode> = emptyList(),
    var edges: List<ReactFlowEdge> = emptyList(),
    var fitView: Boolean = true,
    var snapToGrid: Boolean = false,
    var snapGridX: Int = 15,
    var snapGridY: Int = 15,
    var minZoom: Double = 0.5,
    var maxZoom: Double = 2.0,
    var nodesDraggable: Boolean = true,
    var nodesConnectable: Boolean = true,
    var elementsSelectable: Boolean = true,
    var backgroundVariant: String = "dots",
    var showMiniMap: Boolean = true,
    var showControls: Boolean = true,
    var showBackground: Boolean = true,
    var edgesReconnectable: Boolean = true,
    var defaultEdgeType: String? = null,
) : Serializable
