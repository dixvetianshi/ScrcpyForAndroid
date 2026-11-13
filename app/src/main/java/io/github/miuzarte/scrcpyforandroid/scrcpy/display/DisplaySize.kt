package io.github.miuzarte.scrcpyforandroid.scrcpy.display

data class DisplaySize(val width: Int, val height: Int) {
    init {
        require(width > 0 && height > 0) { "Display dimensions must be positive" }
    }

    /** wm size uses the phone's natural orientation, not its current rotation. */
    fun orientedLike(physical: DisplaySize): DisplaySize {
        val shorter = minOf(width, height)
        val longer = maxOf(width, height)
        return when {
            physical.width < physical.height -> DisplaySize(shorter, longer)
            physical.width > physical.height -> DisplaySize(longer, shorter)
            else -> this
        }
    }

    override fun toString(): String = width.toString() + "x" + height

    companion object {
        private val FORMAT = Regex("""([1-9][0-9]*)x([1-9][0-9]*)""")

        fun parse(value: String): DisplaySize? {
            val match = FORMAT.matchEntire(value) ?: return null
            val width = match.groupValues[1].toIntOrNull() ?: return null
            val height = match.groupValues[2].toIntOrNull() ?: return null
            return DisplaySize(width, height)
        }
    }
}
