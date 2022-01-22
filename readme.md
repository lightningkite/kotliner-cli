# Kotlin-er CLI

A different take on making command line programs, emphasizing development speed over customization and performance.

Essentially exposes Kotlin functions as command-line options.

Pros:

- Extremely fast to set up
- Uses Kotlin reflection to reduce documentation and argument definition overhead
- Ensures at least a minimal documentation and error checking will exist in your CLI

Cons:

- Slow runtime performance due to reflection
- Not compatible with Kotlin Multiplatform due to reflection

## Status

Auto-deployed to Maven Central after passing unit tests using GitHub Actions

[![release status](https://github.com/lightningkite/kotliner-cli/actions/workflows/release.yaml/badge.svg)](https://s01.oss.sonatype.org/content/repositories/releases/com/lightningkite/kotliner-cli/)
[![snapshot status](https://github.com/lightningkite/kotliner-cli/actions/workflows/snapshot.yaml/badge.svg)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/lightningkite/kotliner-cli/)

[Release on Sonatype Releases](https://s01.oss.sonatype.org/content/repositories/releases/com/lightningkite/kotliner-cli/)
[Release on Sonatype Snapshots](https://s01.oss.sonatype.org/content/repositories/snapshots/com/lightningkite/kotliner-cli/)

## Download

```kotlin
dependencies {
  //...
  implementation("com.lightningkite:kotliner-cli:$kotlinerVersion")
}
```

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
deleteItems(ids: Int...): Unit

$ myProgram runServer --help
runServer
--host <String> (optional)
--port <Int> (optional)
$ myProgram runServer
Running the server at 0.0.0.0 on port 8080

$ myProgram runServer 127.0.0.0 8080
Running the server at 127.0.0.0 on port 8080

$ myProgram runServer --port 8080
Running the server at 0.0.0.0 on port 8080

$ myProgram deleteItems 1 2 3
Deleting 1, 2, 3
```

## Supported Features

- [X] Primitive Types
- [X] Types with a constructor that takes a `String`
- [X] Ordered Arguments
- [X] Named Arguments
- [X] Default Arguments
- [X] Variadic Arguments
- [X] Enum Arguments (case-insensitive)
- [X] Boolean Flags (i.e. using `--flag` for `flag=true`)
- [X] Description Annotation - add the annotation to provide more documentation about functions and parameters.
- [X] Interactive mode when no arguments are given (like `supervisor`)

## Potential Features

- [ ] Annotation to automatically register functions to expose to the CLI
  - Questionable security-wise - makes it easy to insert functionality with no obvious connection.
- [ ] Compiler plugin to read KDoc for runtime documentation

## Deliberately unsupported features

- Object types without a string constructor - No good way to represent in the command line.
- Expose all functions in the command line - would be a potential security problem 