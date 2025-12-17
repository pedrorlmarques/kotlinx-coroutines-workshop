package com.example.coroutine_testing.`1_standard`

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class CoroutinesUnitTest {

  private val service = mockk<TestService>()
  private val controller = TestController(service)

  /**
   * TEST 1: Parallel Execution
   * PROOF: The logic works.
   * SPEED: Runs in ~10ms, even though code says "delay(1000)".
   */
  @Test
  fun `should get data in parallel using virtual time`() = runTest {
    coEvery { service.getProfile("1") } coAnswers {
      delay(1000) // Virtual Delay
      "Profile"
    }
    coEvery { service.getOrders("1") } coAnswers {
      delay(2000) // Virtual Delay
      "Orders"
    }
    val result = controller.getParallel("1")
    assertThat(result)
      .isEqualTo(listOf("Profile", "Orders"))
  }

  /**
   * TEST 2: Timeout Logic
   * SCENARIO: Service takes 5s, Timeout is 1s.
   * RESULT: Should return "Fallback".
   */
  @Test
  fun `should use fallback when service times out`() = runTest {
    coEvery { service.getOrders("1") } coAnswers {
      delay(5.seconds) // Takes 5s virtual time
      "Real Data"
    }
    // Timeout is 1000ms
    val result = controller.getTimeout("1")
    assertThat(result).isEqualTo("Fallback")
  }

  /**
   * TEST 3: Edge Case - Service is FASTER than timeout
   * SCENARIO: Service takes 0.5s, Timeout is 1s.
   * RESULT: Should return "Real Data".
   */
  @Test
  fun `should return real data when service is fast enough`() = runTest {
    coEvery { service.getOrders("1") } coAnswers {
      delay(500) // 0.5s (Fast enough!)
      "Real Data"
    }
    // Timeout is 1000ms
    val result = controller.getTimeout("1")
    assertThat(result).isEqualTo("Real Data")
  }

  /**
   * TEST 4: Supervisor / Crash Safety
   * SCENARIO: Service throws RuntimeException.
   * RESULT: Controller catches it and returns recovery message.
   */
  @Test
  fun `should recover from crash using supervisor`() = runTest {
    coEvery { service.getOrders("1") } throws RuntimeException("Boom!")
    val result = controller.getSupervisor("1")
    assertThat(result).isEqualTo("Recovered from Crash")
  }
}
