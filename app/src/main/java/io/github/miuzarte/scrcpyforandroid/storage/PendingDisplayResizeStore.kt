package io.github.miuzarte.scrcpyforandroid.storage

import android.content.Context
import io.github.miuzarte.scrcpyforandroid.scrcpy.display.DisplayResizeStore
import io.github.miuzarte.scrcpyforandroid.scrcpy.display.DisplaySize
import io.github.miuzarte.scrcpyforandroid.scrcpy.display.DisplaySizeSnapshot
import io.github.miuzarte.scrcpyforandroid.scrcpy.display.PendingDisplayResize
import org.json.JSONObject
import java.io.IOException

/**
 * Commit the original settings before issuing a device mutation. A failed
 * restore remains here across Activity recreation, app restart and target switches.
 */
internal class PendingDisplayResizeStore(context: Context) : DisplayResizeStore {
    private val preferences = context.getSharedPreferences(
        "pending_main_display_resize",
        Context.MODE_PRIVATE,
    )

    val hasPendingRecords: Boolean
        get() = preferences.all.isNotEmpty()

    override fun load(deviceSerial: String): PendingDisplayResize? {
        val source = preferences.getString(deviceSerial, null) ?: return null
        try {
            val json = JSONObject(source)
            val version = json.get("version")
            val serial = json.get("deviceSerial")
            require(version is Int && version == 1) { "Unsupported display backup version" }
            require(serial is String && serial == deviceSerial) {
                "Display backup belongs to another device"
            }
            require(json.has("overrideSize")) { "Display backup is missing its override state" }
            return PendingDisplayResize(
                deviceSerial = deviceSerial,
                original = DisplaySizeSnapshot(
                    physical = readSize(json.getJSONObject("physical")),
                    overrideSize = if (json.isNull("overrideSize")) null
                    else readSize(json.getJSONObject("overrideSize")),
                ),
                applied = readSize(json.getJSONObject("applied")),
            )
        } catch (error: Exception) {
            // Do not discard malformed owned backups or guess that no override existed.
            throw IOException("Cannot read the saved main display settings", error)
        }
    }

    override fun save(record: PendingDisplayResize) {
        val previous = load(record.deviceSerial)
        require(previous == null || previous == record) {
            "A previous main display restore is still pending"
        }
        val json = JSONObject()
            .put("version", 1)
            .put("deviceSerial", record.deviceSerial)
            .put("physical", writeSize(record.original.physical))
            .put(
                "overrideSize",
                record.original.overrideSize?.let(::writeSize) ?: JSONObject.NULL,
            )
            .put("applied", writeSize(record.applied))
        if (!preferences.edit().putString(record.deviceSerial, json.toString()).commit()) {
            throw IOException("Cannot save the original main display settings")
        }
    }

    override fun remove(deviceSerial: String) {
        if (!preferences.edit().remove(deviceSerial).commit()) {
            throw IOException("Cannot clear the completed main display restore")
        }
    }

    private fun readSize(json: JSONObject): DisplaySize {
        val width = json.get("width")
        val height = json.get("height")
        require(width is Int && height is Int && width > 0 && height > 0) {
            "Invalid saved display dimensions"
        }
        return DisplaySize(width, height)
    }

    private fun writeSize(size: DisplaySize): JSONObject = JSONObject()
        .put("width", size.width)
        .put("height", size.height)
}
