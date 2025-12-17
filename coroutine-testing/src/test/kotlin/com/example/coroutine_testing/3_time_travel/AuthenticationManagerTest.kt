package com.example.coroutine_testing.`3_time_travel`

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AuthenticationManagerTest {

  private val authClient = mockk<AuthClient>()
  private val manager = AuthenticationManager(authClient)

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `should show 'Authenticating' state during slow network call`() = runTest {
    // We simulate a network call that takes exactly 5 seconds
    coEvery { authClient.login("user1") } coAnswers {
      delay(5000) // Virtual Delay inside the mock!
      "ACCESS_TOKEN_123"
    }

    // Launch the action in a background coroutine so we can control time while it runs
    val job = launch {
      manager.performLogin("user1")
    }

    // CHECKPOINT 1: T=0s (Immediately after launch)
    // The coroutine has started and hit the 'client.login' line.
    // It is now suspended inside our mock's delay(5000).
    // The status should be updated to "Authenticating...".
    runCurrent() // Ensure the coroutine has advanced to the suspension point
    assertThat(manager.status.value).isEqualTo("Authenticating...")

    // CHECKPOINT 2: T=2s (Middle of the call)
    // We fast-forward 2 seconds. The mock is still sleeping.
    advanceTimeBy(2000)
    assertThat(manager.status.value).isEqualTo("Authenticating...")

    // CHECKPOINT 3: T=5s (Call finishes)
    // We fast-forward past the 5s mark.
    advanceTimeBy(3001) // 2000 + 3001 > 5000

    // Now the mock returns "ACCESS_TOKEN_123" and the function continues.
    assertThat(manager.status.value).isEqualTo("Success: ACCESS_TOKEN_123")

    // Cleanup
    job.cancel()
  }
}
