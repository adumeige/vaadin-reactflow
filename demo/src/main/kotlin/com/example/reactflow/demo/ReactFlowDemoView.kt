package com.example.reactflow.demo

import com.example.reactflow.component.ReactFlow
import com.example.reactflow.component.ReactFlow.*
import com.example.reactflow.component.ReactFlowEdge
import com.example.reactflow.component.ReactFlowNode
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.NumberField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.Menu
import com.vaadin.flow.router.Route

@Route("")
@Menu(title = "Playground", order = 0.0)
class ReactFlowDemoView : VerticalLayout() {
    private var index = 10

    init {
        setSizeFull()

        add(H2("React Flow - Vaadin Component Demo"))
        add(
            Paragraph(
                "This demo shows a React Flow diagram embedded in a Vaadin application. "
                        + "You can drag nodes, create connections, zoom, and pan."
            )
        )

        val flow = ReactFlow()
        flow.setDefaultEdgeType("floating")
        flow.setHeight("500px")
        flow.setWidthFull()

        // --- Nodes ---

        val groupA = ReactFlowNode.group("group-a", "Group A", 0.0, 0.0, 300, 200)
        val child1 = ReactFlowNode("g-a-1", "default", "Step A.1", 20.0, 50.0)
            .withParent("group-a")
        val child2 = ReactFlowNode("g-a-2", "default", "Step A.2", 20.0, 120.0)
            .withParent("group-a")
        val start = ReactFlowNode("1", "input", "Start", 0.0, 300.0)
        val validate = ReactFlowNode("2", "default", "Validate Input", 250.0, 380.0)
        val process = ReactFlowNode("3", "default", "Process Data", 500.0, 300.0)
        val transform = ReactFlowNode("4", "default", "Transform", 500.0, 460.0)
        val merge = ReactFlowNode("5", "default", "Merge Results", 750.0, 380.0)
        val output = ReactFlowNode("6", "output", "Output", 1000.0, 380.0)

        flow.addNodes(listOf(groupA, child1, child2, start, validate, process, transform, merge, output))

        // --- Edges ---

        flow.addEdge(ReactFlowEdge("g-a-1", "g-a-2").withAnimated(true))
        flow.addEdge(ReactFlowEdge("1", "2").withAnimated(true))
        flow.addEdge(ReactFlowEdge("2", "3").withLabel("valid"))
        flow.addEdge(ReactFlowEdge("2", "4").withLabel("transform"))
        flow.addEdge(ReactFlowEdge("3", "5").withArrowEnd())
        flow.addEdge(ReactFlowEdge("4", "5").withArrowEnd())
        flow.addEdge(ReactFlowEdge("5", "6").withAnimated(true).withArrowEnd())

        // --- Toolbar ---

        val toolbar = HorizontalLayout()
        toolbar.defaultVerticalComponentAlignment = Alignment.BASELINE
        toolbar.add(Button("Add node") {
            flow.addNode(ReactFlowNode("$index", "default", "New Node", 100.0 + (10 * index), 100.0 + (10 * index++)))
        })
        toolbar.add(Button("Dagre (vertical)") { flow.applyLayout(LayoutAlgorithm.DAGRE, LayoutDirection.TB) })
        toolbar.add(Button("Dagre (horizontal)") { flow.applyLayout(LayoutAlgorithm.DAGRE, LayoutDirection.LR) })
        toolbar.add(Button("ELK (vertical)") { flow.applyLayout(LayoutAlgorithm.ELK, LayoutDirection.TB) })
        toolbar.add(Button("ELK (horizontal)") { flow.applyLayout(LayoutAlgorithm.ELK, LayoutDirection.LR) })
        toolbar.add(Button("Export image") { flow.exportImage() })
        add(toolbar)

        // --- Relative placement toolbar ---

        val sourceSelect = ComboBox<String>("Source node").apply {
            setItems(flow.getNodes().mapNotNull { it.id })
            value = "6"
            width = "160px"
        }
        val directionSelect = ComboBox<Direction>("Direction").apply {
            setItems(*Direction.entries.toTypedArray())
            value = Direction.EAST
            width = "130px"
        }
        val distanceField = IntegerField("Distance").apply {
            value = 250
            min = 50
            step = 50
            isStepButtonsVisible = true
            width = "120px"
        }
        val floatingCheckbox = Checkbox("Floating edges", true).apply {
            addValueChangeListener { flow.setDefaultEdgeType(if (it.value) "floating" else null) }
        }
        val addRelativeBtn = Button("Add connected node") {
            val srcId = sourceSelect.value ?: return@Button
            val dir = directionSelect.value ?: return@Button
            val dist = distanceField.value?.toDouble() ?: 250.0
            val node = ReactFlowNode(id = "${index++}").withType("default").label("Node $index")
            flow.addNodeRelativeTo(
                anchorNodeId = srcId,
                newNode = node,
                direction = dir,
                distance = dist,
                connect = true,
                edgeType = if (floatingCheckbox.value) "floating" else null,
            )
            sourceSelect.setItems(flow.getNodes().mapNotNull { it.id })
        }

        val relativeToolbar =
            HorizontalLayout(sourceSelect, directionSelect, distanceField, floatingCheckbox, addRelativeBtn)
        relativeToolbar.defaultVerticalComponentAlignment = Alignment.BASELINE
        add(relativeToolbar)

        // --- Main content: flow + property editor sidebar ---

        val sidebar = createSidebar(flow)
        val contentLayout = HorizontalLayout(flow, sidebar).apply {
            setSizeFull()
            expand(flow)
            sidebar.width = "340px"
        }
        add(contentLayout)
        expand(contentLayout)

        // --- Configuration ---

        flow.setShowMiniMap(true)
        flow.setShowControls(true)
        flow.setShowBackground(true)
        flow.setBackgroundVariant("dots")
    }

