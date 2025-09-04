package me.m64diamondstar

import io.ktor.server.application.*
import me.m64diamondstar.plugins.configureAuthentication
import me.m64diamondstar.plugins.configureDatabases
import me.m64diamondstar.plugins.configureRouting

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDatabases()
    configureAuthentication()
    configureRouting()
}
