package me.m64diamondstar.db

import io.ktor.http.*
import me.m64diamondstar.security.ApiKeyUtil
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

/**
 * Fetches an API key record by its ID.
 *
 * @param id The ID of the API key.
 * @return The corresponding [ApiKeyDto] if found, or `null` if the key does not exist.
 */
suspend fun getApiKeyById(id: Int) = suspendTransaction {
    ApiKeyDAO.findById(id)?.toDto()
}

/**
 * Retrieves all tags from the database.
 *
 * @return A list of [TagDto] objects representing all tags in the system.
 */
suspend fun getAllTags() = suspendTransaction {
    TagDAO.all().map { it.toDto() }
}

/**
 * Fetches a tag record by its ID.
 *
 * @param id The ID of the tag.
 * @return The corresponding [TagDto] if found, or `null` if the key does not exist.
 */
suspend fun getTagById(id: Int) = suspendTransaction {
    TagDAO.findById(id)?.toDto()
}

/**
 * Fetches a tag record by its name.
 *
 * @param name The name of the tag.
 * @return The corresponding [TagDto] if found, or `null` if the tag does not exist.
 */
suspend fun getTagByName(name: String) = suspendTransaction {
    TagDAO.find { TagsTable.name eq name }.firstOrNull()?.toDto()
}

/**
 * Deletes a tag by its ID.
 *
 * @param id The ID of the tag to delete.
 * @return [HttpStatusCode.OK] if deleted, [HttpStatusCode.NotFound] if the tag does not exist.
 */
suspend fun deleteTag(id: Int): HttpStatusCode = suspendTransaction {
    val tag = TagDAO.findById(id) ?: return@suspendTransaction HttpStatusCode.NotFound
    tag.delete()
    HttpStatusCode.OK
}

/**
 * Creates a new tag.
 *
 * @param name Tag name.
 */
suspend fun createTag(name: String): HttpStatusCode = suspendTransaction {
    val existing = TagDAO.find { TagsTable.name eq name }.firstOrNull()
    if (existing != null) {
        return@suspendTransaction HttpStatusCode.Conflict
    }
    TagDAO.new {
        this.name = name
    }
    return@suspendTransaction HttpStatusCode.OK
}

/**
 * Renames an existing tag by its ID.
 *
 * @param id The ID of the tag to rename.
 * @param newName The new name to assign to the tag.
 * @return [HttpStatusCode.OK] if rename was successful,
 *         [HttpStatusCode.Conflict] if the new name is already in use,
 *         or [HttpStatusCode.NotFound] if the tag does not exist.
 */
suspend fun renameTag(id: Int, newName: String): HttpStatusCode = suspendTransaction {
    val tag = TagDAO.findById(id) ?: return@suspendTransaction HttpStatusCode.NotFound
    val existing = TagDAO.find { TagsTable.name eq newName }.firstOrNull()
    if (existing != null) {
        return@suspendTransaction HttpStatusCode.Conflict
    }
    tag.name = newName
    HttpStatusCode.OK
}

/**
 * Retrieves all types from the database.
 *
 * @return A list of [TypeDto] objects representing all types in the system.
 */
suspend fun getAllTypes() = suspendTransaction {
    TypeDAO.all().map { it.toDto() }
}

/**
 * Fetches a type record by its name.
 *
 * @param name The name of the type.
 * @return The corresponding [TypeDto] if found, or `null` if the type does not exist.
 */
suspend fun getTypeByName(name: String) = suspendTransaction {
    TypeDAO.find { TypesTable.name eq name }.firstOrNull()?.toDto()
}

/**
 * Fetches a type record by its ID.
 *
 * @param id The ID of the type.
 * @return The corresponding [TypeDto] if found, or `null` if the key does not exist.
 */
suspend fun getTypeById(id: Int) = suspendTransaction {
    TypeDAO.findById(id)?.toDto()
}