    private fun colorSwatch(id: String): Span = Span().apply {
        element.setAttribute("id", id)
        element.style.set("display", "inline-block")
        element.style.set("width", "16px")
        element.style.set("height", "16px")
        element.style.set("border-radius", "3px")
        element.style.set("border", "1px solid var(--lumo-contrast-30pct)")
        element.style.set("background", "#ffffff")
    }

    // ── Sidebar with tabs ──────────────────────────────────────────────

    private fun createSidebar(flow: ReactFlow): VerticalLayout {
        val nodeEditorPanel = createNodeEditor(flow)
        val edgeEditorPanel = createEdgeEditor(flow)

        val nodeTab = Tab("Node")
        val edgeTab = Tab("Edge")
        val tabs = Tabs(nodeTab, edgeTab)

        edgeEditorPanel.isVisible = false

        tabs.addSelectedChangeListener {
            nodeEditorPanel.isVisible = tabs.selectedTab === nodeTab
            edgeEditorPanel.isVisible = tabs.selectedTab === edgeTab
        }

        // Auto-switch tab when selection changes
        flow.addSelectionChangeListener {
            val selNode = flow.getSelectedNode()
            val selEdge = flow.getSelectedEdge()
            if (selNode != null) tabs.selectedTab = nodeTab
            else if (selEdge != null) tabs.selectedTab = edgeTab
        }

        return VerticalLayout(tabs, nodeEditorPanel, edgeEditorPanel).apply {
            isPadding = false
            isSpacing = false
            style.set("border-left", "1px solid var(--lumo-contrast-10pct)")
        }
    }

    // ── Node Editor ────────────────────────────────────────────────────

