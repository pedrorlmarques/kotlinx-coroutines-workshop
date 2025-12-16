package com.example.dashboard_coroutines

import kotlinx.coroutines.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class DashboardController(private val remoteService: RemoteService) {

  /**
   * 1. Basic Parallel Execution.
   * <p>
   * This endpoint demonstrates how to execute multiple independent tasks concurrently
   * using {@code async}. The total response time is determined by the <b>slowest</b>
   * task in the group.
   * </p>
   *
   * <ul>
   * <li><b>Pattern:</b> Parallel Decomposition</li>
   * <li><b>Behavior:</b> All tasks start at the same time. We wait for all to finish.</li>
   * <li><b>Failure Mode:</b> If ONE task fails (throws exception), the entire scope fails
   * and the request is aborted (unless using {@code supervisorScope}).</li>
   * <li><b>Latency:</b> Max(Task A, Task B, Task C).</li>
   * </ul>
   *
   * @param id The user ID.
   * @return A map containing results from all services.
   */
  @GetMapping("/dashboard/{id}")
  suspend fun getDashboard(@PathVariable id: String): Map<String, Any> {
    val startTime = System.currentTimeMillis()

    // 1. Create a scope for parallel execution
    val result = coroutineScope {
      println("Starting parallel fetch on thread: ${Thread.currentThread()}")

      // 2. Launch 3 Async tasks
      // 'async' starts the coroutine immediately
      val profileDeferred = async {
        println("Fetching profile...")
        remoteService.getUserProfile(id)
      }

      val ordersDeferred = async {
        println("Fetching orders...")
        remoteService.getUserOrders(id)
      }

      val scoreDeferred = async {
        println("Fetching score...")
        remoteService.getCreditScore(id)
      }

      // 3. Wait for all of them to finish
      // The controller will suspend here until the slowest one (Orders) is done.
      mapOf(
        "user" to profileDeferred.await(),
        "orders" to ordersDeferred.await(),
        "score" to scoreDeferred.await()
      )
    }

    val totalTime = System.currentTimeMillis() - startTime
    println("Done in $totalTime ms")

    // Add timing info to response just so you can see it in the browser
    return result + ("_time_taken" to totalTime)
  }

  /**
   * 2. Granular Timeout (Graceful Degradation).
   * <p>
   * This endpoint applies a timeout <b>only to specific, non-critical tasks</b>.
   * Critical data (like User Profile) is allowed to take as long as needed, but
   * secondary data (like Orders) is dropped if it takes too long.
   * </p>
   *
   * <ul>
   * <li><b>Pattern:</b> Graceful Degradation / Partial Content.</li>
   * <li><b>Behavior:</b> We wait for critical tasks indefinitely (or until global timeout).
   * We wait for optional tasks only up to X seconds.</li>
   * <li><b>Use Case:</b> Dashboards where "Profile" must load, but "Ads" or "History"
   * can be skipped to speed up the page load.</li>
   * </ul>
   *
   * @param id The user ID.
   * @return A map where 'orders' might contain a fallback message if it timed out.
   */
  @GetMapping("/dashboard-graceful/{id}")
  suspend fun getGracefulDashboard(@PathVariable id: String): Map<String, Any> = coroutineScope {
    println("\n--- NEW REQUEST: Graceful Dashboard ---")
    val startTime = System.currentTimeMillis()

    // 1. Launch Async Tasks
    val profile = async { remoteService.getUserProfile(id) }
    val score = async { remoteService.getCreditScore(id) }

    // 2. Orders with Timeout
    val orders = async {
      println("  [Controller] ⏳ Requesting orders (1.8s timeout)...")
      withTimeoutOrNull(1800) {
        remoteService.getUserOrders(id)
      }
    }

    // 3. Await Results
    val profileResult = profile.await()
    val scoreResult = score.await()
    val ordersResult = orders.await() // Will be NULL if timed out

    // 4. Log Result
    if (ordersResult == null) {
      println("  [Controller] ⚠️ Orders timed out and were dropped.")
    } else {
      println("  [Controller] ✅ All data received.")
    }

    val totalTime = System.currentTimeMillis() - startTime
    println("--- COMPLETED in $totalTime ms ---\n")

    mapOf(
      "user" to profileResult,
      "score" to scoreResult,
      "orders" to (ordersResult ?: "⚠️ Unavailable (Timeout)"),
      "_time_taken" to totalTime
    )
  }

  /**
   * 3. Best Effort Search (Global Hard Deadline).
   * <p>
   * This endpoint applies a <b>Global Timeout</b> to the entire group of tasks.
   * We guarantee the client waits NO LONGER than the specified time (e.g., 1s).
   * Whatever data is ready by then is returned; everything else is cancelled.
   * </p>
   *
   * <ul>
   * <li><b>Pattern:</b> Best Effort / Aggregator.</li>
   * <li><b>Behavior:</b> Fire off 10 requests. At T=1.0s, stop waiting.
   * Collect successful results, ignore the rest.</li>
   * <li><b>Use Case:</b> Flight Search, Real-time Bidding, "Find my Price".
   * Speed is more important than completeness.</li>
   * </ul>
   *
   * @return A map containing only the providers that finished within 1000ms.
   */
  @GetMapping("/flight-search")
  suspend fun searchFlights(): Map<String, Any> {
    println("\n--- NEW REQUEST: Flight Search ---")
    val startTime = System.currentTimeMillis()

    val response = coroutineScope {

      // Launch 3 providers
      val taskA = async { remoteService.searchProvider("Expedia", 200) }
      val taskB = async { remoteService.searchProvider("Kayak", 800) }
      val taskC = async { remoteService.searchProvider("SlowAir", 5000) }

      // SLA: 1000ms Hard Limit
      println("  [Controller] ⏳ Waiting for results (Max 1s)...")

      val allResults = withTimeoutOrNull(1000) {
        listOf(taskA.await(), taskB.await(), taskC.await())
      }

      if (allResults != null) {
        println("  [Controller] ✅ All providers finished in time!")
        return@coroutineScope mapOf("status" to "Complete", "data" to allResults)
      }

      // Timeout hit!
      println("  [Controller] ⏰ Timeout hit! Harvesting partial results...")

      val partialData = listOf(taskA, taskB, taskC)
        .filter { it.isCompleted }
        .map { it.getCompleted() }

      println("  [Controller] Harvested ${partialData.size} results. (SlowAir is dying...)")

      mapOf(
        "status" to "Partial (Timeout)",
        "data" to partialData,
        "missing_providers" to listOf(taskA, taskB, taskC).count { !it.isCompleted }
      )
    }

    val totalTime = System.currentTimeMillis() - startTime
    println("--- COMPLETED in $totalTime ms ---\n")

    return response + ("_time_taken" to totalTime)
  }

  /**
   * 4. Strict Timeout (All or Nothing).
   * <p>
   * This endpoint enforces a <b>Hard Deadline</b> on the entire operation.
   * Unlike "Best Effort", we do not return partial results. If the deadline is missed,
   * the entire request is considered a failure, and we return an error.
   * </p>
   *
   * <ul>
   * <li><b>Pattern:</b> SLA Enforcement / Circuit Breaking.</li>
   * <li><b>Behavior:</b> If tasks take > 1.8s, throw an exception immediately.</li>
   * <li><b>Result:</b> HTTP 504 (Gateway Timeout) or a custom Error JSON.</li>
   * </ul>
   */
  @GetMapping("/dashboard-strict/{id}")
  suspend fun getStrictDashboard(@PathVariable id: String): ResponseEntity<Any> {
    println("\n--- NEW REQUEST: Strict Dashboard ---")
    val startTime = System.currentTimeMillis()

    try {
      // 1. Wrap the entire logic in a Hard Timeout
      // If this block takes longer than 1800ms, it throws TimeoutCancellationException
      return withTimeout(1800) {

        // This scope inherits the timeout
        coroutineScope {
          val profile = async { remoteService.getUserProfile(id) } // 1s
          val orders = async { remoteService.getUserOrders(id) }   // 2s (Too slow!)
          val score = async { remoteService.getCreditScore(id) }   // 1.5s

          val data = mapOf(
            "user" to profile.await(),
            "orders" to orders.await(),
            "score" to score.await()
          )

          // We only reach here if EVERYTHING finished before 1.8s
          ResponseEntity.ok(data)
        }
      }
    } catch (e: TimeoutCancellationException) {
      val totalTime = System.currentTimeMillis() - startTime
      println("  [Controller] 🛑 STRICT TIMEOUT! Cancelled everything at ${totalTime}ms")

      // Return a 504 Gateway Timeout error
      return ResponseEntity.status(504).body(
        mapOf(
          "error" to "Gateway Timeout",
          "message" to "The request took too long (>1.8s) and was cancelled.",
          "status" to "FAILED"
        )
      )
    }
  }

  /**
   * 5. Bulletproof Supervisor (Generic Protection).
   * <p>
   * This endpoint assumes ANY service might fail. It uses `supervisorScope` so that
   * one crash doesn't kill the neighbors. It uses `runCatching` to safely
   * unwrap the results without verbose try-catch blocks.
   * </p>
   */
  @GetMapping("/dashboard-bulletproof/{id}")
  suspend fun getBulletproofDashboard(@PathVariable id: String) = supervisorScope {
    println("\n--- NEW REQUEST: Bulletproof Dashboard ---")
    val startTime = System.currentTimeMillis()

    // 1. Launch ALL tasks.
    // We use supervisorScope, so if one fails, the others keep running.
    // Note: We don't need try-catch INSIDE the async here because
    // exceptions are thrown when we call .await(), not when we launch.
    val profileDeferred = async { remoteService.getUserProfile(id) }
    val ordersDeferred = async { remoteService.getOrdersWithCrash(id) } // This will crash
    val scoreDeferred = async { remoteService.getCreditScore(id) }

    // 2. Await ALL results safely.
    // 'runCatching' wraps the .await() call.
    // It returns a Result<T> object which is either Success or Failure.
    val profileResult = runCatching { profileDeferred.await() }
    val ordersResult = runCatching { ordersDeferred.await() }
    val scoreResult = runCatching { scoreDeferred.await() }

    // 3. Unpack and Log
    // We use .getOrElse { fallback } to handle errors generically.
    val response = mapOf(
      "user" to profileResult.getOrElse {
        println("  [Controller] ❌ User failed: ${it.message}")
        "⚠️ Guest (Service Error)"
      },
      "orders" to ordersResult.getOrElse {
        println("  [Controller] ❌ Orders failed: ${it.message}")
        "⚠️ Orders Unavailable"
      },
      "score" to scoreResult.getOrElse {
        println("  [Controller] ❌ Score failed: ${it.message}")
        -1 // Fallback value for int
      }
    )

    val totalTime = System.currentTimeMillis() - startTime
    println("--- COMPLETED in $totalTime ms ---\n")

    return@supervisorScope response + ("_time_taken" to totalTime)
  }

  /**
   * 6. The Ultimate Defensive Dashboard.
   * <p>
   * Combines <b>Supervisor</b> (Isolation) + <b>Timeout</b> (Latency Control) + <b>Catching</b> (Crash Safety).
   * </p>
   * <ul>
   * <li>If Profile crashes? -> Returns "Guest".</li>
   * <li>If Orders hangs > 1s? -> Returns "Orders Unavailable (Timeout)".</li>
   * <li>If Score is fast? -> Returns 750.</li>
   * </ul>
   */
  @GetMapping("/dashboard-ultimate/{id}")
  suspend fun getUltimateDashboard(@PathVariable id: String): Map<String, Any> = supervisorScope {
    println("\n--- NEW REQUEST: Ultimate Dashboard ---")
    val startTime = System.currentTimeMillis()

    // 1. Profile: Safe + 5s Timeout (Generous)
    val profileDeferred = async {
      runCatching {
        withTimeout(5000) { remoteService.getUserProfile(id) }
      }
    }

    // 2. Orders: Safe + 1s Timeout (Strict)
    val ordersDeferred = async {
      runCatching {
        // If this takes > 1000ms, it throws TimeoutCancellationException
        // runCatching will catch it and treat it as a failure.
        withTimeout(1000) { remoteService.getUserOrders(id) }
      }
    }

    // 3. Score: Safe + 2s Timeout
    val scoreDeferred = async {
      runCatching {
        withTimeout(2000) { remoteService.getCreditScore(id) }
      }
    }

    // 4. Unpack Results
    val response = mapOf(
      "user" to profileDeferred.await().getOrElse {
        "⚠️ Guest (Error: ${it.message})"
      },

      // We can even distinguish between "Timeout" and "Crash" if we want!
      "orders" to ordersDeferred.await().fold(
        onSuccess = { it },
        onFailure = { e ->
          if (e is TimeoutCancellationException) "⚠️ Orders Timeout (Too Slow)"
          else "⚠️ Orders Error (Crashed)"
        }
      ),

      "score" to scoreDeferred.await().getOrElse { -1 }
    )

    val totalTime = System.currentTimeMillis() - startTime
    println("--- COMPLETED in $totalTime ms ---\n")

    return@supervisorScope response + ("_time_taken" to totalTime)
  }
}
