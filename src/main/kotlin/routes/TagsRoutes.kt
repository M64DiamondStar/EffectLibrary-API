package me.m64diamondstar.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.m64diamondstar.db.createTag
import me.m64diamondstar.db.deleteTag
import me.m64diamondstar.db.getAllTags
import me.m64diamondstar.db.getTagById
import me.m64diamondstar.db.renameTag
import me.m64diamondstar.security.isAllowed

@Serializable
data class CreateTagRequest(
    val name: String
)

@Serializable
data class DeleteTagRequest(
    val id: Int
)

@Serializable
data class RenameTagRequest(
    val id: Int,
    val name: String
)

fun Route.tagsRoutes() {
    route("/tags") {
        authenticate("auth-level-1") {
            get {
                val key = call.principal<UserIdPrincipal>()!!.name
                if (!isAllowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                    return@get
                }

                call.respond(getAllTags())
            }

            // Gets the name of a tag
            get("/{id}") {
                val key = call.principal<UserIdPrincipal>()!!.name
                if (!isAllowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                    return@get
                }

                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                    return@get
                }
                val effect = getTagById(id)
                if (effect == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Effect not found"))
                } else {
                    call.respond(effect)
                }
            }
        }

        authenticate("auth-level-999") {
            post {
                val request = call.receive<CreateTagRequest>()
                if (request.name.length > 50) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Tag name cannot exceed 50 characters"))
                    return@post
                }
                call.respond(createTag(request.name))
            }

            delete {
                val request = call.receive<DeleteTagRequest>()
                call.respond(deleteTag(request.id))
            }

            patch {
                val request = call.receive<RenameTagRequest>()
                if (request.name.length > 50) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Tag name cannot exceed 50 characters"))
                    return@patch
                }
                call.respond(renameTag(request.id, request.name))
            }
        }
    }
}