package com.lightningkite.kotlinercli

import com.lightningkite.kotlinercli.cli
import org.junit.Test
import java.io.File

class ReadmeTest {
    @Test
    fun documentations() {
        emitForReadme("")
        emitForReadme("--help")
        emitForReadme("runServer --help")
        emitForReadme("runServer")
        emitForReadme("runServer 127.0.0.0 8080")
        emitForReadme("runServer --port 8080")
        emitForReadme("deleteItems 1 2 3")
    }

    fun emitForReadme(input: String) {
        val args = input.split(' ').toTypedArray()
        try {
            println("$ myProgram $input")
            cliReturning(args, available = functions)
            println()
        } catch (e: WrongCliArgumentsException) { /*squish*/
        }
    }
}

val functions = listOf(::runServer, ::migrate, ::dump, ::deleteItems)

fun runServer(host: String = "0.0.0.0", port: Int = 8080) = println("Running the server at $host on port $port")
fun migrate(version: Int) = println("Upgrading the database")
fun dump(to: File) = println("Dumping to a file")
fun deleteItems(vararg ids: Int) = println("Deleting ${ids.joinToString()}")