package com.example.coroutine_testing.`1_standard`

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

interface TestService {
  suspend fun getProfile(id: String): String
  suspend fun getOrders(id: String): String
}

@RestController
class TestController(private val service: TestService) {

  // Pattern 1: Parallel
  @GetMapping("/parallel")
  suspend fun getParallel(id: String): List<String> = coroutineScope {
    val p = async { service.getProfile(id) }
    val o = async { service.getOrders(id) }
    listOf(p.await(), o.await())
  }

  // Pattern 2: Graceful Timeout
  @GetMapping("/timeout")
  suspend fun getTimeout(id: String): String = coroutineScope {
    val orders = async {
      // Wait max 1000ms
      withTimeoutOrNull(1000) { service.getOrders(id) }
    }
    orders.await() ?: "Fallback"
  }

  // Pattern 3: Supervisor (Crash Safety)
  @GetMapping("/supervisor")
  suspend fun getSupervisor(id: String): String = supervisorScope {
    val ordersDeferred = async { service.getOrders(id) }

    // Safely unwrap
    runCatching { ordersDeferred.await() }
      .getOrElse { "Recovered from Crash" }
  }
}
