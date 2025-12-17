package com.example.coroutine_testing.`2_background_job`

interface RemoteServer {
  suspend fun fetchNewLogs(): List<String>
}
