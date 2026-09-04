package com.mastercompanion.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CommandRequest(
    val action: String,
    val params: Map<String, String> = emptyMap()
)

@Serializable
data class CommandResponse(
    val status: String,
    val message: String,
    val action: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class CommandDefinition(
    val action: String,
    val name: String,
    val description: String,
    val category: String,
    val requiresRoot: Boolean
)

