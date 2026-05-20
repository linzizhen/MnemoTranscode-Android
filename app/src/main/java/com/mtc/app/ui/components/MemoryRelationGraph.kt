package com.mtc.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mtc.app.domain.model.GraphEdge
import com.mtc.app.domain.model.GraphNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

private val NODE_COLORS = mapOf(
    "Person" to Color(0xFFFFFEF3C7),
    "Event" to Color(0xFFCCFBF1),
    "Emotion" to Color(0xFFFCE7F3),
    "SemanticFact" to Color(0xFFE0E7FF)
)
private val SELECTED_COLOR = Color(0xFFF59E0B)
private val HL_COLORS = listOf(Color(0xFFFDE68A), Color(0xFF5EEAD4), Color(0xFFF9A8D4), Color(0xFF6EE7B7))
private val EDGE_COLORS = mapOf(
    "TEMPORAL_NEXT" to Color(0xFF0D9488),
    "CAUSED_BY" to Color(0xFFB45309),
    "RELATED_TO" to Color(0xFF64748B),
    "EMOTIONALLY_LINKED" to Color(0xFFC026D3),
    "SUPPORTS" to Color(0xFF2563EB),
    "COACTIVATED_WITH" to Color(0xFF94A3B8)
)
private val EDGE_LABELS = mapOf(
    "TEMPORAL_NEXT" to "时间先后",
    "CAUSED_BY" to "因果归因",
    "RELATED_TO" to "主题相关",
    "EMOTIONALLY_LINKED" to "情感关联",
    "SUPPORTS" to "支撑印证",
    "COACTIVATED_WITH" to "共现关联"
)
private const val NODE_RADIUS_NORMAL = 8f
private const val NODE_RADIUS_SELECTED = 12f
private const val MIN_ZOOM = 0.15f
private const val MAX_ZOOM = 8f
private const val FORCE_TICK_MS = 16L
private const val FORCE_IDEAL_DIST = 180f

private data class FloatRect(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    val width get() = maxX - minX
    val height get() = maxY - minY
    fun isValid() = width > 0f && height > 0f
}

// screenX = (canvasX + centerX) * scale + panX
// canvasX = (screenX - panX) / scale - centerX
// pan 初始 = (0, 0)：画布原点映射到屏幕中心，视图变换只通过 scale
// 缩放/平移围绕 screen center 统一计算，确保 pan/scale 始终一致
// ============================================================
private class GraphState {
    val nodePositions = mutableStateMapOf<String, Offset>()
    /** 散布后的初始边界，用于 centerOnContent 保持结果稳定 */
    var initialBounds: FloatRect? = null; private set

    /** canvasSize 必须是 MutableState<IntSize>，LaunchedEffect 才能观察变化 */
    var canvasSize by mutableStateOf(IntSize.Zero)
        private set

    /** onSizeChanged 检测到首次有效尺寸时设为 true，触发初始化 */
    var hasValidSize by mutableStateOf(false)
        private set

    var scale: Float = 1f
    var panX: Float = 0f; var panY: Float = 0f

    val centerX get() = if (canvasSize.width > 0) canvasSize.width / 2f else 0f
    val centerY get() = if (canvasSize.height > 0) canvasSize.height / 2f else 0f

    fun updateCanvasSize(size: IntSize) {
        if (size.width <= 0 || size.height <= 0) return
        if (size == canvasSize) {
            if (!hasValidSize) hasValidSize = true
            return
        }
        val oldCx = centerX; val oldCy = centerY
        canvasSize = size
        hasValidSize = true
        // resize 后同步视图中心（canvas 原点仍映射到屏幕中心）
        panX += centerX - oldCx; panY += centerY - oldCy
    }

    fun screenToCanvas(sx: Float, sy: Float): Offset {
        val s = if (scale > 0.01f) scale else 1f
        return Offset((sx - panX) / s - centerX, (sy - panY) / s - centerY)
    }

    fun canvasToScreen(cx: Float, cy: Float) = Offset((cx + centerX) * scale + panX, (cy + centerY) * scale + panY)

