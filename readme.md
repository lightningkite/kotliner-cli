# Kotlin-er CLI

A different take on making command line programs, emphasizing development speed over customization

## Example

```kotlin
import com.lightningkite.kotlinercli.cli
import java.io.File

fun main(args: Array<String>) = cli(args, ::runServer, ::migrate, ::dump, ::deleteItems)

fun runServer(host: String = "0.0.0.0", port: Int = 8080) = println("Running the server at $host on port $port")
fun migrate(version: Int) = println("Upgrading the database")
fun dump(to: File) = println("Dumping to a file")
fun deleteItems(vararg ids: Int) = println("Deleting ${ids.joinToString()}")
```

```
$ myProgram
Available commands:
runServer(host: String = ..., port: Int = ...): Unit
migrate(version: Int): Unit
dump(to: File): Unit
deleteItems(ids: IntArray): Unit

$ myProgram runServer --help
runServer
--host: String (optional)
--port: Int (optional)

$ myProgram runServer
Running the server at 0.0.0.0 with port 8000

$ myProgram runServer 127.0.0.0 8080
Running the server at 127.0.0.0 with port 8080

$ myProgram runServer --port 8080
Running the server at 0.0.0.0 with port 8080

$ myProgram deleteItems 1 2 3
Deleting 1, 2, 3
```