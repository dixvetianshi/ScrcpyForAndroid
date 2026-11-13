package io.github.miuzarte.scrcpyforandroid.scrcpy

import io.github.miuzarte.scrcpyforandroid.nativecore.AdbSocketStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PreparingSessionResourcesTest {
    @Test
    fun failedHandshakeClosesEveryPreparedChannelBeforeItsWorker() {
        val owner = PreparingSessionResources()
        val server = TrackingResource()
        val video = TrackingResource()
        val audio = TrackingResource()
        owner.own("server", server)
        owner.own("server log reader", Closeable { assertTrue(server.closed) })
        owner.own("video", video)
        owner.own("audio", audio)

        val failures = owner.closeAll()

        assertTrue(failures.isEmpty())
        assertTrue(server.closed)
        assertTrue(video.closed)
        assertTrue(audio.closed)
        assertFalse(owner.hasRetainedResources)
    }

    @Test
    fun successfulHandshakeTransfersExclusiveCleanupOwnershipToActiveSession() {
        val prepared = PreparingSessionResources()
        val active = PreparingSessionResources()
        val channel = TrackingResource()
        prepared.own("channel", channel)

        prepared.transferTo(active)
        assertFalse(prepared.hasRetainedResources)
        assertTrue(active.hasRetainedResources)
        assertTrue(prepared.closeAll().isEmpty())
        assertFalse(channel.closed)

        assertTrue(active.closeAll().isEmpty())
        assertTrue(channel.closed)
        assertEquals(1, channel.closeCount)
    }

    @Test
    fun mixedCleanupRetainsFailedReferencesAndRetriesOnlyThoseResources() {
        val owner = PreparingSessionResources()
        val first = TrackingResource()
        val failure = IOException("Still in use")
        val second = TrackingResource(failure)
        val last = TrackingResource()
        owner.own("first", first)
        owner.own("second", second)
        owner.own("last", last)

        val failures = owner.closeAll()

        assertEquals("second", failures.single().name)
        assertSame(failure, failures.single().cause)
        assertEquals(1, first.closeCount)
        assertEquals(1, second.closeCount)
        assertEquals(1, last.closeCount)
        assertTrue(owner.hasRetainedResources)

        second.failure = null
        assertTrue(owner.closeAll().isEmpty())
        assertEquals(1, first.closeCount)
        assertEquals(2, second.closeCount)
        assertEquals(1, last.closeCount)
        assertFalse(owner.hasRetainedResources)
    }

    @Test
    fun cancellingTheMetadataReadCleansItsPreparedProductionStream() = runBlocking {
        val owner = PreparingSessionResources()
        val stream = owner.own("metadata channel", AdbSocketStream(7, { _, _, _, _ -> }))
        val started = CountDownLatch(1)
        val reader = launch(Dispatchers.Default) {
            try {
                runInterruptible(Dispatchers.IO) {
                    started.countDown()
                    stream.inputStream.read()
                }
            } finally {
                withContext(NonCancellable) {
                    assertTrue(owner.closeAll().isEmpty())
                }
            }
        }

        assertTrue(started.await(1, TimeUnit.SECONDS))
        withTimeout(1_000) { reader.cancelAndJoin() }

        assertTrue(reader.isCancelled)
        assertTrue(stream.closed)
        assertFalse(owner.hasRetainedResources)
    }

    @Test
    fun cancellationDuringFinalCleanupStillAttemptsOtherResourcesAndAllowsRetry() {
        val owner = PreparingSessionResources()
        val cancellation = CancellationException("Cleanup interrupted")
        val interrupted = TrackingResource(cancellation)
        val sibling = TrackingResource()
        owner.own("interrupted", interrupted)
        owner.own("sibling", sibling)

        val failures = owner.closeAll()

        assertSame(cancellation, failures.single().cause)
        assertTrue(sibling.closed)
        assertTrue(owner.hasRetainedResources)
        interrupted.failure = null
        assertTrue(owner.closeAll().isEmpty())
        assertEquals(1, sibling.closeCount)
        assertEquals(2, interrupted.closeCount)
        assertFalse(owner.hasRetainedResources)
    }

    @Test
    fun failedSocketAttemptCanBeReleasedWithoutClosingThePreparedServer() {
        val owner = PreparingSessionResources()
        val server = TrackingResource()
        val failedAttempt = TrackingResource()
        val connected = TrackingResource()
        owner.own("server", server)
        owner.own("attempt", failedAttempt)

        assertEquals(null, owner.release(failedAttempt))
        assertTrue(failedAttempt.closed)
        assertFalse(server.closed)
        owner.own("connected", connected)
        assertTrue(owner.closeAll().isEmpty())
        assertTrue(server.closed)
        assertTrue(connected.closed)
        assertEquals(1, failedAttempt.closeCount)
    }

    @Test
    fun failedAttemptCloseIsRetainedAcrossAnOwnershipTransfer() {
        val prepared = PreparingSessionResources()
        val active = PreparingSessionResources()
        val failure = IOException("Socket cleanup failed")
        val attempted = TrackingResource(failure)
        prepared.own("attempt", attempted)

        assertSame(failure, prepared.release(attempted)?.cause)
        prepared.transferTo(active)
        assertFalse(prepared.hasRetainedResources)
        assertTrue(active.hasRetainedResources)
        attempted.failure = null
        assertTrue(active.closeAll().isEmpty())
        assertTrue(attempted.closed)
        assertEquals(2, attempted.closeCount)
    }

    @Test
    fun repeatedRegistrationOfTheSameResourceDoesNotCloseItTwice() {
        val owner = PreparingSessionResources()
        val channel = TrackingResource()
        owner.own("first channel", channel)
        owner.own("same channel", channel)

        assertTrue(owner.closeAll().isEmpty())
        assertEquals(1, channel.closeCount)
    }

    private class TrackingResource(var failure: Exception? = null) : Closeable {
        var closeCount = 0
        var closed = false

        override fun close() {
            closeCount++
            failure?.let { throw it }
            closed = true
        }
    }
}