/**
 * Creates a new API key in the database.
 *
 * Workflow:
 * - Uses the hashed key provided in [keyHash].
 * - Stores metadata such as description, permission level, associated Discord ID,
 *   rate limit, and whether the key is active.
 * - Sets the creation timestamp and initializes `lastUsedAt` as `null`.
 *
 * @param keyHash The hashed API key string to store.
 * @param description A descriptive note about the key (e.g., "Moderator key").
 * @param permissionLevel The access level of the key (higher values mean more privileges).
 * @param discordId Optional Discord ID of the owner; `null` if system-generated.
 * @param rateLimit Max allowed requests per minute for this key.
 * @param active Whether the key is currently active and valid.
 *
 * @return The newly created [ApiKeyDAO] entity.
 */
suspend fun createApiKey(
    keyHash: String,
    description: String,
    permissionLevel: Int,
    discordId: String?,
    rateLimit: Int,
    active: Boolean,
) = suspendTransaction {
    ApiKeyDAO.new {
        this.keyHash = keyHash
        this.description = description
        this.permissionLevel = permissionLevel
        this.discordId = discordId
        this.rateLimit = rateLimit
        this.active = active
        this.createdAt = LocalDateTime.now()
        this.lastUsedAt = null
    }
}

/**
 * Creates a new asset along with its associated tags.
 *
 * @param name Asset name.
 * @param description Asset description.
 * @param typeId ID of the asset type.
 * @param author Author of the asset.
 * @param material Material representation for GUI.
 * @param pasteLink Link to the raw paste.
 * @param rawData Full raw content from the paste server.
 * @param discordId Discord ID of the author.
 * @param tagIds List of tag IDs to attach to the asset.
 * @return The created AssetDto.
 */
