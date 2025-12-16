# Spring Boot 4: Advanced Coroutines Patterns

This project demonstrates high-performance concurrency patterns using **Kotlin Coroutines** running on **Java 21 Virtual
Threads** (Project Loom).

It showcases how to handle parallel execution, timeouts, and partial failures in a **Blocking Spring MVC** application
without the complexity of Reactive (WebFlux) code.

## Architecture Framework: Spring Boot 4.0 (Spring Framework 7)

* **Language:** Kotlin 2.2.1
* **Concurrency:** Structured Concurrency (Coroutines)
* **Threading:** Virtual Threads (`spring.threads.virtual.enabled=true`)

---

## Usage
Run the application and test the endpoints:
* **Standard:** `GET /dashboard/{id}`
* **Graceful:** `GET /dashboard-graceful/{id}`
* **Best Effort:** `GET /flight-search`
* **Strict:** `GET /dashboard-strict/{id}`
* **Bulletproof:** `GET /dashboard-bulletproof/{id}`
* **Ultimate:** `GET /dashboard-ultimate/{id}`
---

## Pattern Reference

### 1. Parallel Decomposition (The "Standard")

**Goal:** Run multiple tasks at the same time and wait
for **all** of them to finish.

* **Behavior:** The total time is determined by the *slowest* task.
* **Failure:** If one task fails, the entire request fails (unless handled).
* **Code:** `coroutineScope` + `async` + `await`.

```kotlin
coroutineScope {
  val user = async { api.getUser() }       // 1s
  val orders = async { api.getOrders() }   // 2s

  // Returns in ~2s
  Dashboard(user.await(), orders.await())
}

```

**Use Case:**

* Aggregating data where **all fields are mandatory** (e.g., an Invoice PDF requiring User + Product + Tax info).

---

### 2. Graceful Degradation (Granular Timeout)

**Goal:** "I want the whole dashboard, but if the **Orders** widget is too
slow, just show the rest without it."

* **Behavior:** Critical tasks wait indefinitely. Optional/risky tasks have a specific timeout applied *inside* their
  async block.
* **Failure:** The slow task returns `null`; the request succeeds partially.
* **Code:** `async { withTimeoutOrNull(...) }`.

```kotlin
coroutineScope {
  val user = async { api.getUser() }

  // Only this specific task has a deadline
  val orders = async {
    withTimeoutOrNull(1000) { api.getOrders() }
  }

  // Returns User + (Orders OR null)
  Dashboard(user.await(), orders.await() ?: "Widget Unavailable")
}

```

**Use Case:**

* **User Dashboards** (Profile is critical; "Recommended Friends" is optional).
* **News Sites** (Article content is critical; "Comments section" is optional).

---

### 3. Best Effort Search (Global Hard Deadline)

**Goal:** "Get me as many results as possible in exactly 1 second. Leave
the slow ones behind."

* **Behavior:** A global timer runs. When it expires, **stop everything**. Return whatever finished; cancel whatever
  didn't.
* **Failure:** Slow tasks are cancelled and excluded from the result.
* **Code:** `withTimeoutOrNull` wrapping the **collection** of results.

```kotlin
coroutineScope {
  val taskA = async { providerA.search() } // Fast
  val taskB = async { providerB.search() } // Slow

  // The Bus leaves at 1s!
  val results = withTimeoutOrNull(1000) {
    listOf(taskA.await(), taskB.await())
  }

  // If timeout hit, manually check who isCompleted
  if (results == null) {
    return listOf(taskA, taskB).filter { it.isCompleted }.map { it.getCompleted() }
  }
}

```

**Use Case:**

* **Aggregators** (Flight Search, Hotel Prices).
* **Real-time Bidding** (Ad Tech).
* Scenarios where **Speed > Completeness**.

---

### 4. Strict Timeout (Circuit Breaker)

**Goal:** "If you can't do it in 2 seconds, don't do it at all."

