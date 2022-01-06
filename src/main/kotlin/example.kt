
import com.lightningkite.kotlinercli.cli
import java.io.File

fun main(args: Array<String>) = cli(args, ::runServer, ::migrate, ::dump, ::deleteItems)

fun runServer(host: String = "0.0.0.0", port: Int = 8080) = println("Running the server at $host on port $port")
fun migrate(version: Int) = println("Upgrading the database")
fun dump(to: File) = println("Dumping to a file")
fun deleteItems(vararg ids: Int) = println("Deleting ${ids.joinToString()}")