package com.example.coroutine_testing.`3_time_travel`

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthenticationManager(private val client: AuthClient) {
  // State we want to observe
  private val _status = MutableStateFlow("Idle")
  val status = _status.asStateFlow()

  suspend fun performLogin(user: String) {
    _status.value = "Authenticating..."
    val token = client.login(user)
    _status.value = "Success: $token"
  }
}
