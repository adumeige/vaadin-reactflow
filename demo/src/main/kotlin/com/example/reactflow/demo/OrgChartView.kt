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

@Route("org-chart")
@Menu(title = "Org Chart", order = 2.0)
class OrgChartView : VerticalLayout() {

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
        flow.setNodesDraggable(true)

        val personStyle = mapOf(
            "background" to "#ffffff",
            "borderColor" to "#64748b",
            "borderWidth" to "2px",
            "borderRadius" to "12px",
            "fontSize" to "13px",
        )
        val execStyle = personStyle + mapOf(
            "background" to "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
            "color" to "#ffffff",
            "fontWeight" to "bold",
            "borderColor" to "#764ba2",
        )
        val managerStyle = personStyle + mapOf(
            "background" to "#f0f4ff",
            "borderColor" to "#667eea",
            "fontWeight" to "bold",
        )

        fun person(id: String, name: String, title: String, style: Map<String, Any>): ReactFlowNode =
            ReactFlowNode(id = id).withType("default").label("$name\n$title").apply { this.style = style }

        // C-Suite
        flow.addNode(person("ceo", "Alice Chen", "CEO", execStyle).apply { position = ReactFlowNode.NodePosition(400.0, 0.0) })

        // VPs
        flow.addNode(person("vp-eng", "Bob Smith", "VP Engineering", managerStyle).apply { position = ReactFlowNode.NodePosition(100.0, 120.0) })
        flow.addNode(person("vp-sales", "Carol Davis", "VP Sales", managerStyle).apply { position = ReactFlowNode.NodePosition(400.0, 120.0) })
        flow.addNode(person("vp-ops", "Dan Wilson", "VP Operations", managerStyle).apply { position = ReactFlowNode.NodePosition(700.0, 120.0) })

        // Engineering team
        flow.addNode(person("eng-1", "Eve Johnson", "Senior Engineer", personStyle).apply { position = ReactFlowNode.NodePosition(0.0, 260.0) })
        flow.addNode(person("eng-2", "Frank Lee", "Engineer", personStyle).apply { position = ReactFlowNode.NodePosition(200.0, 260.0) })

        // Sales team
        flow.addNode(person("sales-1", "Grace Kim", "Sales Lead", personStyle).apply { position = ReactFlowNode.NodePosition(350.0, 260.0) })
        flow.addNode(person("sales-2", "Henry Brown", "Sales Rep", personStyle).apply { position = ReactFlowNode.NodePosition(550.0, 260.0) })

        // Ops team
        flow.addNode(person("ops-1", "Ivy Martinez", "DevOps", personStyle).apply { position = ReactFlowNode.NodePosition(650.0, 260.0) })
        flow.addNode(person("ops-2", "Jack Taylor", "SRE", personStyle).apply { position = ReactFlowNode.NodePosition(850.0, 260.0) })

        // Reporting edges
        listOf(
            "ceo" to "vp-eng", "ceo" to "vp-sales", "ceo" to "vp-ops",
            "vp-eng" to "eng-1", "vp-eng" to "eng-2",
            "vp-sales" to "sales-1", "vp-sales" to "sales-2",
            "vp-ops" to "ops-1", "vp-ops" to "ops-2",
        ).forEach { (from, to) ->
            flow.addEdge(ReactFlowEdge(from, to).withType("floating").withArrowEnd())
        }

        val toolbar = HorizontalLayout(
            Button("Top-down") { flow.applyLayout(LayoutAlgorithm.DAGRE, LayoutDirection.TB) },
            Button("Left-right") { flow.applyLayout(LayoutAlgorithm.DAGRE, LayoutDirection.LR) },
            Button("ELK Layout") { flow.applyLayout(LayoutAlgorithm.ELK, LayoutDirection.TB) },
            Button("Export") { flow.exportImage() },
        )
        toolbar.defaultVerticalComponentAlignment = Alignment.BASELINE
        toolbar.isPadding = true

        add(
            VerticalLayout(
                H3("Organization Chart"),
                Paragraph("A hierarchical org chart with styled nodes. Try different layouts."),
                toolbar,
            ).apply { isPadding = true; isSpacing = true },
            flow,
        )
        expand(flow)
    }
}
