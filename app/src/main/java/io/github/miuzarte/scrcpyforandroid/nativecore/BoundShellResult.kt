package io.github.miuzarte.scrcpyforandroid.nativecore

import java.io.IOException

/** Preserve command output while requiring a complete shell exit-status trailer. */
internal fun parseBoundShellResult(response: String, marker: String): String {
    val boundary = response.lastIndexOf("\n$marker=")
    if (boundary < 0) throw IOException("ADB shell ended before reporting command completion")
    val status = response.substring(boundary + marker.length + 2).trim().toIntOrNull()
        ?: throw IOException("ADB shell returned an invalid command status")
    val output = response.substring(0, boundary).trimEnd('\r', '\n')
    if (status != 0) throw IOException("ADB shell command failed (exit $status): $output")
    return output
}
