package io.github.miuzarte.scrcpyforandroid.scrcpy.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisplaySizeSnapshotTest {
    @Test
    fun completeResponsesPreservePhysicalAndOptionalOverride() {
        assertEquals(
            DisplaySizeSnapshot(DisplaySize(1080, 2400)),
            DisplaySizeSnapshot.parse("\r\nPhysical size: 1080x2400\r\n"),
        )
        val snapshot = DisplaySizeSnapshot.parse(
            "Physical size: 1080x2400\nOverride size: 1200x1920\n",
        )
        assertEquals(
            DisplaySizeSnapshot(DisplaySize(1080, 2400), DisplaySize(1200, 1920)),
            snapshot,
        )
        assertEquals(DisplaySize(1200, 1920), snapshot?.effective)
    }

    @Test
    fun partialMalformedDuplicateAndUnexpectedDeclarationsAreRejected() {
        listOf(
            "",
            "Override size: 1200x1920",
            "Physical size: 1080x2400\nOverride size:",
            "Physical size: 1080x2400\nOverride size: not-a-size",
            "Physical size: 1080x2400\nOverride size: 0x1920",
            "Physical size: 1080x2400\nPhysical size: 1080x2400",
            "Physical size: 1080x2400\nOverride size: 1200x1920\nOverride size: 1200x1920",
            "Physical size: 1080x2400\nPermission denied",
            "Physical size: 1080x2400\nOverride density: 320",
        ).forEach { output ->
            assertNull(output, DisplaySizeSnapshot.parse(output))
        }
    }

    @Test
    fun dimensionsUseStrictPositiveIntegerFormat() {
        assertEquals(DisplaySize(2560, 1600), DisplaySize.parse("2560x1600"))
        listOf(
            "0x1600",
            "2560x0",
            "-1x1600",
            "02560x1600",
            "2560X1600",
            "2560 x 1600",
            "2560x1600px",
            " 2560x1600 ",
            "2147483648x1600",
        ).forEach { input -> assertNull(input, DisplaySize.parse(input)) }
    }

    @Test
    fun requestedDimensionsFollowThePhonesNaturalOrientation() {
        val tablet = DisplaySize(2560, 1600)
        assertEquals(DisplaySize(1600, 2560), tablet.orientedLike(DisplaySize(1080, 2400)))
        assertEquals(DisplaySize(2560, 1600), tablet.orientedLike(DisplaySize(2400, 1080)))
        assertEquals(tablet, tablet.orientedLike(DisplaySize(1080, 1080)))
    }
}
