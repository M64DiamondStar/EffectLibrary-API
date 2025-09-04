package me.m64diamondstar.db.services

import me.m64diamondstar.db.*

/**
 * @return a list of assets.
 */
suspend fun getAllProvidedAssets() = suspendTransaction {
    AssetDAO.find { AssetsTable.approved eq true }
        .map { it.toDto() }
}

/**
 * Searches for an asset with the ID.
 * @param id the ID to query for.
 * @return the found asset, or null.
 */
suspend fun getAssetById(id: Int) = suspendTransaction {
    val asset = AssetDAO.findById(id) ?: return@suspendTransaction null

    // Inline the tag query here to avoid nested suspension
    val lowLevelCx = connection.connection as java.sql.Connection
    val stmt = lowLevelCx.prepareStatement("""
        SELECT t.name
        FROM tags t
        INNER JOIN asset_tags at ON at.tag_id = t.id
        WHERE at.asset_id = ?
    """.trimIndent())
    stmt.setInt(1, id)
    val rs = stmt.executeQuery()

    val tags = mutableListOf<String>()
    while (rs.next()) {
        tags.add(rs.getString("name"))
    }

    asset.toDto(tags)
}

/**
 * Retrieves the raw content of a specific asset (effect/show) by its ID.
 *
 * @param id The ID of the asset to fetch.
 * @return The raw data string of the asset, or `null` if the asset does not exist.
 */
suspend fun getRawData(id: Int) = suspendTransaction {
    AssetDAO.findById(id)?.rawData
}

/**
 * Searches approved assets by name (case-insensitive, partial match).
 *
 * @param query The search string.
 * @param limit Optional maximum number of results to return.
 * @return List of matching [AssetDto]s.
 */
suspend fun searchAssetsByName(query: String, limit: Int? = null): List<AssetDto> = suspendTransaction {
    val cx = connection.connection as java.sql.Connection
    val tagNamesMap = mutableMapOf<Int, MutableList<String>>()

    // First, get the asset IDs and main fields
    val sql = buildString {
        append("""
            SELECT a.id, a.name, a.description, ty.name AS type_name,
                   a.author, a.material, a.paste_link, a.approved, 
                   a.created_at, a.updated_at
            FROM assets a
            JOIN types ty ON ty.id = a.type_id
            WHERE a.approved = TRUE
              AND LOWER(a.name) LIKE ?
            ORDER BY a.created_at DESC
        """.trimIndent())
        if (limit != null) append(" LIMIT $limit")
    }

    val ps = cx.prepareStatement(sql)
    ps.setString(1, "%${query.lowercase()}%")
    val rs = ps.executeQuery()

    val assets = mutableListOf<AssetDto>()
    val assetIds = mutableListOf<Int>()

    while (rs.next()) {
        val id = rs.getInt("id")
        assetIds += id
        assets += AssetDto(
            id = id,
            name = rs.getString("name"),
            description = rs.getString("description"),
            type = rs.getString("type_name"),
            author = rs.getString("author"),
            material = rs.getString("material"),
            pasteLink = rs.getString("paste_link"),
            approved = rs.getBoolean("approved"),
            createdAt = rs.getTimestamp("created_at").toInstant().toString(),
            updatedAt = rs.getTimestamp("updated_at").toInstant().toString()
        )
    }

    // Fetch all tags for these assets in one query
    if (assetIds.isNotEmpty()) {
        val tagSql = """
            SELECT at.asset_id, t.name
            FROM asset_tags at
            JOIN tags t ON t.id = at.tag_id
            WHERE at.asset_id = ANY(?)
        """
        val tagPs = cx.prepareStatement(tagSql)
        tagPs.setArray(1, cx.createArrayOf("int", assetIds.toTypedArray()))
        val tagRs = tagPs.executeQuery()

        while (tagRs.next()) {
            val assetId = tagRs.getInt("asset_id")
            val tagName = tagRs.getString("name")
            tagNamesMap.getOrPut(assetId) { mutableListOf() }.add(tagName)
        }
    }

    // Inject tag names into AssetDto
    assets.map { asset ->
        asset.copy(tags = tagNamesMap[asset.id] ?: emptyList())
    }
}

/**
 * Fetches the latest approved assets.
 *
 * @param limit Maximum number of assets to return.
 * @return List of [AssetDto]s ordered by newest first.
 */
