package model

enum class FilterModes(val displayName: String, val color: StrRGB) {
    /**
     * Show records from any dir. (no-filter)
     */
    FM_DIR_ALL("ALL", StrRGB(0u, 0u, 0u, 255u, 255u, 255u, 255u, 255u)),

    /**
     * Show records only from the pwd. Does not include subdirectories.
     */
    FM_DIR_PWD("PWD", StrRGB(0u, 0u, 0u, 255u, 255u, 255u, 255u, 255u)),

    /**
     * Show records from the pwd AND subdirectories.
     */
    FM_DIR_SUB("SUB", StrRGB(0u, 0u, 0u, 255u, 255u, 255u, 255u, 255u));

    companion object {

    }
}