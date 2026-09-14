package view

import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import model.ActivePlane
import store.TUIState
import store.TuiStore
import notcurses.ncplane_create
import notcurses.ncplane_erase
import notcurses.ncplane_move_bottom
import notcurses.ncplane_move_top
import notcurses.ncplane_move_yx
import notcurses.ncplane_options
import notcurses.ncplane_resize
import util.extensions.assertNegativeOrThrow

/**
 * Represents an abstract plane used for drawing and managing UI elements within the TUI.
 *
 *
 * @constructor Creates a KPlane instance with the specified parameters.
 * @param stdPlane A pointer to the standard `notcurses` plane, used as the parent for this plane.
 * @param tuiStore The store that manages TUI state and provides a coroutine scope for operations.
 * @param computeUiRows A function to compute the number of rows for the UI plane based on terminal rows.
 * @param computeUiCols A function to compute the number of columns for the UI plane based on terminal columns.
 * @param computeXCord A function to calculate the X-coordinate of the plane based on terminal columns.
 * @param computeYCord A function to calculate the Y-coordinate of the plane based on terminal rows.
 */
@OptIn(ExperimentalForeignApi::class)
internal abstract class KPlane(
    protected val stdPlane: CPointer<ncplane>?,
    tuiStore: TuiStore,
    protected val computeUiRows: (rows: UInt) -> UInt,
    protected val computeUiCols: (cols: UInt) -> UInt,
    protected val computeXCord: (rows: Int) -> Int,
    protected val computeYCord: (cols: Int) -> Int,
    protected val keySelector: (state: TUIState) -> List<Any> = { listOf(it) },
    protected val activeOnPlane: ActivePlane? = null
) {
    private var active = true
    protected val plane: CPointer<ncplane>
    protected val store: TuiStore
    protected val planeOpts: ncplane_options
    protected var planeRows: UInt
    protected var planeCols: UInt
    protected var planeYCord: Int
    protected var planeXCord: Int

    init {
        memScoped {
            store = tuiStore
            planeRows = computeUiRows(store.state.value.realRows)
            planeCols = computeUiCols(store.state.value.realCols)
            planeYCord = computeYCord(store.state.value.realRows.toInt())
            planeXCord = computeXCord(store.state.value.realCols.toInt())
            planeOpts = alloc<ncplane_options>().apply {
                rows = planeRows
                cols = planeCols
                // These should be relative to [stdPlane]
                y = planeYCord
                x = planeXCord
            }

            plane = ncplane_create(stdPlane, planeOpts.ptr)
                ?: throw IllegalStateException("Failed to create KPlane! ncplane_create returned null")
        }
    }

    fun hide() {
        if (active) {
            active = false
            ncplane_erase(plane) //todo: possibly not needed with better nc usage
            ncplane_move_bottom(plane)
        }
    }

    fun show() {
        if (!active) {
            ncplane_move_top(plane)
            active = true
        }
    }

    fun launch(): Job = store.scope.launch {
        store.state
            .takeWhile { it.isRunning }
            .onEach {
                /**
                 * Check if this [KPlane]'s declared [activeOnPlane] matches [TUIState.activePlane].
                 *
                 * If mismatched, [hide] this plane. Else, [show] it.
                 */
                if (activeOnPlane != null) {
                    if (active && activeOnPlane != it.activePlane) {
                        hide()
                    } else if (!active && activeOnPlane == it.activePlane) {
                        show()
                    }
                }
            }
            .filter { (activeOnPlane == null) || (activeOnPlane == it.activePlane) } //If a plane is not [active], don't process it.
            .onEach {
                /**
                 * Check to see if we need to resize this [KPlane].
                 */
                resizeIfNeeded(it.realRows, it.realCols)
            }
            .distinctUntilChangedBy(keySelector)
            .draw()
    }

    /**
     * Resize if computed cords and dim do not match current [planeRows], [planeCols], [planeXCord], or [planeYCord]
     */
    private fun resizeIfNeeded(newTerminalRows: UInt, newTerminalCols: UInt) {
        val newUiRows = computeUiRows(newTerminalRows)
        val newUiCols = computeUiCols(newTerminalCols)
        val newXCord = computeXCord(newTerminalCols.toInt())
        val newYCord = computeYCord(newTerminalRows.toInt())
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
    protected abstract suspend fun Flow<TUIState>.draw()

    /**
     * Submits a drawing operation to the draw channel for execution.
     *
     * @param block The suspendable drawing operation that will be executed when consumed from the draw channel.
     */
    protected suspend fun submitDraw(block: suspend () -> Unit) = store.drawChannel.send(block)
}