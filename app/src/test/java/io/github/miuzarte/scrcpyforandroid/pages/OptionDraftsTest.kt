package io.github.miuzarte.scrcpyforandroid.pages

import io.github.miuzarte.scrcpyforandroid.storage.ScrcpyOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OptionDraftsTest {
    private val base = ScrcpyOptions.defaultBundle()

    private fun drafts(profileId: String = "phone") = OptionDraftSnapshot(
        profileId = profileId,
        videoSource = "display",
        cameraSizeCustom = "",
        cameraSizeUseCustom = false,
        cameraSizes = emptyList(),
        cameraAr = "",
        cameraZoom = "",
        videoCodecOptions = "",
        audioCodecOptions = "",
        startAppCustom = "",
        startAppUseCustom = false,
        appPackages = emptyList(),
        mainDisplayWidth = "",
        mainDisplayHeight = "",
        newDisplayWidth = "",
        newDisplayHeight = "",
        newDisplayDpi = "",
        cropWidth = "",
        cropHeight = "",
        cropX = "",
        cropY = "",
    )

    @Test
    fun switchingProfilesCommitsAllUnfocusedDisplayDrafts() {
        val snapshot = drafts().copy(
            mainDisplayWidth = "1200",
            mainDisplayHeight = "1920",
            newDisplayWidth = "1920",
            newDisplayHeight = "1200",
            newDisplayDpi = "240",
            cropWidth = "1000",
            cropHeight = "800",
            cropX = "0",
            cropY = "0",
            videoCodecOptions = "i-frame-interval=2",
            audioCodecOptions = "bitrate=128000",
            startAppCustom = "com.example.game",
            startAppUseCustom = true,
            appPackages = listOf("com.example.game"),
        )

        val result = commitOptionDrafts("phone", base, snapshot)

        assertFalse(result.mainDisplaySizeRejected)
        assertEquals("1200x1920", result.bundle.mainDisplaySize)
        assertEquals("1920x1200/240", result.bundle.newDisplay)
        assertEquals("1000:800:0:0", result.bundle.crop)
        assertEquals("i-frame-interval=2", result.bundle.videoCodecOptions)
        assertEquals("bitrate=128000", result.bundle.audioCodecOptions)
        assertEquals("com.example.game", result.bundle.startApp)
        assertFalse(result.bundle.startAppUseCustom)
    }

    @Test
    fun equalSavedValuesDoNotAllowDraftsToCrossProfiles() {
        val snapshot = drafts("first").copy(
            mainDisplayWidth = "1200",
            mainDisplayHeight = "1920",
            videoCodecOptions = "i-frame-interval=2",
        )

        val result = commitOptionDrafts("second", base, snapshot)

        assertSame(base, result.bundle)
        assertFalse(result.mainDisplaySizeRejected)
    }

    @Test
    fun incompleteMainSizeKeepsPreviousValueAndReportsRejection() {
        val previous = base.copy(mainDisplaySize = "1080x2400")
        val snapshot = drafts().copy(
            mainDisplayWidth = "1200",
            videoCodecOptions = "i-frame-interval=2",
        )

        val result = commitOptionDrafts("phone", previous, snapshot)

        assertTrue(result.mainDisplaySizeRejected)
        assertEquals("1080x2400", result.bundle.mainDisplaySize)
        assertEquals("i-frame-interval=2", result.bundle.videoCodecOptions)
    }

    @Test
    fun clearingBothDimensionsReturnsToAutomaticSizing() {
        val previous = base.copy(mainDisplaySize = "1080x2400")

        val result = commitOptionDrafts("phone", previous, drafts())

        assertFalse(result.mainDisplaySizeRejected)
        assertEquals("", result.bundle.mainDisplaySize)
    }

    @Test
    fun cameraDraftsDoNotChangeHiddenMainDisplaySettings() {
        val previous = base.copy(videoSource = "camera", mainDisplaySize = "1080x2400")
        val snapshot = drafts().copy(
            videoSource = "camera",
            cameraSizeCustom = "1280x720",
            cameraSizeUseCustom = true,
            cameraSizes = listOf("1280x720"),
            cameraAr = "1.7778",
            cameraZoom = "2",
            mainDisplayWidth = "bad",
        )

        val result = commitOptionDrafts("phone", previous, snapshot)

        assertFalse(result.mainDisplaySizeRejected)
        assertEquals("1280x720", result.bundle.cameraSize)
        assertFalse(result.bundle.cameraSizeUseCustom)
        assertEquals("1.7778", result.bundle.cameraAr)
        assertEquals("2", result.bundle.cameraZoom)
        assertEquals("1080x2400", result.bundle.mainDisplaySize)
    }
}
