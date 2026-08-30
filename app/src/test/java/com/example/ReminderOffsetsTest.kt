package com.example

import com.example.core.prefs.parseOffsets
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderOffsetsTest {

    @Test fun `parses, dedupes and sorts stored offsets`() {
        assertEquals(listOf(10, 60, 1440), "60,10,60,1440".parseOffsets())
    }

    @Test fun `drops garbage and out-of-range values`() {
        assertEquals(listOf(10), "abc,-5,0,10,9.5,10081".parseOffsets())
    }

    @Test fun `falls back to default when nothing valid remains`() {
        assertEquals(listOf(10), ",".parseOffsets())
        assertEquals(listOf(10), "".parseOffsets())
    }
}