    private fun createNodeEditor(flow: ReactFlow): VerticalLayout {
        val noSelection = Paragraph("Click a node to edit it")

        val nodeIdField = TextField("Node ID").apply { isReadOnly = true; isEnabled = false; setWidthFull() }
        val typeSelect = ComboBox<String>("Type").apply {
            setItems("default", "input", "output", "group", "multihandle")
            setWidthFull()
        }
        val labelField = TextField("Label").apply { setWidthFull() }
        val xField = NumberField("X").apply { setWidthFull() }
        val yField = NumberField("Y").apply { setWidthFull() }
        val parentSelect = ComboBox<String>("Parent").apply {
            isAllowCustomValue = false
            isClearButtonVisible = true
            setWidthFull()
        }
        val draggableCheckbox = Checkbox("Draggable")
        val connectableCheckbox = Checkbox("Connectable")
        val selectableCheckbox = Checkbox("Selectable")
        val deletableCheckbox = Checkbox("Deletable")

        // --- Style fields ---
        val bgColorField = TextField("Background").apply { setWidthFull(); placeholder = "#ffffff"; prefixComponent = colorSwatch("bgSwatch") }
        val textColorField = TextField("Text color").apply { setWidthFull(); placeholder = "#000000"; prefixComponent = colorSwatch("txtSwatch") }
        val fontSizeField = IntegerField("Font size (px)").apply { setWidthFull(); min = 6; max = 72; isStepButtonsVisible = true; step = 1; value = 12 }
        val boldCheckbox = Checkbox("Bold")
        val italicCheckbox = Checkbox("Italic")
        val borderColorField = TextField("Border color").apply { setWidthFull(); placeholder = "#1a192b"; prefixComponent = colorSwatch("borderSwatch") }
        val borderRadiusField = IntegerField("Border radius (px)").apply { setWidthFull(); min = 0; max = 50; isStepButtonsVisible = true; step = 1; value = 3 }
        val borderWidthField = IntegerField("Border width (px)").apply { setWidthFull(); min = 0; max = 10; isStepButtonsVisible = true; step = 1; value = 1 }

        val deleteBtn = Button("Delete node")

        val form = FormLayout(
            nodeIdField, typeSelect, labelField,
            xField, yField, parentSelect,
            draggableCheckbox, connectableCheckbox,
            selectableCheckbox, deletableCheckbox,
            bgColorField, textColorField,
            fontSizeField, boldCheckbox, italicCheckbox,
            borderColorField, borderRadiusField, borderWidthField,
            deleteBtn,
        ).apply {
            setResponsiveSteps(FormLayout.ResponsiveStep("0", 1))
        }

        var editingNodeId: String? = null
        var updatingForm = false

        fun populateForm(node: ReactFlowNode?) {
            updatingForm = true
            try {
                if (node == null) {
                    editingNodeId = null
                    noSelection.isVisible = true
                    form.isVisible = false
                    return
                }
                editingNodeId = node.id
                noSelection.isVisible = false
                form.isVisible = true

                nodeIdField.value = node.id ?: ""
                typeSelect.value = node.type ?: "default"
                labelField.value = (node.data["label"] as? String) ?: ""
                xField.value = node.position?.x ?: 0.0
                yField.value = node.position?.y ?: 0.0
                parentSelect.setItems(flow.getNodes().mapNotNull { it.id }.filter { it != node.id })
                parentSelect.value = node.parentId
                draggableCheckbox.value = node.isDraggable
                connectableCheckbox.value = node.isConnectable
                selectableCheckbox.value = node.isSelectable
                deletableCheckbox.value = node.isDeletable

                val s = node.style.orEmpty()
                bgColorField.value = s["background"] as? String ?: ""
                bgColorField.prefixComponent?.element?.style?.set("background", bgColorField.value.ifBlank { "#ffffff" })
                textColorField.value = s["color"] as? String ?: ""
                textColorField.prefixComponent?.element?.style?.set("background", textColorField.value.ifBlank { "#000000" })
                fontSizeField.value = (s["fontSize"] as? String)?.removeSuffix("px")?.toIntOrNull() ?: 12
                boldCheckbox.value = (s["fontWeight"] as? String) == "bold"
                italicCheckbox.value = (s["fontStyle"] as? String) == "italic"
                borderColorField.value = s["borderColor"] as? String ?: ""
                borderColorField.prefixComponent?.element?.style?.set("background", borderColorField.value.ifBlank { "#1a192b" })
                borderRadiusField.value = (s["borderRadius"] as? String)?.removeSuffix("px")?.toIntOrNull() ?: 3
                borderWidthField.value = (s["borderWidth"] as? String)?.removeSuffix("px")?.toIntOrNull() ?: 1
            } finally {
                updatingForm = false
            }
        }

        fun applyChange(mutator: (ReactFlowNode) -> Unit) {
            if (updatingForm) return
            val id = editingNodeId ?: return
            flow.updateNode(id, mutator)
        }

        typeSelect.addValueChangeListener { applyChange { it.type = typeSelect.value } }
        labelField.addValueChangeListener {
            applyChange {
                val text = labelField.value.ifBlank { null }
                if (text != null) it.data["label"] = text else it.data.remove("label")
            }
        }
        xField.addValueChangeListener {
            applyChange { it.position = ReactFlowNode.NodePosition(xField.value ?: 0.0, it.position?.y ?: 0.0) }
        }
        yField.addValueChangeListener {
            applyChange { it.position = ReactFlowNode.NodePosition(it.position?.x ?: 0.0, yField.value ?: 0.0) }
        }
        parentSelect.addValueChangeListener { applyChange { it.parentId = parentSelect.value } }
        draggableCheckbox.addValueChangeListener { applyChange { it.isDraggable = draggableCheckbox.value } }
        connectableCheckbox.addValueChangeListener { applyChange { it.isConnectable = connectableCheckbox.value } }
        selectableCheckbox.addValueChangeListener { applyChange { it.isSelectable = selectableCheckbox.value } }
        deletableCheckbox.addValueChangeListener { applyChange { it.isDeletable = deletableCheckbox.value } }

        // Style change listeners
        fun applyStyle(key: String, value: String?) {
            applyChange { node ->
                val current = node.style.orEmpty().toMutableMap()
                if (value.isNullOrBlank()) current.remove(key) else current[key] = value
                node.style = current.ifEmpty { null }
            }
        }

        bgColorField.addValueChangeListener {
            bgColorField.prefixComponent?.element?.style?.set("background", bgColorField.value.ifBlank { "#ffffff" })
            applyStyle("background", bgColorField.value)
        }
        textColorField.addValueChangeListener {
            textColorField.prefixComponent?.element?.style?.set("background", textColorField.value.ifBlank { "#000000" })
            applyStyle("color", textColorField.value)
        }
        fontSizeField.addValueChangeListener {
            applyStyle("fontSize", fontSizeField.value?.let { "${it}px" })
        }
        boldCheckbox.addValueChangeListener {
            applyStyle("fontWeight", if (boldCheckbox.value) "bold" else null)
        }
        italicCheckbox.addValueChangeListener {
            applyStyle("fontStyle", if (italicCheckbox.value) "italic" else null)
        }
        borderColorField.addValueChangeListener {
            borderColorField.prefixComponent?.element?.style?.set("background", borderColorField.value.ifBlank { "#1a192b" })
            applyStyle("borderColor", borderColorField.value)
        }
        borderRadiusField.addValueChangeListener {
            applyStyle("borderRadius", borderRadiusField.value?.let { "${it}px" })
        }
        borderWidthField.addValueChangeListener {
            applyStyle("borderWidth", borderWidthField.value?.let { "${it}px" })
        }

        deleteBtn.addClickListener {
            val id = editingNodeId ?: return@addClickListener
            flow.removeNode(id)
            populateForm(null)
        }

        flow.addSelectionChangeListener {
            val selected = flow.getSelectedNode()
            if (selected?.id != editingNodeId) {
                populateForm(selected)
            }
        }

        form.isVisible = false

        return VerticalLayout(noSelection, form).apply {
            isPadding = true
            isSpacing = true
        }
    }

