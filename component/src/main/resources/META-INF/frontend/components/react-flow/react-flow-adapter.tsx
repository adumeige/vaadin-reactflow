import React, {type ReactElement, useCallback, useEffect, useRef, useState} from 'react';
import {
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  Background,
  BackgroundVariant,
  type Connection,
  type ConnectionLineComponentProps,
  Controls,
  type Edge,
  type EdgeChange,
  type EdgeProps,
  getBezierPath,
  Handle,
  type InternalNode,
  MiniMap,
  type Node,
  type NodeChange,
  type NodeProps,
  type OnConnect,
  type OnEdgesChange,
  type OnNodesChange,
  Position,
  ReactFlow,
  ReactFlowProvider,
  reconnectEdge,
  useNodesInitialized,
  useReactFlow,
  useStore,
  type XYPosition,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import dagre from '@dagrejs/dagre';
import ELK from 'elkjs/lib/elk.bundled.js';
import {toPng} from 'html-to-image';

import {ReactAdapterElement, type RenderHooks,} from 'Frontend/generated/flow/ReactAdapter';

// ── Custom node type with handles on all four sides ────────────────────

/**
 * Simple custom node renderer exposing source and target handles on every side.
 * This is used by the Kotlin relative-placement helper so generated edges can
 * connect from the side that matches the chosen direction.
 */
function MultiHandleNode({data}: NodeProps) {
    return (
        <div style={{
            padding: '10px 20px',
            borderRadius: 'inherit',
            fontSize: 'inherit',
            color: 'inherit',
            minWidth: 40,
            minHeight: 20,
        }}>
            <Handle type="target" position={Position.Top} id="top-tgt"/>
            <Handle type="source" position={Position.Top} id="top-src"/>
            <Handle type="target" position={Position.Left} id="left-tgt"/>
            <Handle type="source" position={Position.Left} id="left-src"/>
            {data?.label as string ?? ''}
            <Handle type="target" position={Position.Bottom} id="bottom-tgt"/>
            <Handle type="source" position={Position.Bottom} id="bottom-src"/>
            <Handle type="target" position={Position.Right} id="right-tgt"/>
            <Handle type="source" position={Position.Right} id="right-src"/>
        </div>
    );
}

const nodeTypes = {multihandle: MultiHandleNode};

// ── Floating edges ─────────────────────────────────────────────────────

/** Calculates where the line between two node centers intersects a node box. */
function getNodeIntersection(intersectionNode: InternalNode, targetNode: InternalNode): XYPosition {
    const {width: iw, height: ih} = intersectionNode.measured ?? {width: 0, height: 0};
    const targetPosition = targetNode.internals.positionAbsolute;

    const w = (iw ?? 0) / 2;
    const h = (ih ?? 0) / 2;
    const x2 = intersectionNode.internals.positionAbsolute.x + w;
    const y2 = intersectionNode.internals.positionAbsolute.y + h;
    const x1 = targetPosition.x + ((targetNode.measured?.width ?? 0) / 2);
    const y1 = targetPosition.y + ((targetNode.measured?.height ?? 0) / 2);

    const xx1 = (x1 - x2) / (2 * w) - (y1 - y2) / (2 * h);
    const yy1 = (x1 - x2) / (2 * w) + (y1 - y2) / (2 * h);
    const a = 1 / (Math.abs(xx1) + Math.abs(yy1) || 1);
    const xx3 = a * xx1;
    const yy3 = a * yy1;

    return {
        x: w * (xx3 + yy3) + x2,
        y: h * (-xx3 + yy3) + y2,
    };
}

/** Converts an intersection point into the nearest React Flow handle side. */
function getEdgePosition(node: InternalNode, intersectionPoint: XYPosition): Position {
    const nx = Math.round(node.internals.positionAbsolute.x);
    const ny = Math.round(node.internals.positionAbsolute.y);
    const px = Math.round(intersectionPoint.x);
    const py = Math.round(intersectionPoint.y);

    if (px <= nx + 1) return Position.Left;
    if (px >= nx + (node.measured?.width ?? 0) - 1) return Position.Right;
    if (py <= ny + 1) return Position.Top;
    if (py >= ny + (node.measured?.height ?? 0) - 1) return Position.Bottom;
    return Position.Top;
}

/** Builds the coordinates and side metadata needed by getBezierPath. */
function getEdgeParams(source: InternalNode, target: InternalNode) {
    const si = getNodeIntersection(source, target);
    const ti = getNodeIntersection(target, source);
    return {
        sx: si.x, sy: si.y,
        tx: ti.x, ty: ti.y,
        sourcePos: getEdgePosition(source, si),
        targetPos: getEdgePosition(target, ti),
    };
}

/**
 * Custom edge that floats between node borders instead of fixed handles.
 * It recomputes endpoints from current node bounds so edges stay visually
 * attached when nodes are moved or resized.
 */
function FloatingEdge({id, source, target, markerEnd, markerStart, style, label}: EdgeProps) {
    const {sourceNode, targetNode} = useStore((s) => ({
        sourceNode: s.nodeLookup.get(source),
        targetNode: s.nodeLookup.get(target),
    }));

    if (!sourceNode || !targetNode) return null;

    const {sx, sy, tx, ty, sourcePos, targetPos} = getEdgeParams(sourceNode, targetNode);
    const [path, labelX, labelY] = getBezierPath({
        sourceX: sx, sourceY: sy, sourcePosition: sourcePos,
        targetX: tx, targetY: ty, targetPosition: targetPos,
    });

    return (
        <g className="react-flow__connection">
            <path id={id} className="react-flow__edge-path" d={path}
                  markerEnd={markerEnd as string} markerStart={markerStart as string}
                  style={style}/>
            {label && (
                <text x={labelX} y={labelY} className="react-flow__edge-text"
                      textAnchor="middle" dominantBaseline="central"
                      style={{fontSize: 12, fill: '#222'}}>
                    {label}
                </text>
            )}
        </g>
    );
}

/** Renders the preview line while creating a floating edge. */
function FloatingConnectionLine({toX, toY, fromPosition, toPosition, fromNode}: ConnectionLineComponentProps) {
    if (!fromNode) return null;

    const targetNode = {
        id: 'connection-target',
        measured: {width: 1, height: 1},
        internals: {positionAbsolute: {x: toX, y: toY}},
    } as unknown as InternalNode;

    const {sx, sy} = getEdgeParams(fromNode as unknown as InternalNode, targetNode);
    const [path] = getBezierPath({
        sourceX: sx, sourceY: sy, sourcePosition: fromPosition,
        targetX: toX, targetY: toY, targetPosition: toPosition,
    });

    return (
        <g>
            <path fill="none" stroke="#222" strokeWidth={1.5} className="animated" d={path}/>
            <circle cx={toX} cy={toY} fill="#fff" r={3} stroke="#222" strokeWidth={1.5}/>
        </g>
    );
}

const edgeTypes = {floating: FloatingEdge};

// ── Types ──────────────────────────────────────────────────────────────

/** Shape of the Vaadin `flowState` object shared with the Kotlin component. */
interface FlowState {
    nodes: Node[];
    edges: Edge[];
    fitView: boolean;
    snapToGrid: boolean;
    snapGridX: number;
    snapGridY: number;
    minZoom: number;
    maxZoom: number;
    nodesDraggable: boolean;
    nodesConnectable: boolean;
    elementsSelectable: boolean;
    backgroundVariant: string;
    showMiniMap: boolean;
    showControls: boolean;
    showBackground: boolean;
    edgesReconnectable: boolean;
    defaultEdgeType: string | undefined;
}

/** One-shot layout request sent by the server; counter distinguishes repeats. */
interface LayoutAction {
    algorithm: string;
    direction: string;
    counter: number;
}

/** One-shot export request sent by the server; counter distinguishes repeats. */
interface ExportAction {
    counter: number;
}

// ── Layout: Dagre ──────────────────────────────────────────────────────

const DEFAULT_NODE_WIDTH = 172;
const DEFAULT_NODE_HEIGHT = 36;

/** Runs Dagre on top-level nodes and preserves child/group node relationships. */
function layoutWithDagre(
    nodes: Node[],
    edges: Edge[],
    direction: string,
): { nodes: Node[]; edges: Edge[] } {
    const g = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}));
    const isHorizontal = direction === 'LR' || direction === 'RL';

    g.setGraph({rankdir: direction});

    // Only layout top-level nodes (skip children of group nodes)
    const topLevelNodes = nodes.filter((n) => !n.parentId);
    const childNodes = nodes.filter((n) => !!n.parentId);

    topLevelNodes.forEach((node) => {
        g.setNode(node.id, {
            width: node.width ?? node.measured?.width ?? DEFAULT_NODE_WIDTH,
            height: node.height ?? node.measured?.height ?? DEFAULT_NODE_HEIGHT,
        });
    });

    edges
        .filter((e) => topLevelNodes.some((n) => n.id === e.source) && topLevelNodes.some((n) => n.id === e.target))
        .forEach((edge) => {
            g.setEdge(edge.source, edge.target);
        });

    dagre.layout(g);

    const layoutedTopLevel = topLevelNodes.map((node) => {
        const pos = g.node(node.id);
        const w = node.width ?? node.measured?.width ?? DEFAULT_NODE_WIDTH;
        const h = node.height ?? node.measured?.height ?? DEFAULT_NODE_HEIGHT;
        return {
            ...node,
            sourcePosition: isHorizontal ? 'right' : 'bottom',
            targetPosition: isHorizontal ? 'left' : 'top',
            position: {x: pos.x - w / 2, y: pos.y - h / 2},
        } as Node;
    });

    return {nodes: [...layoutedTopLevel, ...childNodes], edges};
}

