package io.github.miuzarte.scrcpyforandroid.nativecore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class BoundShellResultTest {
    @Test
    fun preservesWmSizeOutputWithCompleteStatus() {
        assertEquals(
            "Physical size: 1080x2400\nOverride size: 1200x1920",
            parseBoundShellResult(
                "Physical size: 1080x2400\nOverride size: 1200x1920\n\nSTATUS=0\n",
                "STATUS",
            ),
        )
    }

    @Test
    fun acceptsEmptySuccessfulMutationOutput() {
        assertEquals("", parseBoundShellResult("\nSTATUS=0\n", "STATUS"))
    }

    @Test
    fun failureIncludesPermissionReason() {
        val error = assertThrows(IOException::class.java) {
            parseBoundShellResult("Permission denied\n\nSTATUS=1\n", "STATUS")
        }
        assertTrue(error.message.orEmpty().contains("Permission denied"))
    }

    @Test
    fun refusesUnacknowledgedOrMalformedCompletion() {
        for (response in listOf("Physical size: 1080x2400", "\nSTATUS=x\n", "\nSTATUS=0\nextra")) {
            assertThrows(IOException::class.java) { parseBoundShellResult(response, "STATUS") }
        }
    }
}
