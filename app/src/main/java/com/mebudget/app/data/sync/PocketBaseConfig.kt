package com.mebudget.app.data.sync

data class PocketBaseConfig(
    val baseUrl: String
) {
    companion object {
        /** Android emulator maps 10.0.2.2 to the host machine's localhost. */
        const val DEFAULT_DEV_URL = "http://10.0.2.2:8090"
    }
}
