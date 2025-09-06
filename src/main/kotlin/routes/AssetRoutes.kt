package me.m64diamondstar.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.m64diamondstar.db.approveAsset
import me.m64diamondstar.db.updatePasteLink
import me.m64diamondstar.db.updateMaterial
import me.m64diamondstar.db.createAsset
import me.m64diamondstar.db.deleteAsset
import me.m64diamondstar.db.services.*
import me.m64diamondstar.db.updateTags
import me.m64diamondstar.security.isAllowed

@Serializable
data class CreateAssetRequest(
    val name: String,
    val description: String,
    val typeId: Int,
    val author: String,
    val material: String,
    val pasteLink: String,
    val rawData: String,
    val discordId: String,
    val tagsIds: List<Int>
)

@Serializable
data class ApproveAssetRequest(
    val id: Int,
    val approvedBy: String? = null
)

@Serializable
data class UpdateMaterialRequest(
    val id: Int,
    val material: String
)

@Serializable
data class UpdatePasteLinkRequest(
    val id: Int,
    val pasteLink: String
)

@Serializable
data class UpdateTagsRequest(
    val id: Int,
    val tags: List<Int>
)

@Serializable
data class DeleteAssetRequest(
    val id: Int
)

fun Route.effectsRoutes() {
    route("/assets") {
        authenticate("auth-level-1") {

            get {
                val key = call.principal<UserIdPrincipal>()!!.name
                if (!isAllowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                    return@get
                }
                call.respond(getAllProvidedAssets())
            }

            get("/get/{id}") {
                val key = call.principal<UserIdPrincipal>()!!.name
                if (!isAllowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                    return@get
                }

                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(mapOf("error" to "Invalid ID"))
                    return@get
                }
                val asset = getAssetById(id)
                if (asset == null) {
                    call.respond(mapOf("error" to "Effect not found"))
                } else {
                    call.respond(asset)
                }
            }

            get("/raw/{id}") {
                val key = call.principal<UserIdPrincipal>()!!.name
                if (!isAllowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                    return@get
                }

                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(mapOf("error" to "Invalid ID"))
                    return@get
                }
                val raw = getRawData(id)
                if (raw == null) {
                    call.respond(mapOf("error" to "Effect not found"))
                } else {
                    call.respond(mapOf("raw_data" to raw))
                }
            }

            get("/search") {
                val key = call.principal<UserIdPrincipal>()!!.name
                if (!isAllowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                    return@get
                }

                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing name")
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50

                val results = searchAssetsByName(name, limit)
                call.respond(results)
            }

            get("/filter") {
                val key = call.principal<UserIdPrincipal>()!!.name
                if (!isAllowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                    return@get
                }

                val tagsParam = call.request.queryParameters["tags"] ?: ""
                val tagIds = tagsParam.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()
                val match = call.request.queryParameters["match"] ?: "any" // "any" or "all"
                val requireAll = match.equals("all", ignoreCase = true)

                val results = filterAssetsByTags(tagIds, limit, requireAll)
                call.respond(results)
            }

            get("/latest") {
                val key = call.principal<UserIdPrincipal>()!!.name
                if (!isAllowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                    return@get
                }

                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50

                val results = getLatestAssets(limit)
                call.respond(results)
            }

        }

        // Need higher permission to execute modification request
        authenticate("auth-level-999") {
            post("/create") {
                val request = call.receive<CreateAssetRequest>()
                val result = createAsset(
                    request.name,
                    request.description,
                    request.typeId,
                    request.author,
                    request.material,
                    request.pasteLink,
                    request.rawData,
                    request.discordId,
                    request.tagsIds
                )

                call.respond(result)
            }

            patch("/update/material") {
                val request = call.receive<UpdateMaterialRequest>()
                val statusCode = updateMaterial(request.id, request.material)
                call.respond(statusCode)
            }

            patch("/update/pastelink") {
                val request = call.receive<UpdatePasteLinkRequest>()
                val statusCode = updatePasteLink(request.id, request.pasteLink)
                call.respond(statusCode)
            }

            patch("/update/tags") {
                val request = call.receive<UpdateTagsRequest>()
                val statusCode = updateTags(request.id, request.tags)
                call.respond(statusCode)
            }

            patch("/approve") {
                val request = call.receive<ApproveAssetRequest>()
                val statusCode = approveAsset(request.id, request.approvedBy)
                call.respond(statusCode)
            }

            delete("/delete") {
                val request = call.receive<DeleteAssetRequest>()
                val statusCode = deleteAsset(request.id)
                call.respond(statusCode)
            }
        }
    }
}