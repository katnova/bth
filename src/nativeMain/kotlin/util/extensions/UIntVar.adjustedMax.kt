package util.extensions

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
fun UIntVar.adjustedMax() =
    this.value.toInt() - 3 // -3 to avoid writing over border box, always seems to be a few rows longer than term