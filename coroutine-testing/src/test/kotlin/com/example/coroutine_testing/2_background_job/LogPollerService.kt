package com.example.coroutine_testing.`2_background_job`

import kotlinx.coroutines.delay

class LogPollerService(private val server: RemoteServer) {

  // This runs forever!
  suspend fun startPolling() {
    while (true) {
      server.fetchNewLogs()
      delay(1000)
    }
  }
}
