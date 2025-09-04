package me.m64diamondstar.plugins

import org.jetbrains.exposed.sql.Database

fun configureDatabases() {
    Database.connect(
        url = System.getenv("DB_URL"),
        user = System.getenv("DB_USER"),
        password = System.getenv("DB_PASSWORD")
    )
}
