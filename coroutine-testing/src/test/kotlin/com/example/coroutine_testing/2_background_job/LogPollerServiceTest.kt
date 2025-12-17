package com.example.coroutine_testing.`2_background_job`

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class LogPollerServiceTest {

  private val server = mockk<RemoteServer>(relaxed = true)
  private val service = LogPollerService(server)

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `should poll server exactly once every second`() = runTest {
    coEvery { server.fetchNewLogs() } returns listOf("Log A")

    // CRITICAL: Launch in backgroundScope.
    // This ensures the infinite loop is killed automatically when the test ends.
    backgroundScope.launch {
      service.startPolling()
    }

    // CHECKPOINT 1: T=0s
    // The loop starts immediately and calls fetchNewLogs() once, then hits delay(1000).
    runCurrent() // trigger pending tasks
    coVerify(exactly = 1) { server.fetchNewLogs() }

    // CHECKPOINT 2: T=1s
    // We advance 1 second. The delay finishes, loop restarts, fetches again.
    advanceTimeBy(1000)
    runCurrent()
    coVerify(exactly = 2) { server.fetchNewLogs() }

    // CHECKPOINT 3: T=10s
    // We fast forward 9 more seconds.
    advanceTimeBy(9000)
    runCurrent()

    // Total calls should be 1 (initial) + 1 (first advance) + 9 (last advance) = 11 calls
    coVerify(exactly = 11) { server.fetchNewLogs() }
  }
}
