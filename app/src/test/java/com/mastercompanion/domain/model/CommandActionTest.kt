package com.mastercompanion.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandActionTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `serialize and deserialize CommandRequest`() {
        val request = CommandRequest(
            action = "navigate",
            params = mapOf("page" to "1")
        )

        val serialized = json.encodeToString(request)
        val deserialized = json.decodeFromString<CommandRequest>(serialized)

        assertEquals("navigate", deserialized.action)
        assertEquals("1", deserialized.params["page"])
    }

    @Test
    fun `serialize and deserialize CommandResponse`() {
        val response = CommandResponse(
            status = "ok",
            message = "Pong",
            action = "ping"
        )

        val serialized = json.encodeToString(response)
        val deserialized = json.decodeFromString<CommandResponse>(serialized)

        assertEquals("ok", deserialized.status)
        assertEquals("Pong", deserialized.message)
        assertEquals("ping", deserialized.action)
    }

    @Test
    fun `deserialize CommandDefinition list`() {
        val jsonStr = """
            [
              {
                "action": "toggle_charge_limit",
                "name": "Toggle Charge Limit",
                "description": "Enables or disables 80% battery bypass",
                "category": "battery",
                "requiresRoot": true
              }
            ]
        """.trimIndent()

        val list = json.decodeFromString<List<CommandDefinition>>(jsonStr)
        assertEquals(1, list.size)
        assertEquals("toggle_charge_limit", list[0].action)
        assertTrue(list[0].requiresRoot)
    }
}
