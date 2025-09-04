package me.m64diamondstar

import me.m64diamondstar.security.ApiKeyUtil
import org.junit.Test

class ApplicationTest {

    @Test
    fun generateKey(){
        val key = ApiKeyUtil.generateApiKey()
        val hash = ApiKeyUtil.hashKey(key)

        println("API-Key: $key")
        println("Hash: $hash")
    }

}
