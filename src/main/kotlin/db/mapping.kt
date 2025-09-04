package me.m64diamondstar.db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// ---------- TYPES ----------
object TypesTable : IntIdTable("types") {
    val name = varchar("name", 50)
}

class TypeDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TypeDAO>(TypesTable)

    var name by TypesTable.name
}

// ---------- TAGS ----------
object TagsTable : IntIdTable("tags") {
    val name = varchar("name", 50)
}

class TagDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TagDAO>(TagsTable)

    var name by TagsTable.name
}

// ---------- ASSETS ----------
object AssetsTable : IntIdTable("assets") {
    val name = varchar("name", 100)
    val type = reference("type_id", TypesTable)
    val description = text("description")
    val author = varchar("author", 50)
    val material = varchar("material", 50)
    val pasteLink = varchar("paste_link", 150)
    val rawData = text("raw_data")
    val discordId = varchar("discord_id", 50)
    val approved = bool("approved").default(false)
    val approvedBy = varchar("approved_by", 50).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

class AssetDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AssetDAO>(AssetsTable)

    var name by AssetsTable.name
    var type by TypeDAO referencedOn AssetsTable.type
    var description by AssetsTable.description
    var author by AssetsTable.author
    var material by AssetsTable.material
    var pasteLink by AssetsTable.pasteLink
    var rawData by AssetsTable.rawData
    var discordId by AssetsTable.discordId
    var approved by AssetsTable.approved
    var approvedBy by AssetsTable.approvedBy
    var createdAt by AssetsTable.createdAt
    var updatedAt by AssetsTable.updatedAt
}

// ---------- ASSET_TAGS ----------
object AssetTagsTable : Table("asset_tags") {
    val assetId = integer("asset_id").references(AssetsTable.id, onDelete = ReferenceOption.CASCADE)
    val tagId = integer("tag_id").references(TagsTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(assetId, tagId, name = "PK_AssetTags")
}

// ---------- API KEYS ----------
object ApiKeysTable : IntIdTable("api_keys") {
    val keyHash = varchar("key_hash", 64)
    val description = text("description")
    val permissionLevel = integer("permission_level") // use Long for up to 64-bit flags
    val discordId = varchar("discord_id", 50).nullable()
    val rateLimit = integer("rate_limit").default(0)
    val active = bool("active").default(true)
    val createdAt = datetime("created_at")
    val lastUsedAt = datetime("last_used_at").nullable()
}

class ApiKeyDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ApiKeyDAO>(ApiKeysTable)

    var keyHash by ApiKeysTable.keyHash
    var description by ApiKeysTable.description
    var permissionLevel by ApiKeysTable.permissionLevel
    var discordId by ApiKeysTable.discordId
    var rateLimit by ApiKeysTable.rateLimit
    var active by ApiKeysTable.active
    var createdAt by ApiKeysTable.createdAt
    var lastUsedAt by ApiKeysTable.lastUsedAt
}

// ---------- SUSPENDED TRANSACTION HELPER ----------
suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)