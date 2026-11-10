package view

import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import model.ActivePlane
import notcurses.ncplane_create
import notcurses.ncplane_erase
import notcurses.ncplane_move_yx
import notcurses.ncplane_options
import notcurses.ncplane_resize
import model.state.SliceReader
import model.state.InputEvent
import model.state.TUIState
import model.state.ViewState
import util.extensions.assertNegativeOrThrow


internal class PlaneContext @OptIn(ExperimentalForeignApi::class) constructor(
    val stdPlane: CPointer<ncplane>?,
    val scope: CoroutineScope,
    val drawChannel: SendChannel<suspend () -> Unit>,
    val view: SliceReader<ViewState>,
)

internal class PlaneLayout(
    val rows: (UInt) -> UInt,
    val cols: (UInt) -> UInt,
    val x: (Int) -> Int,
    val y: (Int) -> Int,
)

internal data class Geometry(val rows: UInt, val cols: UInt)

/**
 * Represents an abstract plane used for drawing and managing UI elements within the TUI.
 *
 *
 * @constructor Creates a KPlane instance with the specified parameters.
 */
@OptIn(ExperimentalForeignApi::class)
internal abstract class KPlane<M>(
    protected val ctx: PlaneContext,
    private val layout: PlaneLayout,
    protected val activeOnPlane: ActivePlane? = null
) {
    private var isPlaneActive = true
    protected val plane: CPointer<ncplane>
    protected val planeOpts: ncplane_options
    protected var planeRows: UInt
    protected var planeCols: UInt
    protected var planeYCord: Int
    protected var planeXCord: Int
    protected abstract val model: Flow<M>
    open fun onAction(action: InputEvent): Boolean = false
    private data class Frame<M>(val view: ViewState, val model: M)

    init {
        memScoped {
            planeRows = layout.rows(ctx.view.value.realRows)
            planeCols = layout.cols(ctx.view.value.realCols)
            planeYCord = layout.y(ctx.view.value.realRows.toInt())
            planeXCord = layout.x(ctx.view.value.realCols.toInt())
            planeOpts = alloc<ncplane_options>().apply {
                rows = planeRows
                cols = planeCols
                // These should be relative to [stdPlane]
                y = planeYCord
                x = planeXCord
            }

            plane = ncplane_create(ctx.stdPlane, planeOpts.ptr)
                ?: throw IllegalStateException("Failed to create KPlane! ncplane_create returned null")
        }
    }

    /**
     * Draw conditions:
     * - If this [KPlane] has no [activeOnPlane] set
     * - If this [KPlane] has [activeOnPlane] set, and [ViewState.activePlane] equals [activeOnPlane]
     */
    fun launch(): Job = ctx.scope.launch {
        combine(ctx.view.state, model) { v, m -> Frame(view = v, model = m) }
            .takeWhile { it.view.isRunning }
            .onEach { syncVisibility(it.view.activePlane) }
            .filter { isPlaneActive }
            .onEach {
                /**
                 * Check to see if we need to resize this [KPlane].
                 */
                resizeIfNeeded(it.view.realRows, it.view.realCols)
            }
            .distinctUntilChanged()
            .collect { frame ->
                val geo = Geometry(planeRows, planeCols)
                ctx.drawChannel.send { draw(frame.model, geo) }
            }

    }

    /**
     * Check if this [KPlane]'s declared [activeOnPlane] matches [TUIState.activePlane].
     *
     * If mismatched, hide this plane. Else, show it.
     */
    private fun syncVisibility(activePlane: ActivePlane) {
        if (activeOnPlane != null) {
            if (isPlaneActive && activeOnPlane != activePlane) {
                isPlaneActive = false
                ncplane_move_yx(plane, ((ctx.view.value.realRows * 2u).toInt()), ((ctx.view.value.realCols * 2u).toInt()))
            } else if (!isPlaneActive && activeOnPlane == activePlane) {
                ncplane_move_yx(plane, planeYCord, planeXCord)
                isPlaneActive = true
            }
        }
    }

    /**
     * Resize if computed cords and dim do not match current [planeRows], [planeCols], [planeXCord], or [planeYCord]
     */
    private fun resizeIfNeeded(newTerminalRows: UInt, newTerminalCols: UInt) {
        val newUiRows = layout.rows(newTerminalRows)
        val newUiCols = layout.cols(newTerminalCols)
        val newXCord = layout.x(newTerminalCols.toInt())
        val newYCord = layout.y(newTerminalRows.toInt())
        if (newUiRows != planeRows || newUiCols != planeCols || newXCord != planeXCord || newYCord != planeYCord) {
            ncplane_erase(plane)

            ncplane_resize(
                plane,
                keepy = 0,
                keepx = 0,
                keepleny = 0u,
                keeplenx = 0u,
                yoff = 0,
                xoff = 0,
                ylen = newUiRows,
                xlen = newUiCols
            ).assertNegativeOrThrow("Failed to resize ParentKPlane.")

            ncplane_move_yx(plane, newYCord, newXCord).assertNegativeOrThrow("Failed to move ParentKPlane.")

            this.planeRows = newUiRows
            this.planeCols = newUiCols
            this.planeXCord = newXCord
            this.planeYCord = newYCord
        }
    }

    /**
     * Per-plane draw logic
     */
    protected abstract fun draw(model: M, geo: Geometry)
}