    // ── Edge Editor ────────────────────────────────────────────────────

    private fun createEdgeEditor(flow: ReactFlow): VerticalLayout {
        val noSelection = Paragraph("Click an edge to edit it")

        val edgeIdField = TextField("Edge ID").apply { isReadOnly = true; isEnabled = false; setWidthFull() }
        val sourceField = TextField("Source").apply { isReadOnly = true; isEnabled = false; setWidthFull() }
        val targetField = TextField("Target").apply { isReadOnly = true; isEnabled = false; setWidthFull() }
        val typeSelect = ComboBox<String>("Type").apply {
            setItems("default", "floating", "straight", "step", "smoothstep", "bezier")
            setWidthFull()
        }
        val labelField = TextField("Label").apply { setWidthFull() }
        val animatedCheckbox = Checkbox("Animated")
        val arrowEndCheckbox = Checkbox("Arrow at end")
        val arrowStartCheckbox = Checkbox("Arrow at start")
        val reconnectableSelect = ComboBox<String>("Reconnectable").apply {
            setItems("true", "false", "source", "target")
            setWidthFull()
        }
        val deleteBtn = Button("Delete edge")

        val form = FormLayout(
            edgeIdField, sourceField, targetField,
            typeSelect, labelField,
            animatedCheckbox, arrowEndCheckbox, arrowStartCheckbox,
            reconnectableSelect, deleteBtn,
        ).apply {
            setResponsiveSteps(FormLayout.ResponsiveStep("0", 1))
        }

        var editingEdgeId: String? = null
        var updatingForm = false

        fun populateForm(edge: ReactFlowEdge?) {
            updatingForm = true
            try {
                if (edge == null) {
                    editingEdgeId = null
                    noSelection.isVisible = true
                    form.isVisible = false
                    return
                }
                editingEdgeId = edge.id
                noSelection.isVisible = false
                form.isVisible = true

                edgeIdField.value = edge.id ?: ""
                sourceField.value = edge.source ?: ""
                targetField.value = edge.target ?: ""
                typeSelect.value = edge.type ?: "default"
                labelField.value = edge.label ?: ""
                animatedCheckbox.value = edge.isAnimated
                arrowEndCheckbox.value = edge.markerEnd != null
                arrowStartCheckbox.value = edge.markerStart != null
                reconnectableSelect.value = edge.reconnectable ?: "true"
            } finally {
                updatingForm = false
            }
        }

        fun applyChange(mutator: (ReactFlowEdge) -> Unit) {
            if (updatingForm) return
            val id = editingEdgeId ?: return
            flow.updateEdge(id, mutator)
        }

        typeSelect.addValueChangeListener { applyChange { it.type = typeSelect.value } }
        labelField.addValueChangeListener { applyChange { it.label = labelField.value.ifBlank { null } } }
        animatedCheckbox.addValueChangeListener { applyChange { it.isAnimated = animatedCheckbox.value } }
        arrowEndCheckbox.addValueChangeListener {
            applyChange { it.markerEnd = if (arrowEndCheckbox.value) mapOf("type" to "arrowclosed") else null }
        }
        arrowStartCheckbox.addValueChangeListener {
            applyChange { it.markerStart = if (arrowStartCheckbox.value) mapOf("type" to "arrowclosed") else null }
        }
        reconnectableSelect.addValueChangeListener { applyChange { it.reconnectable = reconnectableSelect.value } }
        deleteBtn.addClickListener {
            val id = editingEdgeId ?: return@addClickListener
            flow.removeEdge(id)
            populateForm(null)
        }

        flow.addSelectionChangeListener {
            val selected = flow.getSelectedEdge()
            if (selected?.id != editingEdgeId) {
                populateForm(selected)
            }
        }

        form.isVisible = false

        return VerticalLayout(noSelection, form).apply {
            isPadding = true
            isSpacing = true
        }
    }
}
