package io.github.miuzarte.scrcpyforandroid.scrcpy.display

import java.util.concurrent.CancellationException

interface DisplayResizeStore {
    fun load(deviceSerial: String): PendingDisplayResize?
    fun save(record: PendingDisplayResize)
    fun remove(deviceSerial: String)
}

/** The caller binds this executor to an identified ADB connection. */
fun interface DisplayResizeShell {
    suspend fun execute(command: String): String
}

data class PendingDisplayResize(
    val deviceSerial: String,
    val original: DisplaySizeSnapshot,
    val applied: DisplaySize,
) {
    init {
        require(deviceSerial.isNotBlank()) { "Device serial must not be blank" }
    }
}

enum class DisplayResizeFailure {
    DEVICE_IDENTITY_UNAVAILABLE,
    INVALID_SIZE_OUTPUT,
    JOURNAL_FAILURE,
    JOURNAL_MISMATCH,
    SHELL_FAILURE,
    VERIFICATION_FAILED,
    EXTERNAL_CHANGE,
}

class DisplayResizeException(
    val reason: DisplayResizeFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Owns the original main-display size until restoration has been verified.
 *
 * Scrcpy serializes these calls with its session lifecycle mutex. Journal writes
 * must be durable before a remote mutation, and failed cleanup keeps the record.
 */
class TemporaryDisplayResize(private val store: DisplayResizeStore) {
    private data class ActiveResize(
        val record: PendingDisplayResize,
        val shell: DisplayResizeShell,
    )

    private var active: ActiveResize? = null

    val hasActiveResize: Boolean
        get() = active != null

    suspend fun apply(
        deviceSerial: String,
        shell: DisplayResizeShell,
        requested: DisplaySize,
    ) {
        requireIdentity(deviceSerial)
        // A previous interrupted session must never become the new baseline.
        recover(deviceSerial, shell)
        val original = readSnapshot(shell)
        val applied = requested.orientedLike(original.physical)
        if (original.effective == applied) return

        val record = PendingDisplayResize(deviceSerial, original, applied)
        active = ActiveResize(record, shell)
        journal("save") { store.save(record) }
        execute(shell, "wm size " + applied)
        val observed = readSnapshot(shell)
        if (observed.physical != original.physical || observed.effective != applied) {
            throw DisplayResizeException(
                DisplayResizeFailure.VERIFICATION_FAILED,
                "The phone reported " + observed.effective +
                    " instead of the requested size " + applied + "; choose a supported size",
            )
        }
    }

    /** Returns true when an owned resize was restored; no active resize returns false. */
    suspend fun restore(): Boolean {
        val owned = active ?: return false
        restoreRecord(owned.record, owned.shell)
        return true
    }

    /**
     * Retry restoration with a freshly identified connection, even when disabled
     * in the new profile. Records belonging to other phones remain in the store.
     */
    suspend fun recover(deviceSerial: String, shell: DisplayResizeShell): Boolean {
        requireIdentity(deviceSerial)
        val previous = active
        if (previous != null && previous.record.deviceSerial != deviceSerial) {
            // The old connection is borrowed, but its original size is still ours.
            journal("save") { store.save(previous.record) }
            active = null
        }

        val owned = active?.record
        val persisted = journal("read") { store.load(deviceSerial) }
        val record = persisted ?: owned ?: return false
        if (record.deviceSerial != deviceSerial || (owned != null && owned != record)) {
            throw DisplayResizeException(
                DisplayResizeFailure.JOURNAL_MISMATCH,
                "The saved display size does not match this phone's owned resize",
            )
        }
        active = ActiveResize(record, shell)
        restoreRecord(record, shell)
        return true
    }

    private suspend fun restoreRecord(record: PendingDisplayResize, shell: DisplayResizeShell) {
        val observed = readSnapshot(shell)
        if (observed == record.original) {
            finish(record)
            return
        }
        if (observed.physical != record.original.physical || observed.effective != record.applied) {
            throw DisplayResizeException(
                DisplayResizeFailure.EXTERNAL_CHANGE,
                "The phone's display size changed outside this session; its saved size was kept",
            )
        }
        val command = record.original.overrideSize?.let { "wm size " + it } ?: "wm size reset"
        execute(shell, command)
        if (readSnapshot(shell) != record.original) {
            throw DisplayResizeException(
                DisplayResizeFailure.VERIFICATION_FAILED,
                "The phone's original display size could not be verified after restoration",
            )
        }
        finish(record)
    }

    private fun finish(record: PendingDisplayResize) {
        journal("remove") { store.remove(record.deviceSerial) }
        if (active?.record == record) active = null
    }

    private suspend fun readSnapshot(shell: DisplayResizeShell): DisplaySizeSnapshot {
        val output = execute(shell, "wm size")
        return DisplaySizeSnapshot.parse(output) ?: throw DisplayResizeException(
            DisplayResizeFailure.INVALID_SIZE_OUTPUT,
            "Cannot determine the phone's complete display size from wm size: " + output.trim(),
        )
    }

    private suspend fun execute(shell: DisplayResizeShell, command: String): String =
        try {
            shell.execute(command)
        } catch (error: CancellationException) {
            throw error
        } catch (error: DisplayResizeException) {
            throw error
        } catch (error: Exception) {
            throw DisplayResizeException(
                DisplayResizeFailure.SHELL_FAILURE,
                "Display size command failed: " + command + ": " + error.message,
                error,
            )
        }

    private fun <T> journal(operation: String, block: () -> T): T =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: DisplayResizeException) {
            throw error
        } catch (error: Exception) {
            throw DisplayResizeException(
                DisplayResizeFailure.JOURNAL_FAILURE,
                "Could not " + operation + " the saved display size: " + error.message,
                error,
            )
        }

    private fun requireIdentity(deviceSerial: String) {
        if (deviceSerial.isBlank()) {
            throw DisplayResizeException(
                DisplayResizeFailure.DEVICE_IDENTITY_UNAVAILABLE,
                "A stable phone serial is required to restore its display size after reconnection",
            )
        }
    }
}
