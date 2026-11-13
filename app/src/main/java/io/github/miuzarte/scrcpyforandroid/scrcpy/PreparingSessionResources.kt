package io.github.miuzarte.scrcpyforandroid.scrcpy

import java.io.Closeable

/**
 * Owns channels and workers during startup, active use and incomplete cleanup.
 *
 * Register a channel before its reader so cleanup closes the channel first.
 * Calls are serialized by Session's mutex; a failed close keeps its reference.
 */
internal class PreparingSessionResources {
    private class Entry(val name: String, val resource: Closeable)

    data class CleanupFailure(val name: String, val cause: Exception)

    private val entries = mutableListOf<Entry>()

    val hasRetainedResources: Boolean
        get() = entries.isNotEmpty()

    fun <T : Closeable> own(name: String, resource: T): T {
        if (entries.none { it.resource === resource }) {
            entries += Entry(name, resource)
        }
        return resource
    }

    /** The source no longer owns any of these references after the transfer. */
    fun transferTo(target: PreparingSessionResources) {
        require(target !== this) { "Cannot transfer session resources to their current owner" }
        entries.forEach { target.own(it.name, it.resource) }
        entries.clear()
    }

    fun release(resource: Closeable): CleanupFailure? {
        val entry = entries.firstOrNull { it.resource === resource } ?: return null
        return closeEntry(entry)
    }

    /** Attempt every close, retaining only the resources whose close failed. */
    fun closeAll(): List<CleanupFailure> =
        entries.toList().mapNotNull(::closeEntry)

    private fun closeEntry(entry: Entry): CleanupFailure? =
        try {
            entry.resource.close()
            entries.remove(entry)
            null
        } catch (error: Exception) {
            CleanupFailure(entry.name, error)
        }
}
