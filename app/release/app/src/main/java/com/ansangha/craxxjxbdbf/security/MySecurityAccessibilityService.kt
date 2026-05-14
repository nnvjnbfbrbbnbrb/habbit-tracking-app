package com.ansangha.craxxjxbdbf.security

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Declared for accessibility-based device monitoring (parental / MDM-style use).
 * Enable in Settings → Accessibility; no extra manifest permission beyond BIND_ACCESSIBILITY_SERVICE.
 */
class MySecurityAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally minimal: wire domain-specific handling here when needed.
    }

    override fun onInterrupt() {
        // no-op
    }
}
