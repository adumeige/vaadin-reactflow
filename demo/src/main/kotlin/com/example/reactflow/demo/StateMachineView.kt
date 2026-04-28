package com.example.reactflow.demo

import com.example.reactflow.component.ReactFlow
import com.example.reactflow.component.ReactFlow.LayoutAlgorithm
import com.example.reactflow.component.ReactFlow.LayoutDirection
import com.example.reactflow.component.ReactFlowEdge
import com.example.reactflow.component.ReactFlowNode
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Menu
import com.vaadin.flow.router.Route

@Route("state-machine")
@Menu(title = "State Machine", order = 4.0)
class StateMachineView : VerticalLayout() {

    init {
        setSizeFull()
        isPadding = false
        isSpacing = false

        val flow = ReactFlow()
        flow.setDefaultEdgeType("floating")
        flow.setSizeFull()
        flow.setShowMiniMap(true)
        flow.setShowControls(true)
        flow.setBackgroundVariant("cross")

        val stateStyle = mapOf(
            "background" to "#f8fafc",
            "borderColor" to "#94a3b8",
            "borderWidth" to "2px",
            "borderRadius" to "8px",
            "fontSize" to "13px",
            "fontWeight" to "bold",
        )
        val initialStyle = stateStyle + mapOf("background" to "#dcfce7", "borderColor" to "#16a34a")
        val finalStyle = stateStyle + mapOf("background" to "#fee2e2", "borderColor" to "#dc2626")
        val activeStyle = stateStyle + mapOf(
            "background" to "linear-gradient(135deg, #3b82f6, #1d4ed8)",
            "color" to "#ffffff",
            "borderColor" to "#1d4ed8",
        )

        // States of an order processing system
        data class State(val id: String, val label: String, val style: Map<String, Any>, val x: Double, val y: Double)

        val states = listOf(
            State("new", "New Order", initialStyle, 0.0, 200.0),
            State("validating", "Validating", activeStyle, 250.0, 200.0),
            State("approved", "Approved", stateStyle, 500.0, 100.0),
            State("rejected", "Rejected", finalStyle, 500.0, 300.0),
            State("processing", "Processing", activeStyle, 750.0, 100.0),
            State("shipped", "Shipped", stateStyle, 1000.0, 100.0),
            State("delivered", "Delivered", finalStyle, 1250.0, 100.0),
            State("cancelled", "Cancelled", finalStyle, 750.0, 300.0),
            State("refunded", "Refunded", finalStyle, 1000.0, 300.0),
        )

        states.forEach { s ->
            flow.addNode(
                ReactFlowNode(id = s.id).withType("default").label(s.label)
                    .apply { position = ReactFlowNode.NodePosition(s.x, s.y); style = s.style }
            )
        }

        // Transitions with labels
        data class Transition(val from: String, val to: String, val label: String)

        val transitions = listOf(
            Transition("new", "validating", "submit"),
            Transition("validating", "approved", "valid"),
            Transition("validating", "rejected", "invalid"),
            Transition("approved", "processing", "pay"),
            Transition("approved", "cancelled", "cancel"),
            Transition("processing", "shipped", "ship"),
            Transition("processing", "cancelled", "cancel"),
            Transition("shipped", "delivered", "confirm"),
            Transition("shipped", "refunded", "return"),
            Transition("cancelled", "refunded", "refund"),
        )

        transitions.forEach { t ->
            flow.addEdge(
                ReactFlowEdge(t.from, t.to)
                    .withType("floating")
                    .withLabel(t.label)
                    .withArrowEnd()
                    .withAnimated(t.label == "submit" || t.label == "ship")
            )
        }

        val toolbar = HorizontalLayout(
            Button("Horizontal Layout") { flow.applyLayout(LayoutAlgorithm.DAGRE, LayoutDirection.LR) },
            Button("Vertical Layout") { flow.applyLayout(LayoutAlgorithm.DAGRE, LayoutDirection.TB) },
            Button("ELK Layout") { flow.applyLayout(LayoutAlgorithm.ELK, LayoutDirection.LR) },
            Button("Export") { flow.exportImage() },
        )
        toolbar.defaultVerticalComponentAlignment = Alignment.BASELINE
        toolbar.isPadding = true

        add(
            VerticalLayout(
                H3("State Machine - Order Processing"),
                Paragraph("An order processing state machine with transitions. States are color-coded: green=initial, blue=active, red=terminal."),
                toolbar,
            ).apply { isPadding = true; isSpacing = true },
            flow,
        )
        expand(flow)
    }
}
