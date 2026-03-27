package page.matthewt.diskspaceinvestigator.ui

import page.matthewt.diskspaceinvestigator.ui.components.SizeDisplay
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SizeDisplayTest {

    @Test
    fun `format zero bytes`() {
        assertEquals("0 B", SizeDisplay.format(0))
    }

    @Test
    fun `format bytes`() {
        assertEquals("512 B", SizeDisplay.format(512))
    }

    @Test
    fun `format kilobytes`() {
        assertEquals("1.0 KB", SizeDisplay.format(1024))
        assertEquals("1.5 KB", SizeDisplay.format(1536))
    }

    @Test
    fun `format megabytes`() {
        assertEquals("1.0 MB", SizeDisplay.format(1024L * 1024))
        assertEquals("10.0 MB", SizeDisplay.format(10L * 1024 * 1024))
    }

    @Test
    fun `format gigabytes`() {
        assertEquals("1.0 GB", SizeDisplay.format(1024L * 1024 * 1024))
    }

    @Test
    fun `format terabytes`() {
        assertEquals("1.0 TB", SizeDisplay.format(1024L * 1024 * 1024 * 1024))
    }

    @Test
    fun `format petabytes`() {
        assertEquals("1.0 PB", SizeDisplay.format(1024L * 1024 * 1024 * 1024 * 1024))
    }

    @Test
    fun `format negative returns zero`() {
        assertEquals("0 B", SizeDisplay.format(-1))
    }

    @Test
    fun `formatCount small numbers`() {
        assertEquals("0", SizeDisplay.formatCount(0))
        assertEquals("999", SizeDisplay.formatCount(999))
    }

    @Test
    fun `formatCount thousands`() {
        assertEquals("1.0K", SizeDisplay.formatCount(1_000))
        assertEquals("10.5K", SizeDisplay.formatCount(10_500))
    }

    @Test
    fun `formatCount millions`() {
        assertEquals("1.0M", SizeDisplay.formatCount(1_000_000))
        assertEquals("5.5M", SizeDisplay.formatCount(5_500_000))
    }

    @Test
    fun `formatCount billions`() {
        assertEquals("1.0B", SizeDisplay.formatCount(1_000_000_000))
    }
}
