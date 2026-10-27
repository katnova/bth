package util

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
expect fun blockSigwinch()

@OptIn(ExperimentalForeignApi::class)
expect fun unblockSigwinch()