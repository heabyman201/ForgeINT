package com.example.forgeint.presentation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgeHardwareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun publishStats_withSustainedHighVram_invokesRolloverCallback() = runTest {
        var callbackTelemetry: VramTelemetry? = null

        val api = object : HardwareApi {
            override suspend fun getHardwareStats(): HardwareResponse {
                return HardwareResponse(
                    Children = listOf(
                        HardwareNode(
                            id = 1,
                            Text = "NVIDIA GeForce RTX 4060",
                            Children = listOf(
                                HardwareNode(
                                    id = 2,
                                    Text = "GPU Memory Used",
                                    Value = "7.7 GB"
                                ),
                                HardwareNode(
                                    id = 3,
                                    Text = "GPU Memory Total",
                                    Value = "8 GB"
                                )
                            )
                        )
                    )
                )
            }
        }

        val viewModel = ForgeHardwareViewModel(
            repository = HardwareRepository(api, "http://localhost"),
            onSustainedVramPressure = { telemetry ->
                callbackTelemetry = telemetry
                true
            }
        )

        viewModel.setVramPressureStartAtForTest(
            System.currentTimeMillis() - SUSTAINED_WINDOW_MS - 1_000L
        )

        viewModel.publishStatsForTest(
            G14Stats(
                dgpuVram = "7.7 GB",
                dgpuVramTotal = "8 GB"
            )
        )

        assertNotNull("Expected rollover callback telemetry", callbackTelemetry)
        assertTrue(
            "Expected callback telemetry to report pressure",
            callbackTelemetry!!.isUnderPressure()
        )
        assertTrue(
            "Expected published global telemetry to reflect the same high VRAM pressure",
            HardwareTelemetryState.vramTelemetry.value.isUnderPressure()
        )
    }

    companion object {
        private const val SUSTAINED_WINDOW_MS = 5 * 60 * 1000L
    }
}
