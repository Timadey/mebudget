package com.mebudget.app.data.sync

import com.mebudget.app.BuildConfig

object PocketBaseConfig {
    /**
     * Base URL of the PocketBase server, injected per build type:
     * debug → emulator loopback (`http://10.0.2.2:8090`), release → HTTPS.
     */
    val baseUrl: String = BuildConfig.POCKETBASE_URL
}