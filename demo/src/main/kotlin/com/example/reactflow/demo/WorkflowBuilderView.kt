package com.example.reactflow.demo

import com.example.reactflow.component.ReactFlow
import com.example.reactflow.component.ReactFlow.*
import com.example.reactflow.component.ReactFlowEdge
import com.example.reactflow.component.ReactFlowNode
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Menu
import com.vaadin.flow.router.Route

@Route("workflow")
@Menu(title = "Workflow Builder", order = 1.0)
class WorkflowBuilderView : VerticalLayout() {
    private var stepIndex = 1

    init {
        setSizeFull()
        isPadding = false
        isSpacing = false

        val flow = ReactFlow()
        flow.setDefaultEdgeType("floating")
        flow.setSizeFull()
        flow.setShowMiniMap(true)
        flow.setShowControls(true)
        flow.setBackgroundVariant("dots")

        // Predefined workflow step types with styling
        val stepStyles = mapOf(
            "Trigger" to mapOf("background" to "#e8f5e9", "borderColor" to "#4caf50", "borderWidth" to "2px", "borderRadius" to "8px"),
            "Action" to mapOf("background" to "#e3f2fd", "borderColor" to "#2196f3", "borderWidth" to "2px", "borderRadius" to "8px"),
            "Condition" to mapOf("background" to "#fff3e0", "borderColor" to "#ff9800", "borderWidth" to "2px", "borderRadius" to "8px"),
            "Delay" to mapOf("background" to "#f3e5f5", "borderColor" to "#9c27b0", "borderWidth" to "2px", "borderRadius" to "8px"),
            "End" to mapOf("background" to "#ffebee", "borderColor" to "#f44336", "borderWidth" to "2px", "borderRadius" to "8px"),
        )

        // Seed with a trigger
        val trigger = ReactFlowNode(id = "trigger-1").withType("default").label("On Form Submit")
            .apply { style = stepStyles["Trigger"] }
        trigger.position = ReactFlowNode.NodePosition(300.0, 50.0)
        flow.addNode(trigger)

        // Toolbar
        val stepTypeSelect = ComboBox<String>("Step type").apply {
            setItems(stepStyles.keys)
            value = "Action"
            width = "150px"
        }
        val stepNameField = TextField("Step name").apply {
            placeholder = "e.g. Send Email"
            width = "200px"
        }
        val sourceSelect = ComboBox<String>("After step").apply {
            setItems(flow.getNodes().mapNotNull { it.id })
            value = "trigger-1"
            width = "160px"
        }
        val addStepBtn = Button("Add Step", VaadinIcon.PLUS.create()) {
            val type = stepTypeSelect.value ?: return@Button
            val name = stepNameField.value.ifBlank { "$type ${stepIndex + 1}" }
            val src = sourceSelect.value ?: return@Button

            val node = ReactFlowNode(id = "step-${++stepIndex}").withType("default").label(name)
                .apply { style = stepStyles[type] }
            flow.addNodeRelativeTo(
                anchorNodeId = src,
                newNode = node,
                direction = Direction.SOUTH,
                distance = 120.0,
                connect = true,
                edgeType = "floating",
            )
            sourceSelect.setItems(flow.getNodes().mapNotNull { it.id })
            sourceSelect.value = node.id
            stepNameField.clear()
        }
        addStepBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val layoutBtn = Button("Auto Layout", VaadinIcon.GRID_BIG_O.create()) {
            flow.applyLayout(LayoutAlgorithm.DAGRE, LayoutDirection.TB)
        }
        val exportBtn = Button("Export", VaadinIcon.DOWNLOAD.create()) {
            flow.exportImage()
        }
        val validateBtn = Button("Validate", VaadinIcon.CHECK.create()) {
            val nodes = flow.getNodes()
            val edges = flow.getEdges()
            val endNodes = nodes.filter { n -> edges.none { it.source == n.id } }
            if (endNodes.isEmpty()) {
                Notification.show("Warning: No terminal nodes found", 3000, Notification.Position.BOTTOM_CENTER)
            } else {
                Notification.show("Workflow valid: ${nodes.size} steps, ${edges.size} connections", 3000, Notification.Position.BOTTOM_CENTER)
            }
        }

        val toolbar = HorizontalLayout(stepTypeSelect, stepNameField, sourceSelect, addStepBtn, layoutBtn, exportBtn, validateBtn)
        toolbar.defaultVerticalComponentAlignment = Alignment.BASELINE
        toolbar.isPadding = true

        add(
            VerticalLayout(
                H3("Workflow Builder"),
                Paragraph("Build automation workflows by adding steps. Each step type has a distinct color."),
                toolbar,
            ).apply { isPadding = true; isSpacing = true },
            flow,
        )
        expand(flow)
    }
}
