package me.m64diamondstar.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import me.m64diamondstar.db.validateApiKey

fun Application.configureAuthentication() {
    install(Authentication) {

        // Permission level 1: used for simple GET requests
        bearer("auth-level-1") {
            authenticate { tokenCredential ->
                if(validateApiKey(tokenCredential.token, minPermission = 1)) {
                    UserIdPrincipal(tokenCredential.token)
                } else null
            }
        }

        // Permission level 999: highest access possible, only used for the bot (where the key is stored securely)
        bearer("auth-level-999") {
            authenticate { tokenCredential ->
                if(validateApiKey(tokenCredential.token, minPermission = 999)) {
                    UserIdPrincipal(tokenCredential.token)
                } else null
            }
        }
    }
}