    /**
     * 围绕屏幕中心缩放：无论当前 pan 值如何，缩放焦点始终是屏幕中心点。
     * 这确保"放大向左上角/缩小向右下角"的错误不再出现。
     * 计算：缩放后屏幕中心点位置不变 → (width/2 - panX) / scale = (width/2 - newPanX) / newScale
     */
    fun zoomAroundScreenCenter(newScale: Float) {
        val clamped = newScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (abs(clamped - scale) < 0.0001f) return
        val w = canvasSize.width.toFloat(); val h = canvasSize.height.toFloat()
        if (w <= 0 || h <= 0) return
        val cx = w / 2f; val cy = h / 2f
        // 焦点（屏幕中心）在缩放前后必须映射到画布同一点
        panX = cx - (cx - panX) * clamped / scale
        panY = cy - (cy - panY) * clamped / scale
        scale = clamped
    }

    /** 计算节点位置的实时边界（每次调用时重新计算） */
    fun computeCurrentBounds(): FloatRect? {
        val xs = nodePositions.values.map { it.x }
        val ys = nodePositions.values.map { it.y }
        val minX = xs.minOrNull() ?: return null
        val minY = ys.minOrNull() ?: return null
        val maxX = xs.maxOrNull() ?: return null
        val maxY = ys.maxOrNull() ?: return null
        return FloatRect(minX, minY, maxX, maxY)
    }

    /**
     * 居中到当前所有节点的实时边界（包含力导向后的最新位置）。
     * 先计算节点边界的几何中心，然后平移画布使该中心对准屏幕中心。
     */
    fun centerOnContent(padding: Float = 0.15f) {
        if (nodePositions.isEmpty() || canvasSize.width <= 0) return
        val bounds = computeCurrentBounds() ?: return
        if (!bounds.isValid()) return
        val w = canvasSize.width.toFloat(); val h = canvasSize.height.toFloat()
        // 内容宽高
        val cw = bounds.width; val ch = bounds.height
        if (cw <= 0f || ch <= 0f) return
        // 计算缩放：内容填满 padding 区域
        val scaleX = w * (1 - 2 * padding) / cw
        val scaleY = h * (1 - 2 * padding) / ch
        val newScale = minOf(scaleX, scaleY).coerceIn(MIN_ZOOM, MAX_ZOOM)
        // 内容边界中心（画布坐标）
        val contentCx = (bounds.minX + bounds.maxX) / 2f
        val contentCy = (bounds.minY + bounds.maxY) / 2f
        // 屏幕中心（像素坐标）
        val screenCx = w / 2f; val screenCy = h / 2f
        // 计算新的 pan：使内容中心映射到屏幕中心
        // screenCx = (contentCx + centerX) * newScale + newPanX
        // → newPanX = screenCx - (contentCx + centerX) * newScale
        panX = screenCx - (contentCx + centerX) * newScale
        panY = screenCy - (contentCy + centerY) * newScale
        scale = newScale
    }

    fun saveInitialBounds() {
        val xs = nodePositions.values.map { it.x }
        val ys = nodePositions.values.map { it.y }
        initialBounds = FloatRect(xs.minOrNull() ?: 0f, ys.minOrNull() ?: 0f, xs.maxOrNull() ?: 0f, ys.maxOrNull() ?: 0f)
    }

    /** 重置初始化状态（切换全屏等场景需要重新初始化） */
    fun resetInitialization() {
        hasValidSize = false
    }
}

private data class HighlightResult(val chainNodes: Set<String>, val nodeColors: Map<String, Color>)

