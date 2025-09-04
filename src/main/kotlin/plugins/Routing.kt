package me.m64diamondstar.plugins

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.m64diamondstar.routes.apiKeyRoutes
import me.m64diamondstar.routes.effectsRoutes
import me.m64diamondstar.routes.tagsRoutes
import me.m64diamondstar.routes.typesRoutes

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }

    routing {
        apiKeyRoutes()
        effectsRoutes()
        tagsRoutes()
        typesRoutes()
    }
}