* **Behavior:** Enforces a strict SLA. If the time limit is exceeded, the request is aborted immediately with an error.
* **Failure:** Throws `TimeoutCancellationException` -> HTTP 504 Gateway Timeout.
* **Code:** `withTimeout` (throws exception on failure).

```kotlin
try {
  withTimeout(2000) {
    val user = async { api.getUser() }
    val orders = async { api.getOrders() }
    // ... logic ...
  }
} catch (e: TimeoutCancellationException) {
  throw ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT)
}

```

**Use Case:**

* **Financial Transactions** (Payment processing).
* **Critical SLAs** (APIs that contractually must respond < 500ms).
* Preventing system overload by failing fast.

---

### 5. Bulletproof Supervisor (Resilient)

**Goal:** "I don't trust ANY service. If Profile crashes, show 'Guest'. If Orders crash, show 'Empty'. Never show a 500
Error."

* **Failure:** Exceptions in children are isolated. Fallbacks are returned for each specific failure.
* **Code:** `supervisorScope` + `runCatching { ...await() }`.

```kotlin
supervisorScope {
  // If this fails, the scope does NOT cancel the others
  val pDeferred = async { api.getUser() }
  val oDeferred = async { api.getOrders() }

  // Safely unwrap each result individually
  val user = runCatching { pDeferred.await() }.getOrElse { "Guest" }
  val orders = runCatching { oDeferred.await() }.getOrElse { emptyList() }

  Dashboard(user, orders)
}

```
---

### 6. The Ultimate Defensive (Timeout + Crash Safety)

**Goal:** "Handle everything: Crashing services, Hanging services, and Slow services."

* **Failure:** Handles both **Timeouts** AND **Exceptions** gracefully.
* **Code:** `supervisorScope` + `runCatching` + `withTimeout`.

```kotlin
supervisorScope {
  val orders = async {
    runCatching {
      withTimeout(1000) { api.getOrders() }
    }
  }

  val result = orders.await().fold(
    onSuccess = { it },
    onFailure = { e ->
      if (e is TimeoutCancellationException) "Too Slow" else "Crashed"
    }
  )
}

```

---

## Comparison Cheat Sheet:

| Feature       | Pattern 1: Parallel    | Pattern 2: Graceful                    | Pattern 3: Best Effort                   | Pattern 4: Strict                       | Pattern 5: Bulletproof                    | Pattern 6: Ultimate                                     |
|---------------|------------------------|----------------------------------------|------------------------------------------|-----------------------------------------|-------------------------------------------|---------------------------------------------------------|
| **Logic**     | Wait for ALL.          | Wait for ALL, kill slow optional ones. | Wait X seconds. Stop. Grab what's ready. | Wait X seconds. If not done, **Crash**. | Wait for ALL. If any crash, use fallback. | Wait X sec PER TASK. If crash OR timeout, use fallback. |
| **Scope**     | `coroutineScope`       | `coroutineScope`                       | `coroutineScope`                         | `coroutineScope`                        | **`supervisorScope`**                     | **`supervisorScope`**                                   |
| **Timeout**   | None.                  | **Inner** (Specific Task).             | **Outer** (The list).                    | **Outer** (The request).                | None.                                     | **Inner** (Inside `runCatching`).                       |
| **Result**    | Complete Data.         | Partial Data (Nulls allowed).          | Partial List (Missing items).            | **Error** (HTTP 504).                   | **Total Resilience** (Fallbacks).         | **Total Resilience** (Specific Error Msgs).             |
| **Crash?**    | **Yes** (If any fail). | **Yes** (If critical fail).            | **No** (Timeouts only).                  | **Yes** (Always).                       | **No** (Never crashes).                   | **No** (Never crashes).                                 |
| **User Exp.** | **Slowest.**           | **Fast.** Missing widgets are empty.   | **Fast.** "Here is what we found".       | **Bad.** Error page.                    | **Good.** Always shows page.              | **Perfect.** Fast & Informative.                        |