private fun computeHighlight(selectedId: String?, edges: List<GraphEdge>, nodeTypeMap: Map<String, String>): HighlightResult {
    if (selectedId == null || edges.isEmpty()) {
        return HighlightResult(emptySet(), nodeTypeMap.mapValues { NODE_COLORS[it.value] ?: Color(0xFFE2E8F0) })
    }
    val chain: MutableSet<String> = mutableSetOf(selectedId)
    val fwd: MutableMap<String, String> = mutableMapOf(); val bwd: MutableMap<String, String> = mutableMapOf()
    for (e in edges.filter { it.edgeType == "TEMPORAL_NEXT" }) { fwd[e.fromId] = e.toId; bwd[e.toId] = e.fromId }
    var cur = selectedId; while (fwd[cur] != null) { cur = fwd[cur]!!; chain.add(cur) }
    cur = selectedId; while (bwd[cur] != null) { cur = bwd[cur]!!; chain.add(cur) }
    val nodeColors = mutableMapOf<String, Color>()
    for ((id, _) in nodeTypeMap) {
        val color = when {
            id == selectedId -> SELECTED_COLOR
            id in chain -> HL_COLORS[(chain.toList().indexOf(id) + 1) % HL_COLORS.size]
            else -> NODE_COLORS[nodeTypeMap[id]] ?: Color(0xFFE2E8F0)
        }
        nodeColors[id] = color
    }
    return HighlightResult(chain, nodeColors)
}

private fun spreadNodesRadially(nodes: List<GraphNode>, anchorId: String?, canvasW: Float, canvasH: Float): Map<String, Offset> {
    val baseR = (minOf(canvasW, canvasH) * 0.30f).toFloat()
    val jitterA = (minOf(canvasW, canvasH) * 0.10f).toFloat()
    val anchorIdx = nodes.indexOfFirst { it.id == anchorId }.takeIf { it >= 0 } ?: 0
    return nodes.associate { node ->
        val idx = nodes.indexOf(node)
        val angle = (2.0 * PI * idx / maxOf(nodes.size, 1) - PI).toFloat()
        val isAnchor = idx == anchorIdx
        val r = if (isAnchor) baseR * 0.2f else baseR + (Random.nextFloat() - 0.5f) * jitterA
        val jx = (Random.nextFloat() - 0.5f) * jitterA * 0.5f
        val jy = (Random.nextFloat() - 0.5f) * jitterA * 0.5f
        node.id to Offset(r * cos(angle) + jx, r * sin(angle) + jy)
    }
}

private fun forceTick(nodeIds: List<String>, edgePairs: List<Pair<Int, Int>>, positions: MutableMap<String, Offset>, alpha: Float, draggingNodeId: String?) {
    val n = nodeIds.size
    if (n == 0) return
    val IDEAL = FORCE_IDEAL_DIST
    for (i in 0 until n) {
        for (j in i + 1 until n) {
            val nidI = nodeIds[i]; val nidJ = nodeIds[j]
            val pa = positions[nidI] ?: continue; val pb = positions[nidJ] ?: continue
            var dx = pa.x - pb.x; var dy = pa.y - pb.y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
            val f = (IDEAL * IDEAL / dist - dist * 0.5f) * alpha * 0.1f
            dx = (dx / dist) * f; dy = (dy / dist) * f
            if (nidI != draggingNodeId) positions[nidI] = Offset(pa.x + dx, pa.y + dy)
            if (nidJ != draggingNodeId) positions[nidJ] = Offset(pb.x - dx, pb.y - dy)
        }
    }
    for ((fi, ti) in edgePairs) {
        if (fi >= n || ti >= n) continue
        val nidF = nodeIds[fi]; val nidT = nodeIds[ti]
        val pa = positions[nidF] ?: continue; val pb = positions[nidT] ?: continue
        var dx = pb.x - pa.x; var dy = pb.y - pa.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
        val f = (dist - IDEAL) * 0.008f * alpha
        dx = (dx / dist) * f; dy = (dy / dist) * f
        if (nidF != draggingNodeId) positions[nidF] = Offset(pa.x + dx, pa.y + dy)
        if (nidT != draggingNodeId) positions[nidT] = Offset(pb.x - dx, pb.y - dy)
    }
}

/**
 * 从 EngramNode.content 取前 80 字符，后端已截断，无需前端二次处理。
 */

