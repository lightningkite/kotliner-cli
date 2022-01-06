package com.lightningkite.kotlinercli

import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName
import kotlin.system.exitProcess

private fun noSetup(): Unit {}

public fun cli(
    arguments: Array<out String>,
    setup: KFunction<*> = ::noSetup,
    available: List<KFunction<*>>
): Unit {
    val result = cliReturning(arguments, setup, available)
    if(result != Unit) println(result)
}
public fun cli(
    arguments: Array<out String>,
    vararg available: KFunction<*>
): Unit = cli(arguments = arguments, setup = ::noSetup, available = available.toList())

public inline fun <R> select(noinline function: ()->R): KFunction<R> = function as KFunction<R>
public inline fun <A, R> select(noinline function: (A)->R): KFunction<R> = function as KFunction<R>
public inline fun <A, B, R> select(noinline function: (A, B)->R): KFunction<R> = function as KFunction<R>
public inline fun <A, B, C, R> select(noinline function: (A, B, C)->R): KFunction<R> = function as KFunction<R>
public inline fun <A, B, C, D, R> select(noinline function: (A, B, C, D)->R): KFunction<R> = function as KFunction<R>
public inline fun <A, B, C, D, E, R> select(noinline function: (A, B, C, D, E)->R): KFunction<R> = function as KFunction<R>
public inline fun <A, B, C, D, E, F, R> select(noinline function: (A, B, C, D, E, F)->R): KFunction<R> = function as KFunction<R>
public inline fun <A, B, C, D, E, F, G, R> select(noinline function: (A, B, C, D, E, F, G)->R): KFunction<R> = function as KFunction<R>

public fun cliReturning(
    arguments: Array<out String>,
    setup: KFunction<*> = ::noSetup,
    available: List<KFunction<*>>
): Any? {
    // Handle help
    if(arguments.isEmpty() || arguments.size == 1 && arguments[0].endsWith("help") && arguments[0].startsWith("-")) {
        println("Available commands:")
        for(a in available) {
            println(a.toHumanString())
        }
        exitProcess(0)
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
            val value = arguments.getOrNull(++index) ?: setup.helpAndExit()
            val param = setup.valueParameters.find { it.name == option } ?: setup.helpAndExit()
            envArgs[param] = parse(param.type, value)
        } else {
            func = available.find { it.name == entry }
        }
        index++
    }
    val finalFunc = func
    if(finalFunc == null) {
        println("Available commands:")
        for(a in available) {
            println(a.toHumanString())
        }
        exitProcess(0)
    }
    return finalFunc.cliCall(funcArgs)
}

public annotation class Description(val description: String)

private fun KParameter.toHumanString(): String {
    return if(this.isVararg) this.name + ": " + this.varargType().toHumanString() + "..."
    else this.name + ": " + this.type.toHumanString() + if(isOptional) " = ..." else ""
}
private fun KType.toHumanString(): String = this.jvmErasure.simpleName + if(isMarkedNullable) "?" else ""
private fun KFunction<*>.toHumanString(): String {
    val prefix = "${name}(${valueParameters.joinToString { it.toHumanString() }}): ${returnType.toHumanString()}"
    return findAnnotation<Description>()?.let { "$prefix - ${it.description}" } ?: prefix
}

fun <R> KFunction<R>.cliCall(arguments: Array<out String>): R = cliCall(arguments.toList())
fun <R> KFunction<R>.cliCall(arguments: List<String>): R {
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
                val key = entry.removePrefix("--")
                val param = this.valueParameters.find { it.name == key } ?: helpAndExit()
                val value = arguments.getOrNull(++index)
                if(param.type.jvmErasure == Boolean::class && param.isOptional && (value == null || value.startsWith("--") || (value != "true" && value != "false"))) {
                    realArgs[param] = true
                    continue
                }
                realArgs[param] = parse(param.type, value ?: helpAndExit())
                usedNamedParameter = true
            } else {
                if(usedNamedParameter) {
                    val v = this.valueParameters.find { it.isVararg } ?: helpAndExit()
                    parseVararg(v, entry)
                } else {
                    val param = this.valueParameters.find { it.index == index } ?: this.valueParameters.find { it.isVararg } ?: helpAndExit()
                    if(param.isVararg) {
                        parseVararg(param, entry)
                    } else {
                        realArgs[param] = parse(param.type, entry)
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
            } else helpAndExit()
        }
    }
    return this.callBy(realArgs)
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
                val constructor = cls.constructors
                    .find { it.valueParameters.size == 1 && it.valueParameters[0].type.jvmErasure == String::class }
                    ?: throw IllegalArgumentException("Found no string constructors for ${cls.qualifiedName}")
                constructor.call(value)
            }
        }
    }
}

private fun KFunction<*>.helpAndExit(): Nothing {
    println(name)
    this.findAnnotation<Description>()?.let { println(it.description) }
    for(param in valueParameters) {
        if (param.isVararg){
            println("--${param.name} <${param.varargType().toHumanString()}>...")
        } else if(param.isOptional) {
            println("--${param.name} <${param.type.toHumanString()}> (optional)")
        } else {
            println("--${param.name} <${param.type.toHumanString()}>")
        }
        param.findAnnotation<Description>()?.let { println("    ${it.description}") }
    }
    exitProcess(0)
}

private val Any?.typeString: String get() = if(this == null) "null" else this::class.simpleName ?: "?"