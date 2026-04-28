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

@Route("network")
@Menu(title = "Network Topology", order = 5.0)
class NetworkTopologyView : VerticalLayout() {

    init {
        setSizeFull()
        isPadding = false
        isSpacing = false

        val flow = ReactFlow()
        flow.setDefaultEdgeType("floating")
        flow.setSizeFull()
        flow.setShowMiniMap(true)
        flow.setShowControls(true)
        flow.setBackgroundVariant("lines")
        flow.setEdgesReconnectable(true)

        // Zone backgrounds — regular nodes styled as colored panels, not groups.
        // Using groups causes edges between zones to render behind the group background.
        fun zone(id: String, label: String, x: Double, y: Double, w: Int, h: Int, bg: String, border: String): ReactFlowNode =
            ReactFlowNode(id = id).withType("default").label(label).withSize(w, h).apply {
                position = ReactFlowNode.NodePosition(x, y)
                isDraggable = false
                isConnectable = false
                isSelectable = false
                style = (style.orEmpty()) + mapOf(
                    "background" to bg,
                    "borderColor" to border,
                    "borderWidth" to "2px",
                    "borderRadius" to "12px",
                    "fontSize" to "14px",
                    "fontWeight" to "bold",
                    "color" to border,
                    "display" to "flex",
                    "alignItems" to "flex-start",
                    "justifyContent" to "flex-start",
                )
            }

        val zoneInternet = zone("zone-internet", "Internet", 20.0, 10.0, 340, 120, "rgba(254,243,199,0.45)", "#f59e0b")
        val zoneDmz = zone("zone-dmz", "DMZ", 20.0, 170.0, 560, 120, "rgba(252,231,243,0.45)", "#ec4899")
        val zoneApp = zone("zone-app", "Application Tier", 20.0, 330.0, 720, 120, "rgba(219,234,254,0.45)", "#3b82f6")
        val zoneData = zone("zone-data", "Data Tier", 20.0, 490.0, 560, 120, "rgba(209,250,229,0.45)", "#10b981")

        // Nodes — all top-level, positioned absolutely within zone boundaries
        val cdn =       node("cdn",        "CDN",              "#fbbf24",  50.0,  55.0)
        val dns =       node("dns",        "DNS",              "#fbbf24", 210.0,  55.0)
        val lb =        node("lb",         "Load Balancer",    "#f472b6",  50.0, 215.0)
        val waf =       node("waf",        "WAF",              "#f472b6", 230.0, 215.0)
        val proxy =     node("proxy",      "Reverse Proxy",    "#f472b6", 400.0, 215.0)
        val app1 =      node("app-1",      "App Server 1",     "#60a5fa",  50.0, 375.0)
        val app2 =      node("app-2",      "App Server 2",     "#60a5fa", 230.0, 375.0)
        val app3 =      node("app-3",      "App Server 3",     "#60a5fa", 410.0, 375.0)
        val cache =     node("cache",      "Redis Cache",      "#f87171", 590.0, 375.0)
        val dbPrimary = node("db-primary", "DB Primary",       "#34d399",  50.0, 535.0)
        val dbReplica = node("db-replica", "DB Replica",       "#34d399", 230.0, 535.0)
        val s3 =        node("s3",         "Object Storage",   "#34d399", 410.0, 535.0)

        // Add zones first (lower in render order), then nodes on top
        flow.addNodes(listOf(zoneInternet, zoneDmz, zoneApp, zoneData))
        flow.addNodes(listOf(cdn, dns, lb, waf, proxy, app1, app2, app3, cache, dbPrimary, dbReplica, s3))

        // Connections
        listOf(
            "cdn" to "lb", "dns" to "lb",
            "lb" to "waf", "waf" to "proxy",
            "proxy" to "app-1", "proxy" to "app-2", "proxy" to "app-3",
            "app-1" to "cache", "app-2" to "cache", "app-3" to "cache",
            "app-1" to "db-primary", "app-2" to "db-primary", "app-3" to "db-primary",
            "db-primary" to "db-replica",
            "app-1" to "s3",
        ).forEach { (from, to) ->
            flow.addEdge(ReactFlowEdge(from, to).withType("floating").withArrowEnd())
        }

        val toolbar = HorizontalLayout(
            Button("Top-down") { flow.applyLayout(LayoutAlgorithm.DAGRE, LayoutDirection.TB) },
            Button("Left-right") { flow.applyLayout(LayoutAlgorithm.DAGRE, LayoutDirection.LR) },
            Button("ELK") { flow.applyLayout(LayoutAlgorithm.ELK, LayoutDirection.TB) },
            Button("Export") { flow.exportImage() },
        )
        toolbar.defaultVerticalComponentAlignment = Alignment.BASELINE
        toolbar.isPadding = true

        add(
            VerticalLayout(
                H3("Network Topology"),
                Paragraph("A multi-zone network diagram. Zones are visual backgrounds; nodes and edges render above them."),
                toolbar,
            ).apply { isPadding = true; isSpacing = true },
            flow,
        )
        expand(flow)
    }

    private fun node(id: String, label: String, color: String, x: Double, y: Double): ReactFlowNode =
        ReactFlowNode(id = id).withType("default").label(label).apply {
            position = ReactFlowNode.NodePosition(x, y)
            style = mapOf(
                "background" to "#ffffff",
                "borderColor" to color,
                "borderWidth" to "2px",
                "borderRadius" to "6px",
                "fontSize" to "11px",
            )
        }
}
