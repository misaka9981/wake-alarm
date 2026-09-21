package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AppInfoTest {
    @Test
    fun nameIsStable() {
        assertEquals("Wake Alarm", AppInfo.NAME)
    }
}
