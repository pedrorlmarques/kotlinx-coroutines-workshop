# coroutine-testing

## Purpose

This module demonstrates how to write, structure, and execute tests for Kotlin coroutine-based code in a Kotlin project using Gradle. It aims to help developers understand asynchronous programming challenges and how Kotlin's coroutine testing tools address them.

## Tech Stack & Versions

- **Kotlin:** 2.2.1+
- **Coroutines:** 2.2.1+
- **Testing:** `kotlinx-coroutines-test`
- **Mocking:** `MockK` 1.14.7+

## Project Structure and Test Modules

The test suite is organized into focused modules, each demonstrating a specific aspect of coroutine testing:

- **1_standard** Basic unit tests for coroutines, covering coroutine launch, execution, and suspending functions.  
  _Key files:_
  - `CoroutinesUnitTest.kt`: Unit tests for standard coroutine usage
  - `TestController.kt`: Supporting test controller for coroutine scenarios

- **2_background_job** Demonstrates testing of background jobs and services using coroutines, including concurrency and cancellation.  
  _Key files:_
  - `LogPollerService.kt`: Example background service using coroutines
  - `LogPollerServiceTest.kt`: Tests for background job behavior, cancellation, and error handling
  - `RemoteServer.kt`: Mocked remote server for integration in tests

- **3_time_travel** Focuses on testing time-based logic using virtual time, such as delays and authentication timeouts.  
  _Key files:_
  - `AuthClient.kt`: Client for authentication logic
  - `AuthenticationManager.kt`: Manages authentication with coroutine-based timeouts
  - `AuthenticationManagerTest.kt`: Tests for time-based authentication scenarios using virtual time

Each module is located under `src/test/kotlin/com/example/coroutine_testing/` and is self-contained, illustrating best practices for coroutine testing in different contexts.

## What is being tested

- **Coroutine launch and execution:** Verifying that coroutines start, execute, and complete as expected, including correct sequencing and dispatcher usage.
- **Suspending functions:** Testing `suspend` functions for correct results, exception handling, and proper interaction with other coroutines or Java code.
- **Concurrency and parallelism:** Simulating multiple coroutines running in parallel, checking for race conditions, thread safety, and correct shared state updates.
- **Exception handling:** Ensuring exceptions in coroutines are caught, propagated, or handled as intended, and that error scenarios are covered.
- **Coroutine cancellation:** Verifying that coroutines can be cancelled, resources are cleaned up, and no memory or thread leaks occur.
- **Timeouts and delays:** Using virtual time to simulate delays, timeouts, and scheduled tasks, making tests fast and deterministic.

## How Kotlin helps

Kotlin's `kotlinx-coroutines-test` library provides:

- **TestCoroutineDispatcher & TestCoroutineScope:** Control coroutine execution, pause/resume dispatchers, and advance virtual time for deterministic testing.
- **runTest:** Simplifies writing tests for suspending functions and coroutine builders, handling setup and teardown automatically.
- **Virtual time control:** Simulate delays and timeouts instantly, making tests fast and reliable.
- **Structured concurrency:** Encourages safe coroutine hierarchies, making it easier to test and reason about concurrent code.

## Cheat Sheet

| Tool | Usage | When to use |
| :--- | :--- | :--- |
| **`runTest`** | `runTest { ... }` | The entry point for **all** coroutine unit tests. Skips delays automatically. |
| **`advanceTimeBy(ms)`** | `advanceTimeBy(1000)` | Fast-forwards virtual time. Use to test intermediate states (e.g., "Is spinner still loading?"). |
| **`runCurrent()`** | `runCurrent()` | Executes any tasks currently pending at the *exact* current virtual time. |
| **`backgroundScope`** | `backgroundScope.launch` | Launches infinite loops or jobs that should auto-cancel when the test ends. |
| **`Turbine`** | `flow.test { ... }` | (Recommended) The best way to assert sequences of data streams (Flows). |

## When to use coroutine testing

- When your code uses `suspend` functions, `launch`, or `async`.
- When verifying behavior under concurrent or parallel execution.
- When testing error handling, cancellation, or time-based logic.
- When ensuring robustness against race conditions and resource leaks.
- When integrating Kotlin coroutines with Java code.

## Best practices

- **Inject Dispatchers:** Never hardcode `Dispatchers.IO` in production code. Always inject a `DispatcherProvider` so tests can substitute them with `StandardTestDispatcher`.
- **Use `runTest`:** Wrap all coroutine-based unit tests in this builder.
- **Virtual Time:** Prefer virtual time (`delay()`) over real delays (`Thread.sleep`) to keep tests blazing fast.
- **Test Cancellation:** Ensure your background jobs clean up resources correctly when cancelled.
- **Isolate State:** Use thread-safe structures or confinement when testing parallel code to avoid flaky tests.
- **Cleanup:** Clean up resources and jobs after each test (MockK and `runTest` handle most of this automatically).

## How to run the tests

1. Ensure you have Gradle and JDK installed.
2. In the project root, run:

```bash
./gradlew test
