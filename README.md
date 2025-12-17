# kotlinx-coroutines-workshop

This repository is a collection of hands-on modules and examples to explore and learn about Kotlin Coroutines in
practical scenarios.

## Modules

### dashboard-coroutines

The `dashboard-coroutines` module is a Kotlin-based server-side application that demonstrates practical usage of Kotlin
Coroutines. It showcases how to build responsive dashboards by leveraging asynchronous programming, structured
concurrency, and integration with remote services. The module includes:

- A controller (`DashboardController.kt`) that handles HTTP requests and coordinates data fetching.
- A service layer (`RemoteService.kt`) that simulates or interacts with external systems asynchronously.
- An application entry point (`DashboardCoroutinesApplication.kt`) to bootstrap the server.

This module is designed to help developers understand coroutine patterns for non-blocking I/O, parallel data fetching,
and efficient resource management in backend applications.

### coroutine-testing

The `coroutine-testing` module demonstrates how to write, structure, and execute tests for Kotlin coroutine-based code
in a Kotlin project using Gradle. It covers:

- Unit testing coroutine launch, execution, and suspending functions.
- Testing concurrency, parallelism, and exception handling.
- Verifying coroutine cancellation and time-based logic using virtual time.
- Integration scenarios between Kotlin coroutines and Java code.

This module is organized into focused test submodules:

- **1_standard**: Basic coroutine unit tests.
- **2_background_job**: Testing background jobs, concurrency, and cancellation.
- **3_time_travel**: Time-based logic and virtual time testing.

Each submodule is self-contained and illustrates best practices for coroutine testing in different contexts.

---

More modules will be added to cover additional coroutine use cases and patterns.
