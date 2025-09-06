package me.m64diamondstar.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.m64diamondstar.db.getAllTypes
import me.m64diamondstar.db.getTypeById
import me.m64diamondstar.security.isAllowed

fun Route.typesRoutes() {
    route("/types") {
        authenticate("auth-level-1") {
            get {
                val key = call.principal<UserIdPrincipal>()!!.name
                if (!isAllowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                    return@get
                }

                call.respond(getAllTypes())
            }

            // Gets the name of a type
            get("/{id}") {
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
                val effect = getTypeById(id)
                if (effect == null) {
                    call.respond(mapOf("error" to "Effect not found"))
                } else {
                    call.respond(effect)
                }
            }
        }
    }
}