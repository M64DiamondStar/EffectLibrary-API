package me.m64diamondstar.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.m64diamondstar.db.approveEffect
import me.m64diamondstar.db.createAsset
import me.m64diamondstar.db.services.filterAssetsByTags
import me.m64diamondstar.db.services.getAllProvidedAssets
import me.m64diamondstar.db.services.getAssetById
import me.m64diamondstar.db.services.getLatestAssets
import me.m64diamondstar.db.services.getRawData
import me.m64diamondstar.db.services.searchAssetsByName

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

fun Route.effectsRoutes() {
    route("/assets") {
        authenticate("auth-level-1") {
            get {
                call.respond(getAllProvidedAssets())
            }

            get("/get/{id}") {
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
                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing name")
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50

                val results = searchAssetsByName(name, limit)
                call.respond(results)
            }

            get("/filter") {
                val tagsParam = call.request.queryParameters["tags"] ?: ""
                val tagIds = tagsParam.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()
                val match = call.request.queryParameters["match"] ?: "any" // "any" or "all"
                val requireAll = match.equals("all", ignoreCase = true)

                val results = filterAssetsByTags(tagIds, limit, requireAll)
                call.respond(results)
            }

            get("/latest") {
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

            patch("/approve") {
                val request = call.receive<ApproveAssetRequest>()
                val statusCode = approveEffect(request.id, request.approvedBy)
                call.respond(statusCode)
            }
        }
    }
}