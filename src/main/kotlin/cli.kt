package com.lightningkite.kotlinercli

import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

public class WrongCliArgumentsException: Exception("")

/**
 * Exposes the given functions as subcommands in a fairly traditional CLI style.
 * The result is printed to [System.out]`.
 *
 * Supported argument types:
 * - Primitives (Char, Byte, Short, Int, Long, Float, Double, Boolean, String)
 * - Classes with a String constructor, such as [java.io.File]
 *
 * Supported argument styles:
 * - Required
 * - Optional using default arguments
 * - Variadic
 *
 * If no subcommand is provided bu the user OR `--help` is given, a list of available subcommands is printed.
 *
 * If the requirements for executing the function are not met OR `--help` is given, some help information is printed.
 *
 * @param arguments The arguments given by the user.
 * @param setup The environment setup function which takes arguments before the sub command.
 * @param available The functions to make available as sub commands.
 * @param useInteractive If enabled (which it is by default), entering only setup arguments or no arguments will launch into an interactive session where you may enter multiple commands.
 */
public fun cli(
    arguments: Array<out String>,
    setup: KFunction<*> = ::noSetup,
    available: List<KFunction<*>>,
    useInteractive: Boolean = true
): Unit = try {
    val result = cliReturning(
        arguments = arguments,
        setup = setup,
        available = available,
        useInteractive = useInteractive
    )
    if(result != Unit) println(result)
    Unit
} catch(e: WrongCliArgumentsException) {
    /*squish*/
}

/**
 * Exposes the given functions as subcommands in a fairly traditional CLI style.
 * The result is printed to [System.out]`.
 *
 * Supported argument types:
 * - Primitives (Char, Byte, Short, Int, Long, Float, Double, Boolean, String)
 * - Classes with a String constructor, such as [java.io.File]
 *
 * Supported argument styles:
 * - Required
 * - Optional using default arguments
 * - Variadic
 *
 * If no subcommand is provided bu the user OR `--help` is given, a list of available subcommands is printed.
 *
 * If the requirements for executing the function are not met OR `--help` is given, some help information is printed.
 *
 * @param arguments The arguments given by the user.
 * @param available The functions to make available as sub commands.
 */
public fun cli(
    arguments: Array<out String>,
    vararg available: KFunction<*>
): Unit = cli(arguments = arguments, setup = ::noSetup, available = available.toList())

/**
 * Exposes the given functions as subcommands in a fairly traditional CLI style.
 *
 * Supported argument types:
 * - Primitives (Char, Byte, Short, Int, Long, Float, Double, Boolean, String)
 * - Classes with a String constructor, such as [java.io.File]
 *
 * Supported argument styles:
 * - Required
 * - Optional using default arguments
 * - Variadic
 *
 * If no subcommand is provided bu the user OR `--help` is given, a list of available subcommands is printed.
 *
 * If the requirements for executing the function are not met OR `--help` is given, some help information is printed.
 *
 * @param arguments The arguments given by the user.
 * @param setup The environment setup function which takes arguments before the sub command.
 * @param available The functions to make available as sub commands.
 * @param useInteractive If enabled (which it is by default), entering only setup arguments or no arguments will launch into an interactive session where you may enter multiple commands.
 * @return Returns the result of the function instead of printing it.
 */
