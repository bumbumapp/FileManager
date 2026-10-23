package com.securefilemanager.app

import android.app.Application

class FileManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Android 15+ draws edge-to-edge: pad activity content by the system bar insets.
        EdgeToEdgeCallbacks.register(this)
    }
}
