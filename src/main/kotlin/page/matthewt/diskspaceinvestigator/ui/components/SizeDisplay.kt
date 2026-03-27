package page.matthewt.diskspaceinvestigator.ui.components

import kotlin.math.ln
import kotlin.math.pow

object SizeDisplay {
    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")

    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
            .coerceIn(0, UNITS.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            "%.1f %s".format(value, UNITS[digitGroups])
        }
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatCount(count: Long): String {
        return when {
            count < 1_000 -> count.toString()
            count < 1_000_000 -> "%.1fK".format(count / 1_000.0)
            count < 1_000_000_000 -> "%.1fM".format(count / 1_000_000.0)
            else -> "%.1fB".format(count / 1_000_000_000.0)
        }
    }
}
