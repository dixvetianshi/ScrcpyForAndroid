package io.github.miuzarte.scrcpyforandroid.scrcpy.display

/** A complete wm size response; an absent override is distinct from an invalid one. */
data class DisplaySizeSnapshot(
    val physical: DisplaySize,
    val overrideSize: DisplaySize? = null,
) {
    val effective: DisplaySize
        get() = overrideSize ?: physical

    companion object {
        private val DECLARATION = Regex("""(Physical|Override) size:\s*(.*)""")

        fun parse(output: String): DisplaySizeSnapshot? {
            var physical: DisplaySize? = null
            var overrideSize: DisplaySize? = null
            var overrideSeen = false
            for (rawLine in output.lineSequence()) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue
                val declaration = DECLARATION.matchEntire(line) ?: return null
                val size = DisplaySize.parse(declaration.groupValues[2]) ?: return null
                when (declaration.groupValues[1]) {
                    "Physical" -> {
                        if (physical != null) return null
                        physical = size
                    }
                    "Override" -> {
                        if (overrideSeen) return null
                        overrideSeen = true
                        overrideSize = size
                    }
                }
            }
            return DisplaySizeSnapshot(physical ?: return null, overrideSize)
        }
    }
}
