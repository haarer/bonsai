package com.receiptedger.app.di

import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot handoff of a freshly captured image path from the Scan screen to the
 * Review screen. Navigation-argument free: the path is consumed (and cleared)
 * when the ReviewViewModel is created.
 */
@Singleton
class CapturedImageHolder @Inject constructor() {

    private var path: String? = null

    fun set(path: String) {
        this.path = path
    }

    /** Returns and clears the held path; null if none is pending. */
    fun consume(): String? = path.also { path = null }
}