@Composable
fun MemoryRelationGraph(
    nodes: List<GraphNode>, edges: List<GraphEdge>, selectedNodeId: String?,
    onNodeSelected: (String?) -> Unit, onNodeDragEnd: (GraphNode, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var forceOn by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    // 普通视图和全屏视图各自独立的 GraphState，互不干扰
    val normalState = remember { GraphState() }
    val fullscreenState = remember { GraphState() }

    val nodeTypeMap = remember(nodes) { nodes.associate { it.id to it.nodeType } }
    val edgePairs = remember(nodes, edges) {
        val ids = nodes.map { it.id }
        val idxMap: MutableMap<String, Int> = mutableMapOf()
        ids.forEachIndexed { i, id -> idxMap[id] = i }
        edges.mapNotNull { e -> val fi = idxMap[e.fromId]; val ti = idxMap[e.toId]; if (fi != null && ti != null) Pair(fi, ti) else null }
    }
    val hl = remember(selectedNodeId, edges, nodeTypeMap) { computeHighlight(selectedNodeId, edges, nodeTypeMap) }

    // ---- 全屏 Dialog（使用独立的 fullscreenState） ----
    if (isFullscreen) {
        Dialog(onDismissRequest = { isFullscreen = false }) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("关系网络", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { forceOn = !forceOn },
                                label = { Text(if (forceOn) "力导向开" else "力导向关", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(if (forceOn) Icons.Default.Timeline else Icons.Default.Pause, null, Modifier.size(16.dp)) }
                            )
                            IconButton(onClick = { isFullscreen = false }) { Icon(Icons.Default.Close, "关闭") }
                        }
                    }
                    GraphCanvas(
                        graphState = fullscreenState, nodes = nodes, edges = edges,
                        selectedNodeId = selectedNodeId, hl = hl,
                        forceOn = forceOn, edgePairs = edgePairs,
                        onNodeSelected = onNodeSelected,
                        onNodeDragEnd = { nodeId, pos ->
                            nodes.find { it.id == nodeId }?.let { onNodeDragEnd(it, pos.x, pos.y) }
                        }
                    )
                }
            }
        }
    }

    // ---- 普通视图 ----
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((type, color) in NODE_COLORS.entries.take(3)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
                        Spacer(Modifier.width(4.dp))
                        Text(when (type) { "Person" -> "人物"; "Event" -> "事件"; "Emotion" -> "情感"; else -> type },
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(
                    onClick = { forceOn = !forceOn },
                    label = { Text(if (forceOn) "力导向开" else "力导向关", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(if (forceOn) Icons.Default.Timeline else Icons.Default.Pause, null, Modifier.size(16.dp)) }
                )
                IconButton(onClick = { isFullscreen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Fullscreen, "全屏", Modifier.size(20.dp))
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1.2f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            GraphCanvas(
                graphState = normalState, nodes = nodes, edges = edges,
                selectedNodeId = selectedNodeId, hl = hl,
                forceOn = forceOn, edgePairs = edgePairs,
                onNodeSelected = onNodeSelected,
                onNodeDragEnd = { nodeId, pos ->
                    nodes.find { it.id == nodeId }?.let { onNodeDragEnd(it, pos.x, pos.y) }
                }
            )
        }
        selectedNodeId?.let { sid ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val label = nodes.find { it.id == sid }?.label ?: ""
                    Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    val types = edges.filter { it.fromId == sid || it.toId == sid }.map { it.edgeType }.distinct()
                    Text(types.joinToString(" · ") { EDGE_LABELS[it] ?: it },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text("点击结点可点亮时间链；拖动结点松手后会固定在该位置",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
    }
}

// ============================================================
// 画布组件（每个视图独立持有自己的状态和力循环）
// ============================================================
@Composable
private fun GraphCanvas(
    graphState: GraphState, nodes: List<GraphNode>, edges: List<GraphEdge>,
    selectedNodeId: String?, hl: HighlightResult,
    forceOn: Boolean, edgePairs: List<Pair<Int, Int>>,
    onNodeSelected: (String?) -> Unit, onNodeDragEnd: (String, Offset) -> Unit
) {
    val hitRadiusPx = with(LocalDensity.current) { 18.dp.toPx() }
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var draggingNodeId by remember { mutableStateOf<String?>(null) }
    var localAlpha by remember { mutableFloatStateOf(1f) }
    var forceJob by remember { mutableStateOf<Job?>(null) }

    val draggingNodeIdRef by rememberUpdatedState(draggingNodeId)
    val forceOnRef by rememberUpdatedState(forceOn)

    /**
     * alphaDecaying = true 时 forceTick 正常衰减 alpha。
     * 手势期间设为 false，阻止 alpha 在交互过程中耗尽。
     * 与 isDraggingRef 结合：手势结束后重新启动衰减。
     */
    var alphaDecaying by remember { mutableStateOf(false) }

    /** 启动力模拟循环 */
    fun startForceLoop() {
        forceJob?.cancel()
        forceJob = scope.launch {
            while (isActive && localAlpha > 0.001f) {
                // isDraggingRef：被拖拽节点不参与力计算（保持固定位置）
                // alphaDecaying：手势期间暂停衰减（交互结束后恢复衰减）
                forceTick(nodes.map { it.id }, edgePairs, graphState.nodePositions, localAlpha, draggingNodeIdRef)
                if (alphaDecaying) localAlpha *= 0.985f
                delay(FORCE_TICK_MS)
            }
        }
    }

    // 初始化散布
    LaunchedEffect(nodes.size, graphState.hasValidSize) {
        if (nodes.isEmpty()) { graphState.nodePositions.clear(); return@LaunchedEffect }
        if (!graphState.hasValidSize) return@LaunchedEffect
        if (graphState.nodePositions.isNotEmpty()) return@LaunchedEffect
        localAlpha = 1f
        alphaDecaying = true
        val anchor = nodes.find { it.nodeType == "Person" }?.id ?: nodes.firstOrNull()?.id
        val seeded = spreadNodesRadially(nodes, anchor,
            graphState.canvasSize.width.toFloat(), graphState.canvasSize.height.toFloat())
        graphState.nodePositions.clear()
        graphState.nodePositions.putAll(seeded)
        graphState.saveInitialBounds()
        graphState.centerOnContent()
        if (forceOnRef) startForceLoop()
    }

    // forceOn 切换
    LaunchedEffect(forceOn) {
        if (forceOn) {
            localAlpha = 0.5f
            alphaDecaying = true
            startForceLoop()
        } else {
            forceJob?.cancel()
            forceJob = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .onSizeChanged { size -> graphState.updateCanvasSize(size) }
    ) {
        val textMeasurer = rememberTextMeasurer()
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(nodes.map { it.id }) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val hitCanvas = graphState.screenToCanvas(down.position.x, down.position.y)
                        val hitScale = if (graphState.scale > 0.01f) graphState.scale else 1f
                        var hitNodeId: String? = null
                        for ((id, pos) in graphState.nodePositions) {
                            val dx = hitCanvas.x - pos.x; val dy = hitCanvas.y - pos.y
                            if (sqrt(dx * dx + dy * dy) <= hitRadiusPx / hitScale) { hitNodeId = id; break }
                        }

                        if (hitNodeId != null) {
                            // ========== 单指拖动节点 ==========
                            down.consume()
                            val nid = hitNodeId
                            var lastX = down.position.x; var lastY = down.position.y
                            var moved = false
                            isDragging = true; draggingNodeId = nid
                            alphaDecaying = false  // 暂停 alpha 衰减，保持力导引擎活跃

                            while (true) {
                                val ev = awaitPointerEvent()
                                val active = ev.changes.filter { it.pressed }
                                if (active.isEmpty()) {
                                    ev.changes.forEach { it.consume() }
                                    isDragging = false; draggingNodeId = null
                                    alphaDecaying = true  // 恢复衰减
                                    val pos = graphState.nodePositions[nid] ?: Offset(0f, 0f)
                                    if (moved) {
                                        onNodeDragEnd(nid, pos)
                                        onNodeSelected(nid)
                                    }
                                    break
                                }
                                if (active.size >= 2) {
                                    ev.changes.forEach { it.consume() }
                                    val p0 = active[0]; val p1 = active[1]
                                    val startScale = graphState.scale
                                    while (true) {
                                        val ev2 = awaitPointerEvent()
                                        val ap2 = ev2.changes.filter { it.pressed }
                                        if (ap2.size < 2) { ev2.changes.forEach { it.consume() }; break }
                                        val c0 = ap2.find { it.id == p0.id } ?: ap2[0]
                                        val c1 = ap2.find { it.id == p1.id } ?: ap2[1]
                                        val newDist = sqrt((c1.position.x - c0.position.x).pow(2) + (c1.position.y - c0.position.y).pow(2))
                                        val oldDist = sqrt((p1.position.x - p0.position.x).pow(2) + (p1.position.y - p0.position.y).pow(2))
                                        if (newDist > 0f && oldDist > 0f) graphState.zoomAroundScreenCenter(startScale * newDist / oldDist)
                                        ev2.changes.forEach { it.consume() }
                                    }
                                } else {
                                    val c = active.first()
                                    if (!c.pressed) continue
                                    c.consume()
                                    val dx = (c.position.x - lastX) / graphState.scale
                                    val dy = (c.position.y - lastY) / graphState.scale
                                    // 真正的拖动：屏幕像素移动 > 5px
                                    if (sqrt((c.position.x - down.position.x).pow(2) + (c.position.y - down.position.y).pow(2)) > 5f) moved = true
                                    lastX = c.position.x; lastY = c.position.y
                                    val cur = graphState.nodePositions[nid] ?: continue
                                    graphState.nodePositions[nid] = Offset(cur.x + dx, cur.y + dy)
                                }
                            }
                        } else {
                            // ========== 单指平移 / 双指缩放 ==========
                            down.consume()
                            var lastX = down.position.x; var lastY = down.position.y
                            var moved = false
                            alphaDecaying = false  // 手势期间暂停衰减

                            while (true) {
                                val ev = awaitPointerEvent()
                                val active = ev.changes.filter { it.pressed }
                                if (active.isEmpty()) {
                                    ev.changes.forEach { it.consume() }
                                    alphaDecaying = true  // 恢复衰减
                                    if (!moved) onNodeSelected(null)
                                    break
                                }
                                if (active.size >= 2) {
                                    ev.changes.forEach { it.consume() }
                                    val p0 = active[0]; val p1 = active[1]
                                    val oldDist = sqrt((p1.position.x - p0.position.x).pow(2) + (p1.position.y - p0.position.y).pow(2))
                                    val startScale = graphState.scale
                                    while (true) {
                                        val ev2 = awaitPointerEvent()
                                        val ap2 = ev2.changes.filter { it.pressed }
                                        if (ap2.size < 2) { ev2.changes.forEach { it.consume() }; break }
                                        val c0 = ap2.find { it.id == p0.id } ?: ap2[0]
                                        val c1 = ap2.find { it.id == p1.id } ?: ap2[1]
                                        val newDist = sqrt((c1.position.x - c0.position.x).pow(2) + (c1.position.y - c0.position.y).pow(2))
                                        if (newDist > 0f && oldDist > 0f) graphState.zoomAroundScreenCenter(startScale * newDist / oldDist)
                                        ev2.changes.forEach { it.consume() }
                                    }
                                } else {
                                    val c = active.first()
                                    if (!c.pressed) continue
                                    c.consume()
                                    val dx = c.position.x - lastX; val dy = c.position.y - lastY
                                    if (dx.absoluteValue > 3f || dy.absoluteValue > 3f) moved = true
                                    lastX = c.position.x; lastY = c.position.y
                                    graphState.panX += dx; graphState.panY += dy
                                }
                            }
                        }
                    }
                }
        ) {
            val s = graphState.scale; val px = graphState.panX; val py = graphState.panY
            val cx = graphState.centerX; val cy = graphState.centerY
            fun sx(logicX: Float) = (logicX + cx) * s + px
            fun sy(logicY: Float) = (logicY + cy) * s + py
            // 字号随缩放自适应（网页版公式：max(8, 11 / scale)）
            val labelFontSizePx = (11f / s.coerceAtLeast(0.1f)).coerceIn(8f, 22f)
            val labelStyle = TextStyle(fontSize = labelFontSizePx.sp, fontWeight = FontWeight.Medium, color = onSurfaceColor.copy(alpha = 0.80f))

            for (edge in edges) {
                val fp = graphState.nodePositions[edge.fromId]; val tp = graphState.nodePositions[edge.toId]
                if (fp == null || tp == null) continue
                val isHl = selectedNodeId != null && (selectedNodeId == edge.fromId || selectedNodeId == edge.toId)
                val ec = EDGE_COLORS[edge.edgeType] ?: Color(0xFF94A3B8)
                val alphaVal = if (selectedNodeId == null) 0.7f else if (isHl) 1f else 0.12f
                val sw = if (isHl) 2.5f else (0.5f + edge.weight) * s
                drawLine(ec.copy(alpha = alphaVal), Offset(sx(fp.x), sy(fp.y)), Offset(sx(tp.x), sy(tp.y)), strokeWidth = sw.coerceAtLeast(0.3f))
            }

            for ((id, pos) in graphState.nodePositions) {
                val isSel = id == selectedNodeId
                val r = if (isSel) NODE_RADIUS_SELECTED else NODE_RADIUS_NORMAL
                val sr = r * s
                val nodeColor = hl.nodeColors[id] ?: Color(0xFFE2E8F0)
                val screenX = sx(pos.x); val screenY = sy(pos.y)
                if (isSel) drawCircle(SELECTED_COLOR.copy(alpha = 0.25f), radius = sr + 4f * s, center = Offset(screenX, screenY))
                drawCircle(nodeColor, radius = sr, center = Offset(screenX, screenY))
                drawCircle(if (isSel) SELECTED_COLOR.copy(alpha = 0.6f) else Color(0xFF6EE7B7).copy(alpha = 0.4f), radius = sr, center = Offset(screenX, screenY), style = Stroke(width = if (isSel) 2f * s else 0.8f * s))

                // 标签：截取前4字 + 省略号，水平居中于节点下方（网页版一致布局）
                val nodeLabel = nodes.find { it.id == id }?.label ?: ""
                if (nodeLabel.isNotBlank()) {
                    val raw = nodeLabel
                    val txt = if (raw.length > 4) raw.take(4) + "…" else raw
                    val measured = textMeasurer.measure(txt, labelStyle)
                    val lx = screenX - measured.size.width / 2f
                    val ly = screenY + sr + 2f
                    drawText(measured, topLeft = Offset(lx, ly))
                }
            }
        }

        // 缩放和居中按钮列
        Column(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val btnSize = 36.dp; val iconSz = 18.dp
            @Composable
            fun ZoomBtn(label: String?, icon: androidx.compose.ui.graphics.vector.ImageVector?, desc: String, onClick: () -> Unit) {
                Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)) {
                    Box(Modifier.size(btnSize), contentAlignment = Alignment.Center) {
                        if (label != null) Text(label, style = MaterialTheme.typography.titleMedium)
                        else if (icon != null) Icon(icon, desc, Modifier.size(iconSz))
                    }
                }
            }
            ZoomBtn("+", null, "放大") { graphState.zoomAroundScreenCenter(graphState.scale * 1.3f) }
            ZoomBtn("−", null, "缩小") { graphState.zoomAroundScreenCenter(graphState.scale / 1.3f) }
            ZoomBtn(null, Icons.Default.CenterFocusStrong, "居中") { graphState.centerOnContent() }
        }
    }
}
