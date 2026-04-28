package com.example.reactflow.demo

import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.router.Layout
import com.vaadin.flow.router.RouterLink

@Layout
class MainLayout : AppLayout() {

    private data class NavItem(val title: String, val path: String, val target: Class<*>)

    private val navItems = listOf(
        NavItem("Playground", "", ReactFlowDemoView::class.java),
        NavItem("Workflow Builder", "workflow", WorkflowBuilderView::class.java),
        NavItem("Org Chart", "org-chart", OrgChartView::class.java),
        NavItem("Mind Map", "mind-map", MindMapView::class.java),
        NavItem("State Machine", "state-machine", StateMachineView::class.java),
        NavItem("Network Topology", "network", NetworkTopologyView::class.java),
    )

    private val tabs = Tabs()
    private val tabMap = mutableMapOf<String, Tab>()

    init {
        val brand = H4("vaadin-reactflow").apply {
            style.set("margin", "0")
            style.set("padding-right", "var(--lumo-space-l)")
        }

        navItems.forEach { item ->
            val link = RouterLink(item.title, item.target as Class<com.vaadin.flow.component.Component>)
            val tab = Tab(link)
            tabs.add(tab)
            tabMap[item.path] = tab
        }

        val navbar = HorizontalLayout(brand, tabs).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            isPadding = true
            style.set("padding-left", "var(--lumo-space-l)")
        }

        addToNavbar(navbar)
    }

    override fun afterNavigation() {
        super.afterNavigation()
        val currentPath = ui.orElse(null)?.internals?.activeViewLocation?.path ?: ""
        tabMap[currentPath]?.let { tabs.selectedTab = it }
    }
}
