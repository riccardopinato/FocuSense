package com.utilitylab.focussense.domain

import org.junit.Assert.*
import org.junit.Test

class SessionContextTest {
    @Test
    fun unknownContextFallsBackToWork() {
        assertEquals(SessionContext.WORK, SessionContext.fromKey("UNKNOWN"))
    }

    @Test
    fun tagsAreTrimmedDeduplicatedAndLimited() {
        val result = SessionTagCodec.normalize(
            listOf(" Casa ", "casa", "Musica", "", "Uno", "Due", "Tre", "Quattro", "Cinque")
        )
        assertEquals(5, result.size)
        assertEquals(1, result.count { it.equals("Casa", ignoreCase = true) })
        assertFalse(result.any { it.isBlank() })
    }

    @Test
    fun separatorCannotBreakCodec() {
        val decoded = SessionTagCodec.decode(
            SessionTagCodec.encode(listOf("casa|anna", "studio"))
        )
        assertEquals(2, decoded.size)
        assertFalse(decoded.any { it.contains("|") })
    }
}