public fun cliReturning(
    arguments: Array<out String>,
    setup: KFunction<*> = ::noSetup,
    available: List<KFunction<*>>,
    useInteractive: Boolean = true
): Any? {
    // Handle help
    if(arguments.size == 1 && arguments[0].endsWith("help") && arguments[0].startsWith("-")) {
        cliReturningHelp(setup, available)
    }
    val envArgs = HashMap<KParameter, Any?>()
    val funcArgs = ArrayList<String>()
    var func: KFunction<*>? = null
    var index = 0
    while(index < arguments.size) {
        val entry = arguments[index]
        if(func != null) {
            funcArgs.add(entry)
        } else if(entry.startsWith("--")) {
            val option = entry.removePrefix("--")
            val value = arguments.getOrNull(++index)
            val param = setup.valueParameters.find { it.name == option } ?: setup.helpAndExit("Global parameter with name '$option' not found")
            if(param.type.jvmErasure == Boolean::class && param.isOptional && (value == null || value.startsWith("--") || (value != "true" && value != "false"))) {
                envArgs[param] = true
                continue
            }
            envArgs[param] = parse(param.type, value ?: setup.helpAndExit("No value provided for global parameter '$option'"))
        } else {
            setup.isAccessible = true
            setup.callBy(envArgs)
            func = available.find { it.name == entry }
        }
        index++
    }
    val finalFunc = func
        ?: if(useInteractive) {
            interactiveMode(available)
            return null
        } else {
            cliReturningHelp(setup, available)
        }
    return finalFunc.cliCall(funcArgs)
}

private fun cliReturningHelp(
    setup: KFunction<*> = ::noSetup,
    available: List<KFunction<*>>
): Nothing {
    cliHelp(setup, available)
    throw WrongCliArgumentsException()
}

private fun cliHelp(
    setup: KFunction<*> = ::noSetup,
    available: List<KFunction<*>>
) {
    setup.valueParameters
        .takeUnless { it.isEmpty() }
        ?.let {
            println("Global options:")
            for(param in it) {
                param.printShellStringHelp()
            }
            println()
        }
    println("Available commands:")
    for(a in available) {
        println(a.toHumanString())
    }
}

/**
 * A short description of the parameter or subcommand.
 */
public annotation class Description(val description: String)

/**
 * A longer set of information about the parameter or subcommand.
 */
public annotation class Documentation(val documentation: String)

private fun KParameter.toHumanString(): String {
    return if(this.isVararg) this.name + ": " + this.varargType().toHumanString() + "..."
    else this.name + ": " + this.type.toHumanString() + if(isOptional) " = ..." else ""
}
private fun KType.toHumanString(): String = this.jvmErasure.simpleName + if(isMarkedNullable) "?" else ""
private fun KFunction<*>.toHumanString(): String {
    val prefix = "${name}(${valueParameters.joinToString { it.toHumanString() }}): ${returnType.toHumanString()}"
    return findAnnotation<Description>()?.let { "$prefix - ${it.description}" } ?: prefix
}

/**
 * Exposes the function in a fairly traditional CLI style.
 *
 * Supported argument types:
 * - Primitives (Char, Byte, Short, Int, Long, Float, Double, Boolean, String)
 * - Classes with a String constructor, such as [java.io.File]
 *
 * Supported argument styles:
 * - Required
 * - Optional using default arguments
 * - Variadic
 *
 * If the requirements for executing the function are not met OR `--help` is given, some help information is printed.
 *
 * @param arguments The arguments given by the user.
 * @return Returns the result of the function.
 */
public fun <R> KFunction<R>.cliCall(arguments: Array<out String>): R = cliCall(arguments.toList())

/**
 * Exposes the function in a fairly traditional CLI style.
 *
 * Supported argument types:
 * - Primitives (Char, Byte, Short, Int, Long, Float, Double, Boolean, String)
 * - Classes with a String constructor, such as [java.io.File]
 *
 * Supported argument styles:
 * - Required
 * - Optional using default arguments
 * - Variadic
 *
 * If the requirements for executing the function are not met OR `--help` is given, some help information is printed.
 *
 * @param arguments The arguments given by the user.
 * @return Returns the result of the function.
 */
