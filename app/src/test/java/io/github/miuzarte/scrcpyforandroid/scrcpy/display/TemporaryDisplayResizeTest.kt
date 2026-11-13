package io.github.miuzarte.scrcpyforandroid.scrcpy.display

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TemporaryDisplayResizeTest {
    private val phoneSize = DisplaySize(1080, 2400)
    private val tabletSize = DisplaySize(1920, 1200)
    private val targetSize = DisplaySize(1200, 1920)

    @Test
    fun alreadyMatchedDisplayDoesNotMutateOrCreateARecoveryRecord() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize, targetSize))
        val resize = TemporaryDisplayResize(store)

        resize.apply("phone-a", shell, tabletSize)

        assertFalse(resize.hasActiveResize)
        assertTrue(store.records.isEmpty())
        assertTrue(shell.mutations.isEmpty())
        assertFalse(resize.restore())
    }

    @Test
    fun defaultDisplayIsRestoredAndRepeatedStopDoesNothing() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize))
        val resize = TemporaryDisplayResize(store)

        resize.apply("phone-a", shell, tabletSize)
        assertEquals(targetSize, shell.snapshot.effective)
        assertEquals(DisplaySizeSnapshot(phoneSize), store.records["phone-a"]?.original)
        assertTrue(resize.hasActiveResize)

        assertTrue(resize.restore())
        assertEquals(DisplaySizeSnapshot(phoneSize), shell.snapshot)
        assertEquals(listOf("wm size 1200x1920", "wm size reset"), shell.mutations)
        assertFalse(resize.hasActiveResize)
        assertTrue(store.records.isEmpty())
        assertFalse(resize.restore())
    }

    @Test
    fun existingOverrideIsRestoredExactlyInsteadOfResetToPhysical() = runBlocking {
        val original = DisplaySizeSnapshot(phoneSize, DisplaySize(1080, 1920))
        val store = MemoryStore()
        val shell = FakeShell(original)
        val resize = TemporaryDisplayResize(store)

        resize.apply("phone-a", shell, tabletSize)
        assertEquals(original, store.records["phone-a"]?.original)
        resize.restore()

        assertEquals(original, shell.snapshot)
        assertEquals("wm size 1080x1920", shell.mutations.last())
        assertFalse(shell.mutations.contains("wm size reset"))
    }

    @Test
    fun incompleteSnapshotFailsBeforeSavingOrChangingAnything() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize)).apply {
            queryOutput = "Physical size: 1080x2400\nOverride size: invalid"
        }
        val resize = TemporaryDisplayResize(store)

        assertFailure(DisplayResizeFailure.INVALID_SIZE_OUTPUT) {
            resize.apply("phone-a", shell, tabletSize)
        }

        assertTrue(store.records.isEmpty())
        assertTrue(shell.mutations.isEmpty())
        assertFalse(resize.hasActiveResize)
    }

    @Test
    fun unavailableIdentityFailsBeforeDeviceCommands() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize))
        val resize = TemporaryDisplayResize(store)

        assertFailure(DisplayResizeFailure.DEVICE_IDENTITY_UNAVAILABLE) {
            resize.apply("", shell, tabletSize)
        }

        assertTrue(shell.commands.isEmpty())
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun deniedChangeKeepsOriginalFactsUntilCleanupVerifiesNoChange() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize)).apply { denyChanges = true }
        val resize = TemporaryDisplayResize(store)

        assertFailure(DisplayResizeFailure.SHELL_FAILURE) {
            resize.apply("phone-a", shell, tabletSize)
        }
        assertTrue(resize.hasActiveResize)
        assertEquals(DisplaySizeSnapshot(phoneSize), store.records["phone-a"]?.original)

        assertTrue(resize.restore())
        assertTrue(store.records.isEmpty())
        assertFalse(resize.hasActiveResize)
    }

    @Test
    fun lostAcknowledgmentAfterMutationStillLeavesEnoughFactsToRestore() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize))
        val resize = TemporaryDisplayResize(store)
        val transportFailure = IOException("Connection closed before acknowledgment")
        shell.afterChange = {
            shell.afterChange = null
            throw transportFailure
        }

        val error = assertFailure(DisplayResizeFailure.SHELL_FAILURE) {
            resize.apply("phone-a", shell, tabletSize)
        }

        assertSame(transportFailure, error.cause)
        assertEquals(targetSize, shell.snapshot.effective)
        assertTrue(store.records.containsKey("phone-a"))
        assertTrue(resize.restore())
        assertEquals(DisplaySizeSnapshot(phoneSize), shell.snapshot)
    }

    @Test
    fun interruptedConnectionIsRecoveredFromDurableRecordByANewOwner() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize))
        val resize = TemporaryDisplayResize(store)
        resize.apply("phone-a", shell, tabletSize)
        shell.broken = true

        assertFailure(DisplayResizeFailure.SHELL_FAILURE) { resize.restore() }
        assertTrue(resize.hasActiveResize)
        assertTrue(store.records.containsKey("phone-a"))

        shell.broken = false
        val restartedClient = TemporaryDisplayResize(store)
        assertTrue(restartedClient.recover("phone-a", shell))
        assertEquals(DisplaySizeSnapshot(phoneSize), shell.snapshot)
        assertTrue(store.records.isEmpty())
        assertFalse(restartedClient.hasActiveResize)
    }

    @Test
    fun anotherPhoneDoesNotReceiveOrOverwriteTheFirstPhonesSavedSize() = runBlocking {
        val store = MemoryStore()
        val first = FakeShell(DisplaySizeSnapshot(phoneSize))
        val secondOriginal = DisplaySizeSnapshot(DisplaySize(1440, 3200), DisplaySize(1440, 2560))
        val second = FakeShell(secondOriginal)
        val resize = TemporaryDisplayResize(store)
        resize.apply("phone-a", first, tabletSize)
        first.broken = true
        assertFailure(DisplayResizeFailure.SHELL_FAILURE) { resize.restore() }

        assertFalse(resize.recover("phone-b", second))
        assertTrue(second.commands.isEmpty())
        assertTrue(store.records.containsKey("phone-a"))
        resize.apply("phone-b", second, tabletSize)
        assertEquals(setOf("phone-a", "phone-b"), store.records.keys)
        resize.restore()
        assertEquals(secondOriginal, second.snapshot)
        assertEquals(setOf("phone-a"), store.records.keys)

        first.broken = false
        assertTrue(resize.recover("phone-a", first))
        assertEquals(DisplaySizeSnapshot(phoneSize), first.snapshot)
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun externalDisplayChangeIsPreservedAndReportedInsteadOfOverwritten() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize))
        val resize = TemporaryDisplayResize(store)
        resize.apply("phone-a", shell, tabletSize)
        shell.overrideSize = DisplaySize(1080, 2160)
        val changesBeforeRestore = shell.mutations.toList()

        assertFailure(DisplayResizeFailure.EXTERNAL_CHANGE) { resize.restore() }
        assertEquals(DisplaySize(1080, 2160), shell.snapshot.effective)
        assertEquals(changesBeforeRestore, shell.mutations)
        assertTrue(store.records.containsKey("phone-a"))

        shell.overrideSize = null
        assertTrue(resize.recover("phone-a", shell))
        assertEquals(changesBeforeRestore, shell.mutations)
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun cancellationAfterMutationRetainsTheLeaseForFinalCleanup() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize))
        val resize = TemporaryDisplayResize(store)
        val changed = CompletableDeferred<Unit>()
        val holdCommand = CompletableDeferred<Unit>()
        shell.afterChange = {
            changed.complete(Unit)
            holdCommand.await()
        }
        val job = launch { resize.apply("phone-a", shell, tabletSize) }

        changed.await()
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertTrue(resize.hasActiveResize)
        assertEquals(targetSize, shell.snapshot.effective)
        assertTrue(store.records.containsKey("phone-a"))
        shell.afterChange = null
        resize.restore()
        assertEquals(DisplaySizeSnapshot(phoneSize), shell.snapshot)
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun failedJournalSaveCannotSendAMutationAndItsFactsCanBeCleanedUp() = runBlocking {
        val store = MemoryStore().apply { failAfterSave = true }
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize))
        val resize = TemporaryDisplayResize(store)

        assertFailure(DisplayResizeFailure.JOURNAL_FAILURE) {
            resize.apply("phone-a", shell, tabletSize)
        }

        assertTrue(shell.mutations.isEmpty())
        assertTrue(resize.hasActiveResize)
        assertTrue(store.records.containsKey("phone-a"))
        resize.restore()
        assertTrue(store.records.isEmpty())
        assertFalse(resize.hasActiveResize)
    }

    @Test
    fun lostRestoreAcknowledgmentIsClearedOnRetryWithoutAnotherMutation() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize))
        val resize = TemporaryDisplayResize(store)
        resize.apply("phone-a", shell, tabletSize)
        shell.afterChange = {
            shell.afterChange = null
            throw IOException("Restore acknowledgment lost")
        }

        assertFailure(DisplayResizeFailure.SHELL_FAILURE) { resize.restore() }
        assertEquals(DisplaySizeSnapshot(phoneSize), shell.snapshot)
        assertTrue(store.records.containsKey("phone-a"))
        val mutations = shell.mutations.toList()

        assertTrue(resize.recover("phone-a", shell))
        assertEquals(mutations, shell.mutations)
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun journalRemovalFailureKeepsOwnershipUntilFinalizationRetry() = runBlocking {
        val store = MemoryStore()
        val shell = FakeShell(DisplaySizeSnapshot(phoneSize))
        val resize = TemporaryDisplayResize(store)
        resize.apply("phone-a", shell, tabletSize)
        store.failRemove = true

        assertFailure(DisplayResizeFailure.JOURNAL_FAILURE) { resize.restore() }
        assertEquals(DisplaySizeSnapshot(phoneSize), shell.snapshot)
        assertTrue(store.records.containsKey("phone-a"))
        assertTrue(resize.hasActiveResize)

        store.failRemove = false
        val mutations = shell.mutations.toList()
        assertTrue(resize.recover("phone-a", shell))
        assertEquals(mutations, shell.mutations)
        assertTrue(store.records.isEmpty())
        assertFalse(resize.hasActiveResize)
    }

    private suspend fun assertFailure(
        reason: DisplayResizeFailure,
        operation: suspend () -> Unit,
    ): DisplayResizeException {
        try {
            operation()
        } catch (error: DisplayResizeException) {
            assertEquals(reason, error.reason)
            return error
        }
        throw AssertionError("Expected display resize failure " + reason)
    }

    private class MemoryStore : DisplayResizeStore {
        val records = linkedMapOf<String, PendingDisplayResize>()
        var failAfterSave = false
        var failRemove = false

        override fun load(deviceSerial: String): PendingDisplayResize? = records[deviceSerial]

        override fun save(record: PendingDisplayResize) {
            records[record.deviceSerial] = record
            if (failAfterSave) throw IOException("Journal write acknowledgment lost")
        }

        override fun remove(deviceSerial: String) {
            if (failRemove) throw IOException("Cannot remove journal")
            records.remove(deviceSerial)
        }
    }

    private class FakeShell(initial: DisplaySizeSnapshot) : DisplayResizeShell {
        private val physical = initial.physical
        var overrideSize = initial.overrideSize
        var broken = false
        var denyChanges = false
        var queryOutput: String? = null
        var afterChange: (suspend (String) -> Unit)? = null
        val commands = mutableListOf<String>()
        val mutations: List<String>
            get() = commands.filter { it != "wm size" }
        val snapshot: DisplaySizeSnapshot
            get() = DisplaySizeSnapshot(physical, overrideSize)

        override suspend fun execute(command: String): String {
            commands += command
            if (broken) throw IOException("Phone disconnected")
            if (command == "wm size") {
                return queryOutput ?: buildString {
                    append("Physical size: ")
                    append(physical)
                    append('\n')
                    overrideSize?.let {
                        append("Override size: ")
                        append(it)
                        append('\n')
                    }
                }
            }
            if (denyChanges) throw IOException("Permission denied")
            overrideSize = if (command == "wm size reset") null
            else DisplaySize.parse(command.removePrefix("wm size "))
                ?: throw IOException("Invalid wm size command")
            afterChange?.invoke(command)
            return ""
        }
    }
}
