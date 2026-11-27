package dev.project.app3

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.project.app3.ui.theme.GoogleBodyFontFamily
import dev.project.app3.ui.theme.ToyStoryFontFamily
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ============================================================================
// МОДЕЛЬ ДАНИХ ГРАФІЧНОГО РЕДАКТОРА "МАЛЮВАЛКА" (TLDRAW-STYLE)
// ============================================================================

enum class TldrawTool {
    SELECT, HAND, DRAW, ERASER, ARROW, RECTANGLE, ELLIPSE, DIAMOND, TEXT, NOTE
}

enum class GeometryKind { RECTANGLE, ELLIPSE, DIAMOND, ARROW }

enum class FillMode { NONE, SEMI, SOLID }

enum class DashMode { SOLID, DASHED, DOTTED }

enum class StrokeSizePreset(val label: String, val widthPx: Float) {
    S("S", 4.5f),
    M("M", 9f),
    L("L", 16f),
    XL("XL", 26f)
}

sealed class DrawShape {
    abstract val id: Long
    abstract val color: Color
    abstract val strokeWidth: Float
    abstract val dashMode: DashMode

    data class Freehand(
        override val id: Long,
        val points: List<Offset>,
        override val color: Color,
        override val strokeWidth: Float,
        override val dashMode: DashMode
    ) : DrawShape()

    data class Geometry(
        override val id: Long,
        val kind: GeometryKind,
        val start: Offset,
        val end: Offset,
        override val color: Color,
        override val strokeWidth: Float,
        val fillMode: FillMode,
        override val dashMode: DashMode
    ) : DrawShape()

    data class TextLabel(
        override val id: Long,
        val position: Offset,
        val text: String,
        override val color: Color,
        val fontSizeSp: Float = 22f,
        override val strokeWidth: Float = 6f,
        override val dashMode: DashMode = DashMode.SOLID
    ) : DrawShape()

    data class StickyNote(
        override val id: Long,
        val topLeft: Offset,
        val size: Size = Size(300f, 260f),
        val text: String,
        override val color: Color,
        override val strokeWidth: Float = 6f,
        override val dashMode: DashMode = DashMode.SOLID
    ) : DrawShape()
}

val TldrawPaletteColors = listOf(
    Color(0xFF1D1D1D), // Black
    Color(0xFF8E95A2), // Grey
    Color(0xFFC084FC), // Light Violet
    Color(0xFF7C3AED), // Violet
    Color(0xFF2563EB), // Blue
    Color(0xFF0EA5E9), // Sky Blue
    Color(0xFFF97316), // Orange
    Color(0xFFEF4444), // Red
    Color(0xFF10B981)  // Green
)

fun createInitialTldrawShapes(): List<DrawShape> = listOf(
    DrawShape.Geometry(
        id = 1L,
        kind = GeometryKind.RECTANGLE,
        start = Offset(70f, 80f),
        end = Offset(480f, 230f),
        color = Color(0xFF2563EB),
        strokeWidth = StrokeSizePreset.M.widthPx,
        fillMode = FillMode.SEMI,
        dashMode = DashMode.SOLID
    ),
    DrawShape.TextLabel(
        id = 2L,
        position = Offset(115f, 130f),
        text = "Малювалка 🎨",
        color = Color(0xFF1D1D1D),
        fontSizeSp = 17f
    ),
    DrawShape.Geometry(
        id = 3L,
        kind = GeometryKind.ARROW,
        start = Offset(495f, 155f),
        end = Offset(660f, 200f),
        color = Color(0xFFF97316),
        strokeWidth = StrokeSizePreset.M.widthPx,
        fillMode = FillMode.NONE,
        dashMode = DashMode.SOLID
    ),
    DrawShape.StickyNote(
        id = 4L,
        topLeft = Offset(675f, 95f),
        size = Size(340f, 220f),
        text = "Ідеї, скетчі\nта нотатки!",
        color = Color(0xFFFEF08A)
    ),
    DrawShape.Geometry(
        id = 5L,
        kind = GeometryKind.ELLIPSE,
        start = Offset(120f, 295f),
        end = Offset(340f, 425f),
        color = Color(0xFF10B981),
        strokeWidth = StrokeSizePreset.M.widthPx,
        fillMode = FillMode.SEMI,
        dashMode = DashMode.DASHED
    ),
    DrawShape.Freehand(
        id = 6L,
        points = listOf(
            Offset(410f, 370f),
            Offset(465f, 325f),
            Offset(520f, 390f),
            Offset(575f, 320f),
            Offset(635f, 380f)
        ),
        color = Color(0xFF7C3AED),
        strokeWidth = StrokeSizePreset.L.widthPx,
        dashMode = DashMode.SOLID
    )
)

// ============================================================================
// КАРТКА ПРЕВ'Ю НА ГОЛОВНОМУ ЕКРАНІ (ЗАМІСТЬ ПРАПОРА)
// ============================================================================

