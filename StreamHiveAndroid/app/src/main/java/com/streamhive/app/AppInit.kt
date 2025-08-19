package com.streamhive.app

import android.app.Application
import com.streamhive.app.net.ApiClient

object AppInit {
    fun init(app: Application) {
        val backend = BuildConfig.BACKEND_BASE_URL.ifBlank { "http://10.0.2.2:8080/" }
        ApiClient.init(app, if (backend.endsWith('/')) backend else "$backend/")
    }
}

