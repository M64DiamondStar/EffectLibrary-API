package me.m64diamondstar.db

import kotlinx.serialization.Serializable

@Serializable
data class AssetDto(
    val id: Int,
    val name: String,
    val description: String,
    val type: String,
    val author: String,
    val material: String,
    val pasteLink: String,
    val approved: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val tags: List<String> = emptyList()
)

fun AssetDAO.toDto(tags: List<String> = emptyList()) = AssetDto(
    id = id.value,
    name = name,
    description = description,
    type = type.name,
    author = author,
    material = material,
    pasteLink = pasteLink,
    approved = approved,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    tags = tags
)

@Serializable
data class ApiKeyDto(
    val id: Int,
    val description: String,
    val permissionLevel: Int,
    val discordId: String?,
    val rateLimit: Int,
    val active: Boolean,
    val createdAt: String,
    val lastUsedAt: String
)

fun ApiKeyDAO.toDto() = ApiKeyDto(
    id = id.value,
    description = description,
    permissionLevel = permissionLevel,
    discordId = discordId,
    rateLimit = rateLimit,
    active = active,
    createdAt = createdAt.toString(),
    lastUsedAt = lastUsedAt.toString()
)

@Serializable
data class TagDto(
    val id: Int,
    val name: String
)

fun TagDAO.toDto() = TagDto(
    id = id.value,
    name = name
)

@Serializable
data class TypeDto(
    val id: Int,
    val name: String
)

fun TypeDAO.toDto() = TypeDto(
    id = id.value,
    name = name
)