// ── Layout: ELK ────────────────────────────────────────────────────────

const elk = new ELK();

/** Maps the Kotlin layout direction enum names to ELK direction values. */
function toElkDirection(dir: string): string {
    switch (dir) {
        case 'LR':
            return 'RIGHT';
        case 'RL':
            return 'LEFT';
        case 'BT':
            return 'UP';
        default:
            return 'DOWN';
    }
}

/** Runs ELK layered layout, including children nested inside group nodes. */
async function layoutWithElk(
    nodes: Node[],
    edges: Edge[],
    direction: string,
): Promise<{ nodes: Node[]; edges: Edge[] }> {
    const isHorizontal = direction === 'LR' || direction === 'RL';

    // Separate top-level and child nodes
    const topLevelNodes = nodes.filter((n) => !n.parentId);
    const childNodes = nodes.filter((n) => !!n.parentId);

    // Group children by parent
    const childrenByParent = new Map<string, Node[]>();
    childNodes.forEach((child) => {
        const list = childrenByParent.get(child.parentId!) ?? [];
        list.push(child);
        childrenByParent.set(child.parentId!, list);
    });

    const graph = {
        id: 'root',
        layoutOptions: {
            'elk.algorithm': 'layered',
            'elk.direction': toElkDirection(direction),
            'elk.layered.spacing.nodeNodeBetweenLayers': '100',
            'elk.spacing.nodeNode': '80',
        },
        children: topLevelNodes.map((node) => {
            const w = node.width ?? node.measured?.width ?? 150;
            const h = node.height ?? node.measured?.height ?? 50;
            const elkNode: any = {id: node.id, width: w, height: h};

            // If this is a group node, add its children as ELK children
            const kids = childrenByParent.get(node.id);
            if (kids) {
                elkNode.children = kids.map((child) => ({
                    id: child.id,
                    width: child.width ?? child.measured?.width ?? 150,
                    height: child.height ?? child.measured?.height ?? 50,
                }));
                elkNode.layoutOptions = {
                    'elk.algorithm': 'layered',
                    'elk.direction': toElkDirection(direction),
                    'elk.padding': '[top=50,left=20,bottom=20,right=20]',
                };
            }

            return elkNode;
        }),
        edges: edges.map((edge) => ({
            id: edge.id,
            sources: [edge.source],
            targets: [edge.target],
        })),
    };

    const layouted = await elk.layout(graph);

    const positionMap = new Map<string, { x: number; y: number }>();
    layouted.children?.forEach((elkNode: any) => {
        positionMap.set(elkNode.id, {x: elkNode.x ?? 0, y: elkNode.y ?? 0});
        // Map child positions
        elkNode.children?.forEach((elkChild: any) => {
            positionMap.set(elkChild.id, {x: elkChild.x ?? 0, y: elkChild.y ?? 0});
        });
    });

    const layoutedNodes = nodes.map((node) => {
        const pos = positionMap.get(node.id);
        return {
            ...node,
            position: pos ?? node.position,
            sourcePosition: isHorizontal ? 'right' : 'bottom',
            targetPosition: isHorizontal ? 'left' : 'top',
        } as Node;
    });

    return {nodes: layoutedNodes, edges};
}