@Composable
fun TldrawLauncherCard(
    palette: WishPalette,
    shapes: List<DrawShape>,
    onOpenEditor: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenEditor() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ГРАФІЧНИЙ РЕДАКТОР • TLDRAW STYLE",
                        color = Color(0xFF38BDF8),
                        fontFamily = GoogleBodyFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Малювалка",
                        color = palette.textPrimary,
                        fontFamily = ToyStoryFontFamily,
                        fontSize = 22.sp
                    )
                }

                Button(
                    onClick = onOpenEditor,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Відкрити ↗",
                        fontFamily = GoogleBodyFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Живе прев'ю полотна у стилі tldraw з крапковою сіткою
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8F9FA))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawDotGrid(
                        panOffset = Offset.Zero,
                        zoom = 0.72f,
                        dotColor = Color(0xFFD1D5DB)
                    )
                    withTransform({
                        scale(0.88f, 0.88f, Offset.Zero)
                    }) {
                        shapes.forEach { shape ->
                            drawTldrawShape(
                                shape = shape,
                                textMeasurer = textMeasurer,
                                isDarkCanvas = false
                            )
                        }
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Елементів: ${shapes.size} • Натисніть для малювання",
                        color = Color(0xFF374151),
                        fontFamily = GoogleBodyFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// ПОВНОЕКРАННИЙ ГРАФІЧНИЙ РЕДАКТОР "МАЛЮВАЛКА" (TLDRAW.COM)
// ============================================================================

@Composable
fun TldrawEditorScreen(
    shapes: MutableList<DrawShape>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    var currentTool by remember { mutableStateOf(TldrawTool.DRAW) }
    var activeColor by remember { mutableStateOf(Color(0xFF1D1D1D)) }
    var activeStrokeSize by remember { mutableStateOf(StrokeSizePreset.M) }
    var activeFillMode by remember { mutableStateOf(FillMode.NONE) }
    var activeDashMode by remember { mutableStateOf(DashMode.SOLID) }
    var isDarkCanvas by remember { mutableStateOf(false) }
    var showStylePanel by remember { mutableStateOf(true) }

    var panOffset by remember { mutableStateOf(Offset(60f, 840f)) }
    var zoom by remember { mutableFloatStateOf(1f) }

    var selectedShapeId by remember { mutableStateOf<Long?>(null) }
    var nextId by remember { mutableLongStateOf((shapes.maxOfOrNull { it.id } ?: 10L) + 1L) }

    // Undo / Redo стеки (знімки списку фігур)
    val undoStack = remember { mutableStateListOf<List<DrawShape>>() }
    val redoStack = remember { mutableStateListOf<List<DrawShape>>() }

    fun pushUndoSnapshot() {
        undoStack.add(shapes.toList())
        if (undoStack.size > 40) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(shapes.toList())
            val prev = undoStack.removeAt(undoStack.lastIndex)
            shapes.clear()
            shapes.addAll(prev)
            selectedShapeId = null
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(shapes.toList())
            val next = redoStack.removeAt(redoStack.lastIndex)
            shapes.clear()
            shapes.addAll(next)
            selectedShapeId = null
        }
    }

    // Стан активного малювання
    var draftFreehand by remember { mutableStateOf<DrawShape.Freehand?>(null) }
    var draftGeometry by remember { mutableStateOf<DrawShape.Geometry?>(null) }

    // Діалог введення тексту або стікера
    var pendingTextWorldPos by remember { mutableStateOf<Offset?>(null) }
    var pendingTextIsNote by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    val canvasBg = if (isDarkCanvas) Color(0xFF111215) else Color(0xFFF8F9FA)
    val dotCol = if (isDarkCanvas) Color(0xFF2B2D35) else Color(0xFFD2D6DC)
    val uiSurface = if (isDarkCanvas) Color(0xFF1E1F24) else Color.White
    val uiBorder = if (isDarkCanvas) Color(0xFF31333D) else Color(0xFFE5E7EB)
    val uiText = if (isDarkCanvas) Color(0xFFF3F4F6) else Color(0xFF1F2937)
    val uiMuted = if (isDarkCanvas) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val accentBlue = Color(0xFF2563EB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBg)
    ) {
        // --------------------------------------------------------------------
        // 1. ІНТЕРАКТИВНЕ ПОЛОТНО TLDRAW (PAN, ZOOM, DRAW, SELECT, ERASE)
        // --------------------------------------------------------------------
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    currentTool,
                    activeColor,
                    activeStrokeSize,
                    activeFillMode,
                    activeDashMode,
                    isDarkCanvas
                ) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val startWorld = (firstDown.position - panOffset) / zoom

                        var isMultiTouch = false
                        var dragMode = 0 // 0=none, 1=moveSelected, 2=resizeCorner
                        var resizeCornerIndex = -1
                        var hasModifiedForDrag = false
                        var eraserSnapshotTaken = false
                        var lastWorldPos = startWorld

                        when (currentTool) {
                            TldrawTool.DRAW -> {
                                val effectiveColor = if (isDarkCanvas && activeColor == Color(0xFF1D1D1D)) Color.White else activeColor
                                draftFreehand = DrawShape.Freehand(
                                    id = nextId,
                                    points = listOf(startWorld),
                                    color = effectiveColor,
                                    strokeWidth = activeStrokeSize.widthPx,
                                    dashMode = activeDashMode
                                )
                            }

                            TldrawTool.RECTANGLE, TldrawTool.ELLIPSE, TldrawTool.DIAMOND, TldrawTool.ARROW -> {
                                val effectiveColor = if (isDarkCanvas && activeColor == Color(0xFF1D1D1D)) Color.White else activeColor
                                val kind = when (currentTool) {
                                    TldrawTool.RECTANGLE -> GeometryKind.RECTANGLE
                                    TldrawTool.ELLIPSE -> GeometryKind.ELLIPSE
                                    TldrawTool.DIAMOND -> GeometryKind.DIAMOND
                                    else -> GeometryKind.ARROW
                                }
                                draftGeometry = DrawShape.Geometry(
                                    id = nextId,
                                    kind = kind,
                                    start = startWorld,
                                    end = startWorld,
                                    color = effectiveColor,
                                    strokeWidth = activeStrokeSize.widthPx,
                                    fillMode = activeFillMode,
                                    dashMode = activeDashMode
                                )
                            }

                            TldrawTool.SELECT -> {
                                val currentSelected = shapes.find { it.id == selectedShapeId }
                                val cornerIdx = currentSelected?.let { shape ->
                                    findHitCornerHandle(shapeBounds(shape, textMeasurer), startWorld, 28f / zoom)
                                } ?: -1

                                if (cornerIdx >= 0 && currentSelected != null) {
                                    dragMode = 2
                                    resizeCornerIndex = cornerIdx
                                } else {
                                    val hit = shapes.asReversed().firstOrNull {
                                        isPointNearShape(it, startWorld, 24f / zoom, textMeasurer)
                                    }
                                    selectedShapeId = hit?.id
                                    if (hit != null) {
                                        dragMode = 1
                                    }
                                }
                            }

                            TldrawTool.ERASER -> {
                                val hitIdx = shapes.indexOfLast {
                                    isPointNearShape(it, startWorld, 28f / zoom, textMeasurer)
                                }
                                if (hitIdx >= 0) {
                                    pushUndoSnapshot()
                                    eraserSnapshotTaken = true
                                    shapes.removeAt(hitIdx)
                                }
                            }

                            TldrawTool.TEXT -> {
                                pendingTextIsNote = false
                                pendingTextWorldPos = startWorld
                            }

                            TldrawTool.NOTE -> {
                                pendingTextIsNote = true
                                pendingTextWorldPos = startWorld
                            }

                            TldrawTool.HAND -> Unit
                        }

                        // Обробка руху пальців (1 палець або 2 пальці Pinch/Pan)
                        do {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }

                            if (pressedCount >= 2) {
                                isMultiTouch = true
                                draftFreehand = null
                                draftGeometry = null

                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val oldZoom = zoom
                                val newZoom = (oldZoom * zoomChange).coerceIn(0.25f, 4.0f)

                                val centroid = event.changes
                                    .filter { it.pressed }
                                    .fold(Offset.Zero) { acc, c -> acc + c.position } / pressedCount.toFloat()

                                panOffset = centroid - (centroid - panOffset) * (newZoom / oldZoom) + panChange
                                zoom = newZoom
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (pressedCount == 1 && !isMultiTouch) {
                                val change = event.changes.firstOrNull { it.pressed } ?: break
                                val screenDelta = change.position - change.previousPosition
                                val worldPos = (change.position - panOffset) / zoom
                                val worldDelta = worldPos - lastWorldPos
                                lastWorldPos = worldPos

                                if (change.positionChanged()) {
                                    when (currentTool) {
                                        TldrawTool.HAND -> {
                                            panOffset += screenDelta
                                        }

                                        TldrawTool.DRAW -> {
                                            draftFreehand?.let { curr ->
                                                val lastPt = curr.points.last()
                                                if (hypot((worldPos.x - lastPt.x).toDouble(), (worldPos.y - lastPt.y).toDouble()) > 2.5f / zoom) {
                                                    draftFreehand = curr.copy(points = curr.points + worldPos)
                                                }
                                            }
                                        }

                                        TldrawTool.RECTANGLE, TldrawTool.ELLIPSE, TldrawTool.DIAMOND, TldrawTool.ARROW -> {
                                            draftGeometry?.let { curr ->
                                                draftGeometry = curr.copy(end = worldPos)
                                            }
                                        }

                                        TldrawTool.ERASER -> {
                                            val hitIdx = shapes.indexOfLast {
                                                isPointNearShape(it, worldPos, 28f / zoom, textMeasurer)
                                            }
                                            if (hitIdx >= 0) {
                                                if (!eraserSnapshotTaken) {
                                                    pushUndoSnapshot()
                                                    eraserSnapshotTaken = true
                                                }
                                                shapes.removeAt(hitIdx)
                                            }
                                        }

                                        TldrawTool.SELECT -> {
                                            val selId = selectedShapeId
                                            if (selId != null && (dragMode == 1 || dragMode == 2)) {
                                                val idx = shapes.indexOfFirst { it.id == selId }
                                                if (idx >= 0) {
                                                    if (!hasModifiedForDrag && (worldDelta.x != 0f || worldDelta.y != 0f)) {
                                                        pushUndoSnapshot()
                                                        hasModifiedForDrag = true
                                                    }
                                                    shapes[idx] = if (dragMode == 1) {
                                                        translateShape(shapes[idx], worldDelta)
                                                    } else {
                                                        resizeShapeByCorner(shapes[idx], resizeCornerIndex, worldPos)
                                                    }
                                                }
                                            }
                                        }

                                        else -> Unit
                                    }
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        // Завершення штриха або фігури
                        if (!isMultiTouch) {
                            draftFreehand?.let { stroke ->
                                if (stroke.points.size >= 2) {
                                    pushUndoSnapshot()
                                    shapes.add(stroke.copy(id = nextId++))
                                } else if (stroke.points.size == 1) {
                                    pushUndoSnapshot()
                                    val p = stroke.points.first()
                                    shapes.add(stroke.copy(id = nextId++, points = listOf(p, p + Offset(1f, 1f))))
                                }
                                draftFreehand = null
                            }

                            draftGeometry?.let { geo ->
                                val dist = hypot((geo.end.x - geo.start.x).toDouble(), (geo.end.y - geo.start.y).toDouble())
                                if (dist > 8.0 / zoom) {
                                    pushUndoSnapshot()
                                    val committed = geo.copy(id = nextId++)
                                    shapes.add(committed)
                                    selectedShapeId = committed.id
                                }
                                draftGeometry = null
                            }
                        }
                    }
                }
        ) {
            drawDotGrid(panOffset = panOffset, zoom = zoom, dotColor = dotCol)

            withTransform({
                translate(panOffset.x, panOffset.y)
                scale(zoom, zoom, Offset.Zero)
            }) {
                shapes.forEach { shape ->
                    drawTldrawShape(
                        shape = shape,
                        textMeasurer = textMeasurer,
                        isDarkCanvas = isDarkCanvas
                    )
                }

                draftFreehand?.let {
                    drawTldrawShape(it, textMeasurer, isDarkCanvas)
                }
                draftGeometry?.let {
                    drawTldrawShape(it, textMeasurer, isDarkCanvas)
                }

                // Фірмова синя рамка виділення tldraw з 4 кутовими ручками
                val selected = shapes.find { it.id == selectedShapeId }
                if (selected != null && currentTool == TldrawTool.SELECT) {
                    val bounds = shapeBounds(selected, textMeasurer)
                    val pad = 8f / zoom
                    val rectTopLeft = Offset(bounds.left - pad, bounds.top - pad)
                    val rectSize = Size(bounds.width + pad * 2f, bounds.height + pad * 2f)

                    drawRect(
                        color = Color(0xFF2563EB),
                        topLeft = rectTopLeft,
                        size = rectSize,
                        style = Stroke(width = 2.2f / zoom)
                    )

                    val handleSize = 12f / zoom
                    val corners = listOf(
                        rectTopLeft,
                        Offset(rectTopLeft.x + rectSize.width, rectTopLeft.y),
                        Offset(rectTopLeft.x, rectTopLeft.y + rectSize.height),
                        Offset(rectTopLeft.x + rectSize.width, rectTopLeft.y + rectSize.height)
                    )
                    corners.forEach { c ->
                        val hOffset = Offset(c.x - handleSize / 2f, c.y - handleSize / 2f)
                        drawRect(color = Color.White, topLeft = hOffset, size = Size(handleSize, handleSize))
                        drawRect(
                            color = Color(0xFF2563EB),
                            topLeft = hOffset,
                            size = Size(handleSize, handleSize),
                            style = Stroke(width = 2f / zoom)
                        )
                    }
                }
            }
        }

        // --------------------------------------------------------------------
        // 2. ВЕРХНЯ ЛІВА ПАНЕЛЬ (НАЗАД, UNDO, REDO, СТИЛЬ, ОЧИСТИТИ, ЗБЕРЕГТИ)
        // --------------------------------------------------------------------
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = uiSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, uiBorder),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ToolbarTextButton(label = "←", color = uiText, enabled = true, onClick = onBack)
                    VerticalDivider(modifier = Modifier.height(20.dp), color = uiBorder)
                    ToolbarTextButton(
                        label = "↶",
                        color = uiText,
                        enabled = undoStack.isNotEmpty(),
                        onClick = { undo() }
                    )
                    ToolbarTextButton(
                        label = "↷",
                        color = uiText,
                        enabled = redoStack.isNotEmpty(),
                        onClick = { redo() }
                    )
                    VerticalDivider(modifier = Modifier.height(20.dp), color = uiBorder)
                    ToolbarTextButton(
                        label = if (isDarkCanvas) "☀" else "☾",
                        color = uiText,
                        enabled = true,
                        onClick = { isDarkCanvas = !isDarkCanvas }
                    )
                    ToolbarTextButton(
                        label = "🎨",
                        color = if (showStylePanel) accentBlue else uiText,
                        enabled = true,
                        onClick = { showStylePanel = !showStylePanel }
                    )
                    ToolbarTextButton(
                        label = "💾",
                        color = uiText,
                        enabled = shapes.isNotEmpty(),
                        onClick = {
                            exportTldrawToGallery(context, shapes.toList(), isDarkCanvas)
                        }
                    )
                    ToolbarTextButton(
                        label = "🗑",
                        color = Color(0xFFEF4444),
                        enabled = shapes.isNotEmpty(),
                        onClick = {
                            if (selectedShapeId != null) {
                                pushUndoSnapshot()
                                shapes.removeAll { it.id == selectedShapeId }
                                selectedShapeId = null
                            } else {
                                pushUndoSnapshot()
                                shapes.clear()
                            }
                        }
                    )
                }
            }
        }

        // --------------------------------------------------------------------
        // 3. ПРАВА ПЛАВАЮЧА ПАНЕЛЬ СТИЛІВ TLDRAW (КОЛІР, ЗАЛИВКА, ШТРИХ, РОЗМІР)
        // --------------------------------------------------------------------
        AnimatedVisibility(
            visible = showStylePanel,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 12.dp, top = 62.dp)
        ) {
            Surface(
                color = uiSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, uiBorder),
                shadowElevation = 6.dp,
                modifier = Modifier.width(154.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Сітка 3x3 кольорів tldraw
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (col in 0 until 3) {
                                    val c = TldrawPaletteColors[row * 3 + col]
                                    val selected = c == activeColor
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) accentBlue else uiBorder,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                activeColor = c
                                                selectedShapeId?.let { id ->
                                                    val idx = shapes.indexOfFirst { it.id == id }
                                                    if (idx >= 0) {
                                                        pushUndoSnapshot()
                                                        shapes[idx] = updateShapeColor(shapes[idx], c)
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = uiBorder)

                    // Режим заливки (Outline / Semi / Solid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            FillMode.NONE to "□",
                            FillMode.SEMI to "◩",
                            FillMode.SOLID to "■"
                        ).forEach { (mode, glyph) ->
                            val isActive = mode == activeFillMode
                            Surface(
                                color = if (isActive) accentBlue.copy(alpha = 0.14f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isActive) BorderStroke(1.dp, accentBlue) else null,
                                modifier = Modifier
                                    .size(38.dp, 30.dp)
                                    .clickable {
                                        activeFillMode = mode
                                        selectedShapeId?.let { id ->
                                            val idx = shapes.indexOfFirst { it.id == id }
                                            if (idx >= 0 && shapes[idx] is DrawShape.Geometry) {
                                                pushUndoSnapshot()
                                                shapes[idx] = (shapes[idx] as DrawShape.Geometry).copy(fillMode = mode)
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(glyph, color = if (isActive) accentBlue else uiText, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // Стиль лінії (Суцільна / Пунктир / Крапки)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            DashMode.SOLID to "—",
                            DashMode.DASHED to "- -",
                            DashMode.DOTTED to "···"
                        ).forEach { (mode, glyph) ->
                            val isActive = mode == activeDashMode
                            Surface(
                                color = if (isActive) accentBlue.copy(alpha = 0.14f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isActive) BorderStroke(1.dp, accentBlue) else null,
                                modifier = Modifier
                                    .size(38.dp, 30.dp)
                                    .clickable {
                                        activeDashMode = mode
                                        selectedShapeId?.let { id ->
                                            val idx = shapes.indexOfFirst { it.id == id }
                                            if (idx >= 0) {
                                                pushUndoSnapshot()
                                                shapes[idx] = updateShapeDash(shapes[idx], mode)
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = glyph,
                                        color = if (isActive) accentBlue else uiText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Товщина штриха (S / M / L / XL)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StrokeSizePreset.entries.forEach { preset ->
                            val isActive = preset == activeStrokeSize
                            Surface(
                                color = if (isActive) accentBlue.copy(alpha = 0.14f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isActive) BorderStroke(1.dp, accentBlue) else null,
                                modifier = Modifier
                                    .size(31.dp, 28.dp)
                                    .clickable {
                                        activeStrokeSize = preset
                                        selectedShapeId?.let { id ->
                                            val idx = shapes.indexOfFirst { it.id == id }
                                            if (idx >= 0) {
                                                pushUndoSnapshot()
                                                shapes[idx] = updateShapeStroke(shapes[idx], preset.widthPx)
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = preset.label,
                                        color = if (isActive) accentBlue else uiText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --------------------------------------------------------------------
        // 4. НИЖНІЙ ЛІВИЙ ІНДИКАТОР ЗУМУ (ЯК У TLDRAW)
        // --------------------------------------------------------------------
        Surface(
            color = uiSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, uiBorder),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 12.dp, bottom = 74.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "−",
                    color = uiText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { zoom = (zoom - 0.15f).coerceAtLeast(0.25f) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "${(zoom * 100).toInt()}%",
                    color = uiMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            zoom = 1f
                            panOffset = Offset(60f, 840f)
                        }
                        .padding(horizontal = 4.dp)
                )
                Text(
                    text = "+",
                    color = uiText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { zoom = (zoom + 0.15f).coerceAtMost(4.0f) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // --------------------------------------------------------------------
        // 5. НИЖНЯ ПЛАВАЮЧА ПАНЕЛЬ ІНСТРУМЕНТІВ TLDRAW (BOTTOM DOCK)
        // --------------------------------------------------------------------
        Surface(
            color = uiSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, uiBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TldrawTool.entries.forEach { tool ->
                    val isSelected = tool == currentTool
                    Surface(
                        color = if (isSelected) accentBlue else Color.Transparent,
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable {
                                currentTool = tool
                                if (tool != TldrawTool.SELECT) selectedShapeId = null
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TldrawToolIcon(
                                tool = tool,
                                tint = if (isSelected) Color.White else uiText
                            )
                        }
                    }
                }
            }
        }
    }

    // Діалог додавання тексту / нотатки на полотно
    pendingTextWorldPos?.let { pos ->
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingTextWorldPos = null },
            containerColor = uiSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (pendingTextIsNote) "Нова стікер-нотатка" else "Додати текст",
                    color = uiText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text(if (pendingTextIsNote) "Текст нотатки..." else "Введіть текст...")
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (input.isNotBlank()) {
                            pushUndoSnapshot()
                            if (pendingTextIsNote) {
                                val noteColor = if (activeColor == Color(0xFF1D1D1D) || activeColor == Color.White) {
                                    Color(0xFFFEF08A)
                                } else {
                                    activeColor.copy(alpha = 0.35f)
                                }
                                val note = DrawShape.StickyNote(
                                    id = nextId++,
                                    topLeft = pos,
                                    size = Size(280f, 210f),
                                    text = input.trim(),
                                    color = noteColor
                                )
                                shapes.add(note)
                                selectedShapeId = note.id
                            } else {
                                val effectiveColor = if (isDarkCanvas && activeColor == Color(0xFF1D1D1D)) Color.White else activeColor
                                val lbl = DrawShape.TextLabel(
                                    id = nextId++,
                                    position = pos,
                                    text = input.trim(),
                                    color = effectiveColor,
                                    fontSizeSp = 22f
                                )
                                shapes.add(lbl)
                                selectedShapeId = lbl.id
                            }
                            currentTool = TldrawTool.SELECT
                        }
                        pendingTextWorldPos = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Додати", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTextWorldPos = null }) {
                    Text("Скасувати", color = uiMuted)
                }
            }
        )
    }
}

@Composable
private fun ToolbarTextButton(
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() }
    ) {
        Text(
            text = label,
            color = if (enabled) color else color.copy(alpha = 0.28f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================================================
// ВЕКТОРНІ ІКОНКИ ІНСТРУМЕНТІВ TLDRAW (CANVAS)
// ============================================================================

@Composable
private fun TldrawToolIcon(tool: TldrawTool, tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (tool) {
            TldrawTool.SELECT -> {
                val p = Path().apply {
                    moveTo(w * 0.22f, h * 0.14f)
                    lineTo(w * 0.22f, h * 0.82f)
                    lineTo(w * 0.44f, h * 0.64f)
                    lineTo(w * 0.62f, h * 0.90f)
                    lineTo(w * 0.75f, h * 0.83f)
                    lineTo(w * 0.57f, h * 0.58f)
                    lineTo(w * 0.82f, h * 0.54f)
                    close()
                }
                drawPath(p, color = tint, style = stroke)
            }

            TldrawTool.HAND -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.28f, h * 0.36f),
                    size = Size(w * 0.46f, h * 0.48f),
                    cornerRadius = CornerRadius(5f, 5f),
                    style = stroke
                )
                for (i in 0..3) {
                    val x = w * (0.31f + i * 0.13f)
                    drawLine(tint, Offset(x, h * 0.36f), Offset(x, h * (0.14f + (i % 2) * 0.05f)), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }

            TldrawTool.DRAW -> {
                val p = Path().apply {
                    moveTo(w * 0.2f, h * 0.8f)
                    lineTo(w * 0.28f, h * 0.56f)
                    lineTo(w * 0.72f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.28f)
                    lineTo(w * 0.44f, h * 0.72f)
                    close()
                }
                drawPath(p, color = tint, style = stroke)
            }

            TldrawTool.ERASER -> {
                val p = Path().apply {
                    moveTo(w * 0.18f, h * 0.65f)
                    lineTo(w * 0.58f, h * 0.22f)
                    lineTo(w * 0.86f, h * 0.48f)
                    lineTo(w * 0.46f, h * 0.86f)
                    lineTo(w * 0.24f, h * 0.86f)
                    close()
                }
                drawPath(p, color = tint, style = stroke)
                drawLine(tint, Offset(w * 0.36f, h * 0.45f), Offset(w * 0.64f, h * 0.70f), strokeWidth = stroke.width)
            }

            TldrawTool.ARROW -> {
                drawLine(tint, Offset(w * 0.18f, h * 0.82f), Offset(w * 0.82f, h * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.45f, h * 0.18f), Offset(w * 0.82f, h * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.82f, h * 0.55f), Offset(w * 0.82f, h * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            TldrawTool.RECTANGLE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.18f),
                    size = Size(w * 0.68f, h * 0.64f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = stroke
                )
            }

            TldrawTool.ELLIPSE -> {
                drawOval(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.18f),
                    size = Size(w * 0.68f, h * 0.64f),
                    style = stroke
                )
            }

            TldrawTool.DIAMOND -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.88f)
                    lineTo(w * 0.12f, h * 0.5f)
                    close()
                }
                drawPath(p, color = tint, style = stroke)
            }

            TldrawTool.TEXT -> {
                drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.22f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            TldrawTool.NOTE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = Size(w * 0.64f, h * 0.64f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.30f, h * 0.42f), Offset(w * 0.70f, h * 0.42f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.30f, h * 0.60f), Offset(w * 0.58f, h * 0.60f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

// ============================================================================
// РЕНДЕРИНГ СІТКИ, ФІГУР, ПЕРЕМІЩЕННЯ ТА ХІТ-ТЕСТУВАННЯ
// ============================================================================

private fun DrawScope.drawDotGrid(panOffset: Offset, zoom: Float, dotColor: Color) {
    var step = 48f * zoom
    while (step < 24f) step *= 2f
    while (step > 96f) step /= 2f

    val startX = ((panOffset.x % step) + step) % step
    val startY = ((panOffset.y % step) + step) % step
    val dotRadius = (1.6f * zoom).coerceIn(1.1f, 3.2f)

    var x = startX
    while (x < size.width) {
        var y = startY
        while (y < size.height) {
            drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
            y += step
        }
        x += step
    }
}

private fun makePathEffect(dashMode: DashMode, strokeWidth: Float): PathEffect? {
    return when (dashMode) {
        DashMode.SOLID -> null
        DashMode.DASHED -> PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 2.5f, strokeWidth * 1.8f), 0f)
        DashMode.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(2f, strokeWidth * 1.9f), 0f)
    }
}

private fun buildSmoothFreehandPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    for (i in 1 until points.size - 1) {
        val midX = (points[i].x + points[i + 1].x) / 2f
        val midY = (points[i].y + points[i + 1].y) / 2f
        path.quadraticTo(points[i].x, points[i].y, midX, midY)
    }
    val last = points.last()
    path.lineTo(last.x, last.y)
    return path
}

private fun DrawScope.drawTldrawShape(
    shape: DrawShape,
    textMeasurer: TextMeasurer,
    isDarkCanvas: Boolean
) {
    val effect = makePathEffect(shape.dashMode, shape.strokeWidth)
    val stroke = Stroke(
        width = shape.strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
        pathEffect = effect
    )

    when (shape) {
        is DrawShape.Freehand -> {
            val path = buildSmoothFreehandPath(shape.points)
            drawPath(path = path, color = shape.color, style = stroke)
        }

        is DrawShape.Geometry -> {
            val left = min(shape.start.x, shape.end.x)
            val top = min(shape.start.y, shape.end.y)
            val width = max(2f, kotlin.math.abs(shape.end.x - shape.start.x))
            val height = max(2f, kotlin.math.abs(shape.end.y - shape.start.y))
            val fillColor = when (shape.fillMode) {
                FillMode.NONE -> null
                FillMode.SEMI -> shape.color.copy(alpha = 0.22f)
                FillMode.SOLID -> shape.color.copy(alpha = 0.85f)
            }

            when (shape.kind) {
                GeometryKind.RECTANGLE -> {
                    val corner = CornerRadius(12f, 12f)
                    fillColor?.let {
                        drawRoundRect(it, Offset(left, top), Size(width, height), corner, style = Fill)
                    }
                    drawRoundRect(shape.color, Offset(left, top), Size(width, height), corner, style = stroke)
                }

                GeometryKind.ELLIPSE -> {
                    fillColor?.let {
                        drawOval(it, Offset(left, top), Size(width, height), style = Fill)
                    }
                    drawOval(shape.color, Offset(left, top), Size(width, height), style = stroke)
                }

                GeometryKind.DIAMOND -> {
                    val path = Path().apply {
                        moveTo(left + width / 2f, top)
                        lineTo(left + width, top + height / 2f)
                        lineTo(left + width / 2f, top + height)
                        lineTo(left, top + height / 2f)
                        close()
                    }
                    fillColor?.let { drawPath(path, it, style = Fill) }
                    drawPath(path, shape.color, style = stroke)
                }

                GeometryKind.ARROW -> {
                    drawLine(
                        color = shape.color,
                        start = shape.start,
                        end = shape.end,
                        strokeWidth = shape.strokeWidth,
                        cap = StrokeCap.Round,
                        pathEffect = effect
                    )
                    val angle = atan2((shape.end.y - shape.start.y).toDouble(), (shape.end.x - shape.start.x).toDouble())
                    val headLen = max(18f, shape.strokeWidth * 2.8f)
                    val spread = 0.48
                    val p1 = Offset(
                        shape.end.x - (headLen * cos(angle - spread)).toFloat(),
                        shape.end.y - (headLen * sin(angle - spread)).toFloat()
                    )
                    val p2 = Offset(
                        shape.end.x - (headLen * cos(angle + spread)).toFloat(),
                        shape.end.y - (headLen * sin(angle + spread)).toFloat()
                    )
                    drawLine(shape.color, shape.end, p1, strokeWidth = shape.strokeWidth, cap = StrokeCap.Round)
                    drawLine(shape.color, shape.end, p2, strokeWidth = shape.strokeWidth, cap = StrokeCap.Round)
                }
            }
        }

        is DrawShape.TextLabel -> {
            val measured = textMeasurer.measure(
                text = shape.text,
                style = TextStyle(
                    fontFamily = GoogleBodyFontFamily,
                    fontSize = shape.fontSizeSp.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            val color = if (isDarkCanvas && shape.color == Color(0xFF1D1D1D)) Color.White else shape.color
            drawText(textLayoutResult = measured, color = color, topLeft = shape.position)
        }

        is DrawShape.StickyNote -> {
            // Тінь стікера
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.14f),
                topLeft = shape.topLeft + Offset(4f, 8f),
                size = shape.size,
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = shape.color,
                topLeft = shape.topLeft,
                size = shape.size,
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.10f),
                topLeft = shape.topLeft,
                size = shape.size,
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 2f)
            )
            val measured = textMeasurer.measure(
                text = shape.text,
                style = TextStyle(
                    fontFamily = GoogleBodyFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                ),
                constraints = androidx.compose.ui.unit.Constraints(
                    maxWidth = (shape.size.width - 44f).toInt().coerceAtLeast(60)
                )
            )
            drawText(
                textLayoutResult = measured,
                topLeft = shape.topLeft + Offset(22f, 24f)
            )
        }
    }
}

private fun shapeBounds(shape: DrawShape, textMeasurer: TextMeasurer): Rect {
    return when (shape) {
        is DrawShape.Freehand -> {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            shape.points.forEach { p ->
                minX = min(minX, p.x)
                minY = min(minY, p.y)
                maxX = max(maxX, p.x)
                maxY = max(maxY, p.y)
            }
            Rect(minX, minY, max(minX + 10f, maxX), max(minY + 10f, maxY))
        }

        is DrawShape.Geometry -> Rect(
            min(shape.start.x, shape.end.x),
            min(shape.start.y, shape.end.y),
            max(shape.start.x, shape.end.x),
            max(shape.start.y, shape.end.y)
        )

        is DrawShape.TextLabel -> {
            val res = textMeasurer.measure(
                shape.text,
                TextStyle(
                    fontFamily = GoogleBodyFontFamily,
                    fontSize = shape.fontSizeSp.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Rect(
                shape.position.x,
                shape.position.y,
                shape.position.x + res.size.width,
                shape.position.y + res.size.height
            )
        }

        is DrawShape.StickyNote -> Rect(
            shape.topLeft.x,
            shape.topLeft.y,
            shape.topLeft.x + shape.size.width,
            shape.topLeft.y + shape.size.height
        )
    }
}

private fun isPointNearShape(
    shape: DrawShape,
    point: Offset,
    tolerance: Float,
    textMeasurer: TextMeasurer
): Boolean {
    if (shape is DrawShape.Freehand) {
        return shape.points.any { p ->
            hypot((p.x - point.x).toDouble(), (p.y - point.y).toDouble()) <= tolerance + shape.strokeWidth / 2f
        }
    }
    val b = shapeBounds(shape, textMeasurer)
    return point.x >= b.left - tolerance &&
        point.x <= b.right + tolerance &&
        point.y >= b.top - tolerance &&
        point.y <= b.bottom + tolerance
}

private fun findHitCornerHandle(bounds: Rect, point: Offset, radius: Float): Int {
    val corners = listOf(
        Offset(bounds.left, bounds.top),
        Offset(bounds.right, bounds.top),
        Offset(bounds.left, bounds.bottom),
        Offset(bounds.right, bounds.bottom)
    )
    return corners.indexOfFirst { c ->
        hypot((c.x - point.x).toDouble(), (c.y - point.y).toDouble()) <= radius
    }
}

private fun translateShape(shape: DrawShape, delta: Offset): DrawShape {
    return when (shape) {
        is DrawShape.Freehand -> shape.copy(points = shape.points.map { it + delta })
        is DrawShape.Geometry -> shape.copy(start = shape.start + delta, end = shape.end + delta)
        is DrawShape.TextLabel -> shape.copy(position = shape.position + delta)
        is DrawShape.StickyNote -> shape.copy(topLeft = shape.topLeft + delta)
    }
}

private fun resizeShapeByCorner(shape: DrawShape, cornerIdx: Int, newCornerPos: Offset): DrawShape {
    return when (shape) {
        is DrawShape.Geometry -> {
            val left = min(shape.start.x, shape.end.x)
            val top = min(shape.start.y, shape.end.y)
            val right = max(shape.start.x, shape.end.x)
            val bottom = max(shape.start.y, shape.end.y)
            if (shape.kind == GeometryKind.ARROW) {
                if (cornerIdx == 0 || cornerIdx == 2) shape.copy(start = newCornerPos)
                else shape.copy(end = newCornerPos)
            } else {
                when (cornerIdx) {
                    0 -> shape.copy(start = newCornerPos, end = Offset(right, bottom))
                    1 -> shape.copy(start = Offset(left, newCornerPos.y), end = Offset(newCornerPos.x, bottom))
                    2 -> shape.copy(start = Offset(newCornerPos.x, top), end = Offset(right, newCornerPos.y))
                    else -> shape.copy(start = Offset(left, top), end = newCornerPos)
                }
            }
        }

        is DrawShape.StickyNote -> {
            val newW = max(140f, newCornerPos.x - shape.topLeft.x)
            val newH = max(120f, newCornerPos.y - shape.topLeft.y)
            shape.copy(size = Size(newW, newH))
        }

        else -> shape
    }
}

private fun updateShapeColor(shape: DrawShape, color: Color): DrawShape = when (shape) {
    is DrawShape.Freehand -> shape.copy(color = color)
    is DrawShape.Geometry -> shape.copy(color = color)
    is DrawShape.TextLabel -> shape.copy(color = color)
    is DrawShape.StickyNote -> shape.copy(color = color.copy(alpha = 0.35f))
}

private fun updateShapeDash(shape: DrawShape, dash: DashMode): DrawShape = when (shape) {
    is DrawShape.Freehand -> shape.copy(dashMode = dash)
    is DrawShape.Geometry -> shape.copy(dashMode = dash)
    else -> shape
}

private fun updateShapeStroke(shape: DrawShape, strokeWidth: Float): DrawShape = when (shape) {
    is DrawShape.Freehand -> shape.copy(strokeWidth = strokeWidth)
    is DrawShape.Geometry -> shape.copy(strokeWidth = strokeWidth)
    else -> shape
}

// ============================================================================
// ЕКСПОРТ МАЛЮНКА У ГАЛЕРЕЮ (PNG)
// ============================================================================

private fun exportTldrawToGallery(
    context: Context,
    shapes: List<DrawShape>,
    isDarkCanvas: Boolean
) {
    try {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(if (isDarkCanvas) 0xFF111215.toInt() else 0xFFF8F9FA.toInt())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        shapes.forEach { shape ->
            paint.color = shape.color.toArgb()
            paint.strokeWidth = shape.strokeWidth
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            paint.pathEffect = when (shape.dashMode) {
                DashMode.SOLID -> null
                DashMode.DASHED -> DashPathEffect(floatArrayOf(24f, 16f), 0f)
                DashMode.DOTTED -> DashPathEffect(floatArrayOf(4f, 16f), 0f)
            }

            when (shape) {
                is DrawShape.Freehand -> {
                    paint.style = Paint.Style.STROKE
                    val path = AndroidPath()
                    shape.points.firstOrNull()?.let { first ->
                        path.moveTo(first.x, first.y)
                        shape.points.drop(1).forEach { p -> path.lineTo(p.x, p.y) }
                        canvas.drawPath(path, paint)
                    }
                }

                is DrawShape.Geometry -> {
                    val l = min(shape.start.x, shape.end.x)
                    val t = min(shape.start.y, shape.end.y)
                    val r = max(shape.start.x, shape.end.x)
                    val b = max(shape.start.y, shape.end.y)
                    paint.style = if (shape.fillMode == FillMode.SOLID) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
                    when (shape.kind) {
                        GeometryKind.RECTANGLE -> canvas.drawRoundRect(l, t, r, b, 12f, 12f, paint)
                        GeometryKind.ELLIPSE -> canvas.drawOval(l, t, r, b, paint)
                        GeometryKind.DIAMOND -> {
                            val p = AndroidPath().apply {
                                moveTo((l + r) / 2f, t)
                                lineTo(r, (t + b) / 2f)
                                lineTo((l + r) / 2f, b)
                                lineTo(l, (t + b) / 2f)
                                close()
                            }
                            canvas.drawPath(p, paint)
                        }
                        GeometryKind.ARROW -> {
                            paint.style = Paint.Style.STROKE
                            canvas.drawLine(shape.start.x, shape.start.y, shape.end.x, shape.end.y, paint)
                        }
                    }
                }

                is DrawShape.TextLabel -> {
                    paint.style = Paint.Style.FILL
                    paint.textSize = shape.fontSizeSp * 2.5f
                    canvas.drawText(shape.text, shape.position.x, shape.position.y + 40f, paint)
                }

                is DrawShape.StickyNote -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawRoundRect(
                        shape.topLeft.x,
                        shape.topLeft.y,
                        shape.topLeft.x + shape.size.width,
                        shape.topLeft.y + shape.size.height,
                        14f,
                        14f,
                        paint
                    )
                    paint.color = 0xFF1F2937.toInt()
                    paint.textSize = 36f
                    canvas.drawText(shape.text, shape.topLeft.x + 22f, shape.topLeft.y + 64f, paint)
                }
            }
        }

        val fileName = "Malyuvalka_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Malyuvalka")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(context, "Збережено у Галерею: $fileName", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Помилка збереження: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
