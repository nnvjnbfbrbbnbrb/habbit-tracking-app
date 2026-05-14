package com.ansangha.craxxjxbdbf

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansangha.craxxjxbdbf.permissions.PermissionOrchestrator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ansangha.craxxjxbdbf", appContext.packageName)
    }

    @Test
    fun permission_checklist_has_15_items() {
        assertEquals(15, PermissionOrchestrator.CHECKLIST_SIZE)
        assertEquals(15, PermissionOrchestrator.CheckId.entries.size)
    }
}