// ── Export to image ────────────────────────────────────────────────────

/** Starts a browser download for the PNG data URL produced by html-to-image. */
function downloadImage(dataUrl: string) {
    const a = document.createElement('a');
    a.setAttribute('download', 'reactflow.png');
    a.setAttribute('href', dataUrl);
    a.click();
}

// ── Null sanitization ──────────────────────────────────────────────────

/**
 * Vaadin serializes Kotlin null as JSON null, but React Flow distinguishes
 * null from undefined in many internal checks (e.g. isCoordinateExtent).
 * Convert all null values to undefined so React Flow falls back to defaults.
 */
function stripNulls<T extends Record<string, any>>(obj: T): T {
    const cleaned: any = {};
    for (const key in obj) {
        cleaned[key] = obj[key] === null ? undefined : obj[key];
    }
    return cleaned;
}

// ── Background variant helper ──────────────────────────────────────────

/** Converts the string stored in Kotlin state to React Flow's enum value. */
function getBackgroundVariant(variant: string): BackgroundVariant | undefined {
    switch (variant) {
        case 'dots':
            return BackgroundVariant.Dots;
        case 'lines':
            return BackgroundVariant.Lines;
        case 'cross':
            return BackgroundVariant.Cross;
        default:
            return BackgroundVariant.Dots;
    }
}

