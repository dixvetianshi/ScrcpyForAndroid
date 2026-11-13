package io.github.miuzarte.scrcpyforandroid.pages

import io.github.miuzarte.scrcpyforandroid.models.ScrcpyOptions.Crop
import io.github.miuzarte.scrcpyforandroid.models.ScrcpyOptions.NewDisplay
import io.github.miuzarte.scrcpyforandroid.scrcpy.display.DisplaySize
import io.github.miuzarte.scrcpyforandroid.storage.ScrcpyOptions

/** Unfocused text belongs to one profile, independently of its saved values. */
internal data class OptionDraftSnapshot(
    val profileId: String,
    val videoSource: String,
    val cameraSizeCustom: String,
    val cameraSizeUseCustom: Boolean,
    val cameraSizes: List<String>,
    val cameraAr: String,
    val cameraZoom: String,
    val videoCodecOptions: String,
    val audioCodecOptions: String,
    val startAppCustom: String,
    val startAppUseCustom: Boolean,
    val appPackages: List<String>,
    val mainDisplayWidth: String,
    val mainDisplayHeight: String,
    val newDisplayWidth: String,
    val newDisplayHeight: String,
    val newDisplayDpi: String,
    val cropWidth: String,
    val cropHeight: String,
    val cropX: String,
    val cropY: String,
)

internal data class OptionDraftCommit(
    val bundle: ScrcpyOptions.Bundle,
    val mainDisplaySizeRejected: Boolean = false,
)

internal fun commitOptionDrafts(
    profileId: String,
    bundle: ScrcpyOptions.Bundle,
    snapshot: OptionDraftSnapshot?,
): OptionDraftCommit {
    if (snapshot == null || snapshot.profileId != profileId) return OptionDraftCommit(bundle)

    var next = bundle
    if (snapshot.videoSource == "camera") {
        if (snapshot.cameraSizeUseCustom) {
            next = if (snapshot.cameraSizeCustom in snapshot.cameraSizes) {
                next.copy(
                    cameraSize = snapshot.cameraSizeCustom,
                    cameraSizeUseCustom = false,
                )
            } else {
                next.copy(cameraSizeCustom = snapshot.cameraSizeCustom)
            }
        }
        next = next.copy(cameraAr = snapshot.cameraAr, cameraZoom = snapshot.cameraZoom)
    }
    next = next.copy(
        videoCodecOptions = snapshot.videoCodecOptions,
        audioCodecOptions = snapshot.audioCodecOptions,
    )
    if (snapshot.startAppUseCustom) {
        next = if (snapshot.startAppCustom in snapshot.appPackages) {
            next.copy(startApp = snapshot.startAppCustom, startAppUseCustom = false)
        } else {
            next.copy(startAppCustom = snapshot.startAppCustom)
        }
    }

    var mainDisplaySizeRejected = false
    if (snapshot.videoSource == "display") {
        val mainDisplaySize = mainDisplaySizeFromInputs(snapshot.mainDisplayWidth, snapshot.mainDisplayHeight)
        mainDisplaySizeRejected = mainDisplaySize == null
        if (mainDisplaySize != null) next = next.copy(mainDisplaySize = mainDisplaySize)
        next = next.copy(
            newDisplay = NewDisplay.parseFrom(
                snapshot.newDisplayWidth,
                snapshot.newDisplayHeight,
                snapshot.newDisplayDpi,
            ).toString(),
            crop = Crop.parseFrom(
                snapshot.cropWidth,
                snapshot.cropHeight,
                snapshot.cropX,
                snapshot.cropY,
            ).toString(),
        )
    }
    return OptionDraftCommit(next, mainDisplaySizeRejected)
}

internal fun mainDisplaySizeFromInputs(width: String, height: String): String? {
    val w = width.trim()
    val h = height.trim()
    if (w.isEmpty() && h.isEmpty()) return ""
    val size = "${w}x${h}"
    return size.takeIf { DisplaySize.parse(it) != null }
}
