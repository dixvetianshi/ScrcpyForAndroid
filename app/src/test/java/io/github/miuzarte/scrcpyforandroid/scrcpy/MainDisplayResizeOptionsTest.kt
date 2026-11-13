package io.github.miuzarte.scrcpyforandroid.scrcpy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class MainDisplayResizeOptionsTest {
    @Test
    fun ordinaryProfilesRemainUnaffected() {
        val options = ClientOptions()
        assertFalse(options.resizeMainDisplay)
        options.validate()
    }

    @Test
    fun acceptsDefaultOrExplicitMainDisplayWithCustomDimensions() {
        for (display in listOf(-1, 0)) {
            ClientOptions(
                resizeMainDisplay = true,
                mainDisplaySize = "1200x1920",
                displayId = display,
            ).validate()
        }
    }

    @Test
    fun refusesInvalidDimensionsOrConflictingDisplaySelection() {
        for (options in listOf(
            ClientOptions(resizeMainDisplay = true, mainDisplaySize = "1200x"),
            ClientOptions(resizeMainDisplay = true, mainDisplaySize = "0x1920"),
            ClientOptions(resizeMainDisplay = true, newDisplay = "1920x1200"),
            ClientOptions(resizeMainDisplay = true, displayId = 1),
            ClientOptions(resizeMainDisplay = true, videoSource = Shared.VideoSource.CAMERA),
            ClientOptions(resizeMainDisplay = true, video = false),
        )) {
            assertThrows(IllegalArgumentException::class.java) { options.validate() }
        }
    }
}
