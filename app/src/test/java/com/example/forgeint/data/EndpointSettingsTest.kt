package com.example.weargemini.data

import com.example.forgeint.presentation.EndpointSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointSettingsTest {

    @Test
    fun tunnelEndpoint_usesHttpsAndAddsTunnelHeaders() {
        val endpoint = EndpointSettings(
            host = "demo.ngrok.app",
            port = "8080",
            authToken = "secret",
            funnelEnabled = false
        )

        assertTrue(endpoint.isTunnel)
        assertEquals("https://demo.ngrok.app/", endpoint.baseUrl(defaultPort = "8080"))
        assertEquals(
            mapOf(
                "Authorization" to "Bearer secret",
                "User-Agent" to "ForgeIntApp",
                "cf-terminate-connection" to "true",
                "ngrok-skip-browser-warning" to "true"
            ),
            endpoint.defaultHeaders()
        )
    }

    @Test
    fun localEndpoint_usesHttpAndResolvedPort() {
        val endpoint = EndpointSettings(
            host = "http://192.168.1.50",
            port = "9000"
        )

        assertFalse(endpoint.isTunnel)
        assertEquals("http://192.168.1.50:9000/", endpoint.baseUrl(defaultPort = "8080"))
        assertTrue(endpoint.defaultHeaders().isEmpty())
    }
}
