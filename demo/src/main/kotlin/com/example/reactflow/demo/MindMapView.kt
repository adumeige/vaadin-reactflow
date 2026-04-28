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
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Menu
import com.vaadin.flow.router.Route

@Route("mind-map")
@Menu(title = "Mind Map", order = 3.0)
class MindMapView : VerticalLayout() {
    private var nodeIndex = 0

    private val branchColors = listOf(
        "#ef4444", "#f97316", "#eab308", "#22c55e", "#3b82f6", "#8b5cf6", "#ec4899",
    )

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

        // Central topic
        val center = ReactFlowNode(id = "center").withType("default").label("Project Ideas")
            .apply {
                position = ReactFlowNode.NodePosition(400.0, 300.0)
                style = mapOf(
                    "background" to "linear-gradient(135deg, #667eea, #764ba2)",
                    "color" to "#ffffff",
                    "fontWeight" to "bold",
                    "fontSize" to "16px",
                    "borderRadius" to "24px",
                    "borderWidth" to "0px",
                )
            }
        flow.addNode(center)

        // Build branches with manual positioning (no addNodeRelativeTo to keep types as-is)
        data class Branch(val label: String, val dx: Double, val dy: Double, val children: List<Pair<String, Pair<Double, Double>>>)

        val branches = listOf(
            Branch("Mobile App", 0.0, -180.0, listOf("iOS" to (-120.0 to -100.0), "Android" to (0.0 to -100.0), "Flutter" to (120.0 to -100.0))),
            Branch("Web Platform", 280.0, 0.0, listOf("Dashboard" to (160.0 to -70.0), "API" to (160.0 to 0.0), "Auth" to (160.0 to 70.0))),
            Branch("Data Pipeline", 0.0, 180.0, listOf("Ingestion" to (-120.0 to 100.0), "Processing" to (0.0 to 100.0), "Storage" to (120.0 to 100.0))),
            Branch("DevOps", -280.0, 0.0, listOf("CI/CD" to (-160.0 to -70.0), "Monitoring" to (-160.0 to 0.0), "IaC" to (-160.0 to 70.0))),
        )

        val cx = 400.0
        val cy = 300.0

        branches.forEachIndexed { bi, branch ->
            val color = branchColors[bi % branchColors.size]
            val branchId = "branch-$bi"
            val branchNode = ReactFlowNode(id = branchId).withType("default").label(branch.label)
                .apply {
                    position = ReactFlowNode.NodePosition(cx + branch.dx, cy + branch.dy)
                    style = mapOf(
                        "background" to color,
                        "color" to "#ffffff",
                        "fontWeight" to "bold",
                        "borderRadius" to "16px",
                        "borderWidth" to "0px",
                        "fontSize" to "14px",
                    )
                }
            flow.addNode(branchNode)
            flow.addEdge(ReactFlowEdge("center", branchId).withType("floating"))

            branch.children.forEachIndexed { ci, (childLabel, offset) ->
                val leafId = "leaf-$bi-$ci"
                val leafNode = ReactFlowNode(id = leafId).withType("default").label(childLabel)
                    .apply {
                        position = ReactFlowNode.NodePosition(cx + branch.dx + offset.first, cy + branch.dy + offset.second)
                        style = mapOf(
                            "background" to "#ffffff",
                            "borderColor" to color,
                            "borderWidth" to "2px",
                            "borderRadius" to "12px",
                            "fontSize" to "12px",
                        )
                    }
                flow.addNode(leafNode)
                flow.addEdge(ReactFlowEdge(branchId, leafId).withType("floating"))
            }
        }
        nodeIndex = 20

        // Controls
        val ideaField = TextField("New idea").apply { placeholder = "Type an idea..."; width = "200px" }
        val parentSelect = ComboBox<String>("Attach to").apply {
            setItems(flow.getNodes().mapNotNull { it.id })
            value = "center"
            width = "160px"
        }
        val addBtn = Button("Add Idea") {
            val parent = parentSelect.value ?: return@Button
            val label = ideaField.value.ifBlank { "Idea ${++nodeIndex}" }
            val color = branchColors[nodeIndex % branchColors.size]
            val node = ReactFlowNode(id = "idea-${++nodeIndex}").withType("default").label(label)
                .apply {
                    style = mapOf(
                        "background" to "#ffffff",
                        "borderColor" to color,
                        "borderWidth" to "2px",
                        "borderRadius" to "12px",
                    )
                }
            flow.addNodeRelativeTo(parent, node, Direction.EAST, 160.0, connect = true, edgeType = "floating")
            parentSelect.setItems(flow.getNodes().mapNotNull { it.id })
            ideaField.clear()
        }
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val layoutBtn = Button("Auto Layout") { flow.applyLayout(LayoutAlgorithm.ELK, LayoutDirection.TB) }
        val exportBtn = Button("Export") { flow.exportImage() }

        val toolbar = HorizontalLayout(ideaField, parentSelect, addBtn, layoutBtn, exportBtn)
        toolbar.defaultVerticalComponentAlignment = Alignment.BASELINE
        toolbar.isPadding = true

        add(
            VerticalLayout(
                H3("Mind Map"),
                Paragraph("Brainstorm ideas: drag from a node's handle to empty space to create a connected idea. Or use the toolbar to add ideas."),
                toolbar,
            ).apply { isPadding = true; isSpacing = true },
            flow,
        )
        expand(flow)
    }
}
