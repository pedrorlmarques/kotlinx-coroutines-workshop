package com.example.coroutine_testing.`3_time_travel`

interface AuthClient {
  suspend fun login(user: String): String
}