suspend fun getLatestAssets(limit: Int = 10): List<AssetDto> = suspendTransaction {
    val cx = connection.connection as java.sql.Connection
    val tagNamesMap = mutableMapOf<Int, MutableList<String>>()

    val sql = """
        SELECT a.id, a.name, a.description, ty.name AS type_name,
               a.author, a.material, a.paste_link, a.approved,
               a.created_at, a.updated_at
        FROM assets a
        JOIN types ty ON ty.id = a.type_id
        WHERE a.approved = TRUE
        ORDER BY a.created_at DESC
        LIMIT ?
    """
    val ps = cx.prepareStatement(sql)
    ps.setInt(1, limit)
    val rs = ps.executeQuery()

    val assets = mutableListOf<AssetDto>()
    val assetIds = mutableListOf<Int>()

    while (rs.next()) {
        val id = rs.getInt("id")
        assetIds += id
        assets += AssetDto(
            id = id,
            name = rs.getString("name"),
            description = rs.getString("description"),
            type = rs.getString("type_name"),
            author = rs.getString("author"),
            material = rs.getString("material"),
            pasteLink = rs.getString("paste_link"),
            approved = rs.getBoolean("approved"),
            createdAt = rs.getTimestamp("created_at").toInstant().toString(),
            updatedAt = rs.getTimestamp("updated_at").toInstant().toString()
        )
    }

    if (assetIds.isNotEmpty()) {
        val tagSql = """
            SELECT at.asset_id, t.name
            FROM asset_tags at
            JOIN tags t ON t.id = at.tag_id
            WHERE at.asset_id = ANY(?)
        """
        val tagPs = cx.prepareStatement(tagSql)
        tagPs.setArray(1, cx.createArrayOf("int", assetIds.toTypedArray()))
        val tagRs = tagPs.executeQuery()
        while (tagRs.next()) {
            val assetId = tagRs.getInt("asset_id")
            val tagName = tagRs.getString("name")
            tagNamesMap.getOrPut(assetId) { mutableListOf() }.add(tagName)
        }
    }

    assets.map { asset ->
        asset.copy(tags = tagNamesMap[asset.id] ?: emptyList())
    }
}

/**
 * Returns approved assets filtered by tag IDs.
 *
 * @param tagIds      List of tag IDs to filter by. If empty, returns an empty list.
 * @param limit       Optional max number of rows to return.
 * @param requireAll  If true, asset must contain ALL tags (AND). If false, ANY tag (OR).
 */
suspend fun filterAssetsByTags(
    tagIds: List<Int>,
    limit: Int? = null,
    requireAll: Boolean = false
): List<AssetDto> = suspendTransaction {
    if (tagIds.isEmpty()) return@suspendTransaction emptyList()

    val cx = connection.connection as java.sql.Connection
    val placeholders = tagIds.joinToString(",") { "?" }

    val baseSelect = """
        SELECT 
            a.id,
            a.name,
            a.description,
            ty.name AS type_name,
            a.author,
            a.material,
            a.paste_link,
            a.approved,
            a.created_at,
            a.updated_at
        FROM assets a
        JOIN types ty ON ty.id = a.type_id
    """.trimIndent()

    val sql = if (requireAll) {
        // AND semantics: asset must have all tagIds
        """
        $baseSelect
        JOIN asset_tags at ON at.asset_id = a.id
        WHERE a.approved = TRUE
          AND at.tag_id IN ($placeholders)
        GROUP BY 
            a.id, a.name, a.description, ty.name, 
            a.author, a.material, a.paste_link, a.approved, a.created_at, a.updated_at
        HAVING COUNT(DISTINCT at.tag_id) = ?
        ORDER BY a.created_at DESC
        ${if (limit != null) "LIMIT ?" else ""}
        """.trimIndent()
    } else {
        // OR semantics: asset must have at least one of tagIds
        """
        $baseSelect
        WHERE a.approved = TRUE
          AND EXISTS (
            SELECT 1 FROM asset_tags at
            WHERE at.asset_id = a.id AND at.tag_id IN ($placeholders)
          )
        ORDER BY a.created_at DESC
        ${if (limit != null) "LIMIT ?" else ""}
        """.trimIndent()
    }

    val ps = cx.prepareStatement(sql)
    var idx = 1
    // bind tagIds
    tagIds.forEach { ps.setInt(idx++, it) }
    // bind HAVING param for ALL match
    if (requireAll) {
        ps.setInt(idx++, tagIds.size)
    }
    // bind LIMIT if present
    if (limit != null) {
        ps.setInt(idx++, limit)
    }

    val rs = ps.executeQuery()
    val out = mutableListOf<AssetDto>()
    while (rs.next()) {
        out += AssetDto(
            id = rs.getInt("id"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            type = rs.getString("type_name"),
            author = rs.getString("author"),
            material = rs.getString("material"),
            pasteLink = rs.getString("paste_link"),
            approved = rs.getBoolean("approved"),
            createdAt = rs.getTimestamp("created_at").toInstant().toString(),
            updatedAt = rs.getTimestamp("updated_at").toInstant().toString()
        )
    }
    out
}