package com.example.dashboard_coroutines

import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import kotlin.coroutines.cancellation.CancellationException

@Service
class RemoteService {

  // Simulates fetching User Profile (takes 1 second)
  suspend fun getUserProfile(id: String): Map<String, String> {
    println("  [Service] 🟢 Profile fetch started... (Thread: ${Thread.currentThread()})")
    delay(1000)
    println("  [Service] ✅ Profile ready")
    return mapOf("id" to id, "name" to "John Doe")
  }

  // Simulates fetching Orders (takes 2 seconds)
  suspend fun getUserOrders(id: String): List<String> {
    println("  [Service] 🟢 Orders fetch started... (Thread: ${Thread.currentThread()})")
    try {
      delay(2000) // Slow!
      println("  [Service] ✅ Orders ready")
      return listOf("Order #101", "Order #102")
    } catch (e: CancellationException) {
      println("  [Service] ❌ Orders cancelled!")
      throw e
    }
  }

  // Simulates fetching Credit Score (takes 1.5 seconds)
  suspend fun getCreditScore(id: String): Int {
    println("  [Service] 🟢 Score fetch started... (Thread: ${Thread.currentThread()})")
    delay(1500)
    println("  [Service] ✅ Score ready")
    return 750
  }

  suspend fun searchProvider(name: String, delayMs: Long): String {
    println("  [Service] ✈️ $name search started... (Thread: ${Thread.currentThread()})")
    try {
      delay(delayMs)
      println("  [Service] ✅ $name found results")
      return "Flight from $name ($delayMs ms)"
    } catch (e: CancellationException) {
      println("  [Service] ⚠️ $name search was cancelled!")
      throw e
    }
  }

  // Add this to RemoteService.kt
  suspend fun getOrdersWithCrash(id: String): List<String> {
    delay(500) // It works for a bit...
    throw RuntimeException("Database Connection Failed!") // Then crashes
  }
}
