package io.github.miuzarte.scrcpyforandroid.nativecore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AdbDisplayShellStreamTest {
    @Test
    fun localCloseWakesAStillOpeningStream() {
        val stream = AdbSocketStream(3, { _, _, _, _ -> })
        stream.close()
        val error = assertThrows(IOException::class.java) { stream.awaitOpen(100) }
        assertTrue(error.message.orEmpty().contains("rejected"))
    }

    @Test
    fun lateAcknowledgmentClosesTheRemoteStream() {
        val sent = mutableListOf<Triple<Int, Int, Int>>()
        val stream = AdbSocketStream(3, { command, local, remote, _ ->
            sent += Triple(command, local, remote)
        })
        stream.close()
        stream.onRemoteOkay(7)
        assertEquals(listOf(Triple(0x45534c43, 3, 7)), sent)
        assertTrue(stream.closed)
        assertEquals(-1, stream.inputStream.read())
    }

    @Test
    fun cancellationInterruptsTheProductionStreamReader() = runBlocking {
        val stream = AdbSocketStream(3, { _, _, _, _ -> })
        val started = CountDownLatch(1)
        val reader = launch(Dispatchers.Default) {
            runInterruptible(Dispatchers.IO) {
                started.countDown()
                stream.inputStream.read()
            }
        }
        assertTrue(started.await(1, TimeUnit.SECONDS))
        try {
            withTimeout(1_000) { reader.cancelAndJoin() }
            assertTrue(reader.isCancelled)
        } finally {
            stream.close()
        }
    }
}