public fun <R> KFunction<R>.cliCall(arguments: List<String>): R {
    // Handle help
    if(arguments.size == 1 && arguments[0].endsWith("help") && arguments[0].startsWith("-")) {
        helpAndExit()
    }
    val realArgs = HashMap<KParameter, Any?>()
    run {
        var index = 0
        var usedNamedParameter = false
        fun parseVararg(param: KParameter, entry: String) {
            val existing = when(val raw = realArgs[param]) {
                is Array<*> -> raw.toList()
                is ByteArray -> raw.toList()
                is ShortArray -> raw.toList()
                is IntArray -> raw.toList()
                is LongArray -> raw.toList()
                is FloatArray -> raw.toList()
                is DoubleArray -> raw.toList()
                is BooleanArray -> raw.toList()
                is CharArray -> raw.toList()
                else -> listOf()
            }
            val newEntry = parse(param.varargType(), entry)
            @Suppress("UNCHECKED_CAST")
            realArgs[param] = when(param.type.jvmErasure) {
                Array::class -> (existing + newEntry).toTypedArray()
                ByteArray::class -> ((existing + newEntry) as List<Byte>).toByteArray()
                ShortArray::class -> ((existing + newEntry) as List<Short>).toShortArray()
                IntArray::class -> ((existing + newEntry) as List<Int>).toIntArray()
                LongArray::class -> ((existing + newEntry) as List<Long>).toLongArray()
                FloatArray::class -> ((existing + newEntry) as List<Float>).toFloatArray()
                DoubleArray::class -> ((existing + newEntry) as List<Double>).toDoubleArray()
                BooleanArray::class -> ((existing + newEntry) as List<Boolean>).toBooleanArray()
                CharArray::class -> ((existing + newEntry) as List<Char>).toCharArray()
                else -> throw IllegalArgumentException()
            }
        }
        while(index < arguments.size) {
            val entry = arguments[index]
            if(entry.startsWith("--")) {
                val option = entry.removePrefix("--")
                val param = this.valueParameters.find { it.name == option } ?: helpAndExit("Parameter with name '$option' not found")
                val value = arguments.getOrNull(++index)
                if(param.type.jvmErasure == Boolean::class && param.isOptional && (value == null || value.startsWith("--") || (value != "true" && value != "false"))) {
                    realArgs[param] = true
                    continue
                }
                if(param.isVararg) {
                    parseVararg(param, value ?: helpAndExit("No value provided for parameter '$option'"))
                } else {
                    realArgs[param] = parse(param.type, value ?: helpAndExit("No value provided for parameter '$option'"))
                }
                usedNamedParameter = true
            } else {
                if(usedNamedParameter) {
                    val v = this.valueParameters.find { it.isVararg } ?: helpAndExit("No varargs parameter found and named parameters have already been used.")
                    parseVararg(v, entry)
                } else {
                    val param = this.valueParameters.find { it.index == index } ?: this.valueParameters.find { it.isVararg } ?: helpAndExit("More arguments provided than the function can receive.")
                    val value = entry
                    if(param.isVararg) {
                        parseVararg(param, value)
                    } else {
                        realArgs[param] = parse(param.type, value)
                    }
                }
            }
            index++
        }
    }
    this.isAccessible = true
    for(param in valueParameters) {
        if(!param.isOptional && !realArgs.containsKey(param)) {
            if(param.isVararg) {
                realArgs[param] = when(param.type.jvmErasure) {
                    Array::class -> arrayOf<Any?>()
                    ByteArray::class -> byteArrayOf()
                    ShortArray::class -> shortArrayOf()
                    IntArray::class -> intArrayOf()
                    LongArray::class -> longArrayOf()
                    FloatArray::class -> floatArrayOf()
                    DoubleArray::class -> doubleArrayOf()
                    BooleanArray::class -> booleanArrayOf()
                    CharArray::class -> charArrayOf()
                    else -> throw IllegalArgumentException()
                }
            } else helpAndExit("'${param.name}' is required, but wasn't provided.")
        }
    }
    return this.callBy(realArgs)
}

