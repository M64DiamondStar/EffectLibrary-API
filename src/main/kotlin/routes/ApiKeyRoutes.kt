package me.m64diamondstar.routes

import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.m64diamondstar.db.createApiKey
import me.m64diamondstar.db.getApiKeyById
import me.m64diamondstar.security.ApiKeyUtil

@Serializable
data class CreateApiKeyRequest(
    val description: String,
    val discordId: String? = null,
    val rateLimit: Int = 60,
    val active: Boolean = true
)

fun Route.apiKeyRoutes() {
    route("/api-key") {

        // You need permission level 999 to generate permission level 1 keys!
        authenticate("auth-level-999") {

            // Retrieve a specific api key
            get("/get/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(mapOf("error" to "Invalid ID"))
                    return@get
                }
                val effect = getApiKeyById(id)
                if (effect == null) {
                    call.respond(mapOf("error" to "API Key not found"))
                } else {
                    call.respond(effect)
                }
            }

            // Create a new level-1 API key
            post("/generate") {
                val request = call.receive<CreateApiKeyRequest>()

                // Generate raw key
                val rawKey = ApiKeyUtil.generateApiKey()

                // Hash key before saving
                val hashedKey = ApiKeyUtil.hashKey(rawKey)

                // Store in DB
                createApiKey(
                    keyHash = hashedKey,
                    description = request.description,
                    permissionLevel = 1,
                    discordId = request.discordId,
                    rateLimit = request.rateLimit,
                    active = request.active
                )

                call.respond(mapOf("apiKey" to rawKey))
            }
        }
    }
}