// ── Inner React component ──────────────────────────────────────────────

/**
 * Inner component that owns React Flow hooks and bridges React Flow callbacks
 * back to Vaadin state updates.
 */
function ReactFlowWrapper({
                              flowState,
                              setFlowState,
                              layoutAction,
                              exportAction,
                          }: {
    flowState: FlowState | undefined;
    setFlowState: (state: FlowState) => void;
    layoutAction: LayoutAction | undefined;
    exportAction: ExportAction | undefined;
}) {
    const {fitView, screenToFlowPosition} = useReactFlow();
    const nodesInitialized = useNodesInitialized();
    const prevLayoutCounter = useRef(0);
    const prevExportCounter = useRef(0);
    const [extentsReady, setExtentsReady] = useState(false);
    const connectingFrom = useRef<string | null>(null);
    let nodeIdCounter = useRef(Date.now());

    // Defer extent: 'parent' until nodes are measured to avoid clampPosition crash
    useEffect(() => {
        if (nodesInitialized && !extentsReady) {
            setExtentsReady(true);
        }
    }, [nodesInitialized, extentsReady]);

    // Handle layout action
    useEffect(() => {
        if (!layoutAction || !flowState) return;
        if (layoutAction.counter === prevLayoutCounter.current) return;
        prevLayoutCounter.current = layoutAction.counter;

        const {algorithm, direction} = layoutAction;

        if (algorithm === 'DAGRE') {
            const result = layoutWithDagre(flowState.nodes, flowState.edges, direction);
            setFlowState({...flowState, ...result});
            window.requestAnimationFrame(() => fitView({duration: 300}));
        } else if (algorithm === 'ELK') {
            layoutWithElk(flowState.nodes, flowState.edges, direction).then((result) => {
                setFlowState({...flowState, ...result});
                window.requestAnimationFrame(() => fitView({duration: 300}));
            });
        }
    }, [layoutAction, flowState, setFlowState, fitView]);

    // Handle export action
    useEffect(() => {
        if (!exportAction) return;
        if (exportAction.counter === prevExportCounter.current) return;
        prevExportCounter.current = exportAction.counter;

        const viewport = document.querySelector('.react-flow__viewport') as HTMLElement;
        if (viewport) {
            toPng(viewport, {backgroundColor: '#ffffff'}).then(downloadImage);
        }
    }, [exportAction]);

    if (!flowState) {
        return <div>Loading...</div>;
    }

    const {
        nodes,
        edges,
        fitView: fitViewProp,
        snapToGrid,
        snapGridX,
        snapGridY,
        minZoom,
        maxZoom,
        nodesDraggable,
        nodesConnectable,
        elementsSelectable,
        backgroundVariant,
        showMiniMap,
        showControls,
        showBackground,
        edgesReconnectable,
        defaultEdgeType,
    } = flowState;

    // Persist node drags, selections, deletions and dimension changes to Vaadin.
    const onNodesChange: OnNodesChange = useCallback(
        (changes: NodeChange[]) => {
            const updatedNodes = applyNodeChanges(changes, nodes);
            setFlowState({...flowState, nodes: updatedNodes});
        },
        [flowState, setFlowState, nodes],
    );

    // Persist edge selection/deletion changes to Vaadin.
    const onEdgesChange: OnEdgesChange = useCallback(
        (changes: EdgeChange[]) => {
            const updatedEdges = applyEdgeChanges(changes, edges);
            setFlowState({...flowState, edges: updatedEdges});
        },
        [flowState, setFlowState, edges],
    );

    // Create a new edge when the user connects two handles.
    const onConnect: OnConnect = useCallback(
        (connection: Connection) => {
            const newEdge: Edge = {
                id: `e-${connection.source}-${connection.target}-${Date.now()}`,
                source: connection.source!,
                target: connection.target!,
                sourceHandle: connection.sourceHandle ?? undefined,
                targetHandle: connection.targetHandle ?? undefined,
            };
            const updatedEdges = addEdge(newEdge, edges);
            setFlowState({...flowState, edges: updatedEdges});
        },
        [flowState, setFlowState, edges],
    );

    // Update an existing edge when the user drags one endpoint to another handle.
    const onReconnect = useCallback(
        (oldEdge: Edge, newConnection: Connection) => {
            const updatedEdges = reconnectEdge(oldEdge, newConnection, edges);
            setFlowState({...flowState, edges: updatedEdges});
        },
        [flowState, setFlowState, edges],
    );

    // Remember the source node so dropping on empty canvas can create a new node.
    const onConnectStart = useCallback((_: any, params: { nodeId: string | null }) => {
        connectingFrom.current = params.nodeId;
    }, []);

    const onConnectEnd = useCallback(
        (event: MouseEvent | TouchEvent) => {
            if (!connectingFrom.current || !flowState) return;

            // Check if dropped on a node/handle — if so, onConnect already handled it
            const target = (event as MouseEvent).target as HTMLElement;
            if (target?.closest('.react-flow__handle') || target?.closest('.react-flow__node')) return;

            const clientX = 'changedTouches' in event ? event.changedTouches[0].clientX : (event as MouseEvent).clientX;
            const clientY = 'changedTouches' in event ? event.changedTouches[0].clientY : (event as MouseEvent).clientY;
            const position = screenToFlowPosition({x: clientX, y: clientY});

            const label = prompt('New node label:');
            if (label === null) {
                connectingFrom.current = null;
                return;
            }

            const newId = `auto-${++nodeIdCounter.current}`;
            const newNode: Node = {
                id: newId,
                type: 'default',
                position,
                data: {label: label || newId},
            };
            const newEdge: Edge = {
                id: `e-${connectingFrom.current}-${newId}`,
                source: connectingFrom.current,
                target: newId,
                type: defaultEdgeType ?? undefined,
            };

            setFlowState({
                ...flowState,
                nodes: [...nodes, newNode],
                edges: [...edges, newEdge],
            });
            connectingFrom.current = null;
        },
        [flowState, setFlowState, nodes, edges, screenToFlowPosition, defaultEdgeType],
    );

    // Provide a minimal built-in label editor for demo and simple use cases.
    const onNodeDoubleClick = useCallback(
        (_: React.MouseEvent, node: Node) => {
            const currentLabel = (node.data?.label as string) ?? '';
            const newLabel = prompt('Edit node label:', currentLabel);
            if (newLabel === null) return;

            const updatedNodes = nodes.map((n) =>
                n.id === node.id ? {...n, data: {...n.data, label: newLabel}} : n,
            );
            setFlowState({...flowState, nodes: updatedNodes});
        },
        [flowState, setFlowState, nodes],
    );

    // Sanitize: convert all Kotlin nulls to undefined, defer extent:'parent'
    const safeNodes = nodes.map((n) => {
        const clean = stripNulls(n);
        if (clean.extent === 'parent' && !extentsReady) {
            return {...clean, extent: undefined};
        }
        return clean;
    });
    const safeEdges = edges.map(stripNulls);

    const bgVariant = getBackgroundVariant(backgroundVariant);

    return (
        <div style={{width: '100%', height: '100%'}}>
            <ReactFlow
                nodes={safeNodes}
                edges={safeEdges}
                nodeTypes={nodeTypes}
                edgeTypes={edgeTypes}
                connectionLineComponent={FloatingConnectionLine}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                onConnectStart={onConnectStart}
                onConnectEnd={onConnectEnd}
                onNodeDoubleClick={onNodeDoubleClick}
                onReconnect={onReconnect}
                edgesReconnectable={edgesReconnectable}
                defaultEdgeOptions={defaultEdgeType ? {type: defaultEdgeType} : undefined}
                fitView={fitViewProp}
                snapToGrid={snapToGrid}
                snapGrid={[snapGridX, snapGridY]}
                minZoom={minZoom}
                maxZoom={maxZoom}
                nodesDraggable={nodesDraggable}
                nodesConnectable={nodesConnectable}
                elementsSelectable={elementsSelectable}
            >
                {showBackground && bgVariant != null && (
                    <Background variant={bgVariant} gap={snapGridX} size={1}/>
                )}
                {showControls && <Controls/>}
                {showMiniMap && <MiniMap zoomable pannable/>}
            </ReactFlow>
        </div>
    );
}

// ── Vaadin adapter element ─────────────────────────────────────────────

/** Custom element registered for the Kotlin @Tag("vaadin-react-flow"). */
class VaadinReactFlowElement extends ReactAdapterElement {
    protected override render(hooks: RenderHooks): ReactElement | null {
        const [flowState, setFlowState] = hooks.useState<FlowState>('flowState');
        const [layoutAction] = hooks.useState<LayoutAction>('layoutAction');
        const [exportAction] = hooks.useState<ExportAction>('exportAction');

        return (
            <ReactFlowProvider>
                <ReactFlowWrapper
                    flowState={flowState}
                    setFlowState={setFlowState}
                    layoutAction={layoutAction}
                    exportAction={exportAction}
                />
            </ReactFlowProvider>
        );
    }
}

customElements.define('vaadin-react-flow', VaadinReactFlowElement);