suspend fun createAsset(
    name: String,
    description: String,
    typeId: Int,
    author: String,
    material: String,
    pasteLink: String,
    rawData: String,
    discordId: String,
    tagIds: List<Int>
): AssetDto = suspendTransaction {
    // Create the asset
    val asset = AssetDAO.new {
        this.name = name
        this.description = description
        this.type = TypeDAO.findById(typeId)!!
        this.author = author
        this.material = material
        this.pasteLink = pasteLink
        this.rawData = rawData
        this.discordId = discordId
        this.approved = false
        this.createdAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    // Filter valid tags
    val validTags = tagIds.mapNotNull { TagDAO.findById(it)?.id?.value }

    // Batch insert tags with ON CONFLICT IGNORE
    AssetTagsTable.batchInsert(validTags, true) { tagId ->
        this[AssetTagsTable.assetId] = asset.id.value
        this[AssetTagsTable.tagId] = tagId
    }

    val tagNames = TagDAO.find { TagsTable.id inList validTags }
        .map { it.name }

    asset.toDto(tagNames)
}

/**
 * Deletes an asset from the database by its ID.
 *
 * Workflow:
 * - Looks up the asset using [AssetDAO].
 * - If the asset exists, deletes it along with all related entries in `asset_tags`
 *   due to the ON DELETE CASCADE foreign key.
 * - Returns an appropriate HTTP status code.
 *
 * @param id The ID of the asset to delete.
 * @return [HttpStatusCode.OK] if the asset was successfully deleted,
 *         [HttpStatusCode.NotFound] if no asset with the given ID exists.
 */
suspend fun deleteAsset(id: Int): HttpStatusCode = suspendTransaction {
    val asset = AssetDAO.findById(id) ?: return@suspendTransaction HttpStatusCode.NotFound
    asset.delete()
    HttpStatusCode.OK
}

/**
 * Approves an asset (effect/show) by setting its `approved` field to true.
 *
 * Workflow:
 * - Fetches the asset with the given ID.
 * - If the asset does not exist or is already approved, returns `false`.
 * - Otherwise, updates the `approved` column to `true` and sets `updatedAt` to the current timestamp.
 *
 * @param id The ID of the asset to approve.
 * @return `true` if the asset was successfully approved; `false` if it was already approved or not found.
 */
suspend fun approveAsset(id: Int, approvedBy: String?): HttpStatusCode = suspendTransaction {
    val entry = AssetDAO.findById(id) ?: return@suspendTransaction HttpStatusCode.NotFound
    if(entry.approved) return@suspendTransaction HttpStatusCode.NotModified // Entry has already been approved
    AssetsTable.update({ AssetsTable.id eq id}) {
        it[this.approved] = true
        it[this.approvedBy] = approvedBy
        it[this.updatedAt] = CurrentDateTime
    }
    HttpStatusCode.OK
}

suspend fun updateMaterial(id: Int, material: String): HttpStatusCode = suspendTransaction {
    val entry = AssetDAO.findById(id) ?: return@suspendTransaction HttpStatusCode.NotFound
    if(entry.approved) return@suspendTransaction HttpStatusCode.NotModified // Entry has already been approved
    AssetsTable.update({ AssetsTable.id eq id}) {
        it[this.material] = material
        it[this.updatedAt] = CurrentDateTime
    }
    HttpStatusCode.OK
}

suspend fun updatePasteLink(id: Int, pasteLink: String): HttpStatusCode = suspendTransaction {
    val entry = AssetDAO.findById(id) ?: return@suspendTransaction HttpStatusCode.NotFound
    if(entry.approved) return@suspendTransaction HttpStatusCode.NotModified // Entry has already been approved
    AssetsTable.update({ AssetsTable.id eq id}) {
        it[this.pasteLink] = pasteLink
        it[this.updatedAt] = CurrentDateTime
    }
    HttpStatusCode.OK
}

/**
 * Updates the tags associated with an asset.
 *
 * Workflow:
 * - Removes all existing tag associations for the given asset ID.
 * - Inserts the new list of tag IDs using batchInsert with conflict ignore.
 *
 * @param id The ID of the asset to update.
 * @param tags A list of tag IDs to associate with the asset.
 * @return [HttpStatusCode.OK] if updated successfully, [HttpStatusCode.NotFound] if the asset does not exist.
 */
suspend fun updateTags(id: Int, tags: List<Int>): HttpStatusCode = suspendTransaction {
    val asset = AssetDAO.findById(id) ?: return@suspendTransaction HttpStatusCode.NotFound

    // Delete existing associations
    AssetTagsTable.deleteWhere { AssetTagsTable.assetId eq id }

    // Filter valid tag IDs
    val validTags = tags.mapNotNull { TagDAO.findById(it)?.id?.value }

    // Batch insert new tag associations with conflict ignore
    AssetTagsTable.batchInsert(validTags, ignore = true) { tagId ->
        this[AssetTagsTable.assetId] = id
        this[AssetTagsTable.tagId] = tagId
    }

    HttpStatusCode.OK
}

/**
 * Validates an API key against the database.
 *
 * Workflow:
 * - Hashes the provided key with [ApiKeyUtil.hashKey].
 * - Looks up the hashed key in the `api_keys` table.
 * - Verifies that the key exists, is active, and has a permission level
 *   greater than or equal to [minPermission].
 * - If validation succeeds, updates the `last_used_at` column to the current time.
 *
 * @param key The raw API key string provided in the request
 *            (e.g. from the `Authorization: Bearer <token>` header).
 *            Can be null, in which case validation fails.
 * @param minPermission The minimum required permission level
 *                      (defaults to 1 for read access).
 *
 * @return `true` if the key exists, is active, and meets the permission requirement;
 *         `false` otherwise.
 *
 * Security:
 * - Only hashed keys are stored in the database.
 * - Incoming keys are hashed before comparison, so raw keys are never persisted.
 *
 * Side effects:
 * - Updates the `last_used_at` field of the API key row on successful validation,
 *   which can be used for monitoring, cleanup, or auditing.
 */
suspend fun validateApiKey(key: String?, minPermission: Int = 1): Boolean {
    if (key == null) return false
    return suspendTransaction {
        val hashed = ApiKeyUtil.hashKey(key)
        val apiKey = ApiKeyDAO.find { ApiKeysTable.keyHash eq hashed }.firstOrNull()

        if (apiKey != null && apiKey.permissionLevel >= minPermission && apiKey.active) {
            // update last_used_at
            ApiKeysTable.update({ ApiKeysTable.id eq apiKey.id }) {
                it[lastUsedAt] = CurrentDateTime  // make sure lastUsedAt column is a timestamp
            }
            true
        } else {
            false
        }
    }
}