private fun interactiveMode(
    available: List<KFunction<*>>
) {
    println("Entering interactive mode:")
    while(true) {
        println()
        val input = readLine() ?: return
        if(input.isBlank() || input == "help") {
            cliHelp(available = available)
            continue
        }
        if(input == "exit" || input == "quit") return
        val parts = input.cliSplit().toTypedArray()
        cli(parts, available = available, useInteractive = false)
    }
}

internal fun String.cliSplit(): List<String> {
    val results = ArrayList<String>()
    var start = 0
    var index = 0
    var inQuote = false
    fun splitHere() {
        val part = this.substring(start, index)
        if(part.isNotBlank()) results.add(part
            .replace("\\n", "\n")
            .replace("\\b", "\b")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
            .replace("\\'", "\'")
            .replace("\\\"", "\"")
        )
        start = index + 1
    }
    while(index < this.length) {
        when(this[index]) {
            '"' -> if(start == index) {
                inQuote = true
                start++
            } else {
                inQuote = false
                splitHere()
            }
            ' ' -> if(!inQuote) splitHere()
            '\\' -> index++
        }
        index++
    }
    splitHere()
    return results
}

private class SimpleType(override val classifier: KClassifier?): KType {
    override val annotations: List<Annotation>
        get() = listOf()
    override val arguments: List<KTypeProjection>
        get() = listOf()
    override val isMarkedNullable: Boolean
        get() = false
}
private fun KParameter.varargType(): KType = when(this.type.jvmErasure) {
    Array::class -> type.arguments[0].type!!
    ByteArray::class -> SimpleType(Byte::class)
    ShortArray::class -> SimpleType(Short::class)
    IntArray::class -> SimpleType(Int::class)
    LongArray::class -> SimpleType(Long::class)
    FloatArray::class -> SimpleType(Float::class)
    DoubleArray::class -> SimpleType(Double::class)
    BooleanArray::class -> SimpleType(Boolean::class)
    CharArray::class -> SimpleType(Char::class)
    else -> throw IllegalArgumentException()
}

private fun parse(type: KType, value: String): Any? {
    return when {
        type.isMarkedNullable && value == "null" -> null
        else -> when(val cls = type.jvmErasure) {
            Any::class -> value
            Byte::class -> value.toByte()
            Short::class -> value.toShort()
            Int::class -> value.toInt()
            Long::class -> value.toLong()
            Float::class -> value.toFloat()
            Double::class -> value.toDouble()
            String::class -> value
            Boolean::class -> value.toBoolean()
            Char::class -> value.single()
            else ->  {
                cls.java.enumConstants?.let {
                    return it.find { (it as Enum<*>).name.equals(value, true) }
                }
                val constructor = cls.constructors
                    .find { it.valueParameters.size == 1 && it.valueParameters[0].type.jvmErasure == String::class }
                    ?: throw IllegalArgumentException("Found no string constructors for ${cls.qualifiedName}")
                constructor.call(value)
            }
        }
    }
}

private fun KFunction<*>.helpAndExit(errorMessage: String? = null): Nothing {
    errorMessage?.let { println(errorMessage); println() }
    println(name)
    this.findAnnotation<Description>()?.let { println(it.description) }
    this.findAnnotation<Documentation>()?.let { println(it.documentation) }
    for(param in valueParameters) {
        param.printShellStringHelp()
    }
    throw WrongCliArgumentsException()
}

private fun KParameter.toShellString() = if (isVararg){
    "--${name} <${varargType().toHumanString()}>..."
} else if(isOptional) {
    "--${name} <${type.toHumanString()}> (optional)"
} else {
    "--${name} <${type.toHumanString()}>"
}
private fun KParameter.printShellStringHelp() {
    println(toShellString())
    findAnnotation<Description>()?.let { println("    ${it.description}") }
    findAnnotation<Documentation>()?.let { println("    ${it.documentation}") }
}
private val Any?.typeString: String get() = if(this == null) "null" else this::class.simpleName ?: "?"

private fun noSetup(): Unit {}