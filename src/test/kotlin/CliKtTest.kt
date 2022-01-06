package com.lightningkite.kotlinercli

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CliKtTest {
    @Test fun testNoParams() {
        success = false
        cliReturning(arrayOf("commandA"), available = listOf(::commandA))
        assertTrue(success)
    }
    @Test fun testBasicParam() {
        success = false
        cliReturning(arrayOf("commandB", "10", "Hello world!"), available = listOf(::commandB))
        assertTrue(success)

        success = false
        cliReturning(arrayOf("commandB", "10", "--text", "Hello world!"), available = listOf(::commandB))
        assertTrue(success)

        success = false
        cliReturning(arrayOf("commandB", "--number", "10", "--text", "Hello world!"), available = listOf(::commandB))
        assertTrue(success)
    }
    @Test fun testAdvancedParam() {
        success = false
        cliReturning(arrayOf("commandC", "test.txt"), available = listOf(::commandC))
        assertTrue(success)
        success = false
        cliReturning(arrayOf("commandC", "test.txt", "33"), available = listOf(::commandC))
        assertTrue(success)
        success = false
        cliReturning(arrayOf("commandC", "test.txt", "--default", "33"), available = listOf(::commandC))
        assertTrue(success)
        success = false
        cliReturning(arrayOf("commandC", "--file", "test.txt", "--default", "33"), available = listOf(::commandC))
        assertTrue(success)
    }
    @Test fun testVararg() {
        success = false
        cliReturning(arrayOf("commandD", "test.txt"), available = listOf(::commandD))
        assertTrue(success)
        success = false
        cliReturning(arrayOf("commandD", "test.txt", "33"), available = listOf(::commandD))
        assertTrue(success)
        success = false
        cliReturning(arrayOf("commandD", "test.txt", "33", "22", "89"), available = listOf(::commandD))
        assertTrue(success)
        success = false
        cliReturning(arrayOf("commandD", "--file", "test.txt", "22", "33"), available = listOf(::commandD))
        assertTrue(success)
    }
    @Test fun testFlag() {
        assertEquals(false, cliReturning(arrayOf("commandE", "test.txt"), available = listOf(::commandE)))
        assertEquals(true, cliReturning(arrayOf("commandE", "test.txt", "--flag"), available = listOf(::commandE)))
        assertEquals(true, cliReturning(arrayOf("commandE", "test.txt", "--flag", "true"), available = listOf(::commandE)))
        assertEquals(false, cliReturning(arrayOf("commandE", "--file", "test.txt"), available = listOf(::commandE)))
        assertEquals(true, cliReturning(arrayOf("commandE", "--file", "test.txt", "--flag", "true"), available = listOf(::commandE)))
        assertEquals(true, cliReturning(arrayOf("commandE", "--file", "test.txt", "--flag"), available = listOf(::commandE)))
        assertEquals(true, cliReturning(arrayOf("commandE", "--flag", "--file", "test.txt"), available = listOf(::commandE)))
        assertEquals(true, cliReturning(arrayOf("commandE", "--flag", "true", "--file", "test.txt"), available = listOf(::commandE)))
    }
    @Test fun obj() {
        success = false
        cliReturning(arrayOf("test"), available = listOf(TestObject::test))
        assertTrue(success)
    }
    @Test fun testSetup() {
        success = false
        cliReturning(arrayOf("doNothing"), setup = ::commandA, available = listOf(::doNothing))
        assertTrue(success)
    }
}

private var success = false
private fun commandA() {
    success = true
}

private fun doNothing() {
}

private fun commandB(number: Int, text: String) {
    success = true
}

private fun commandC(file: File, default: Int = 2) {
    success = true
}

private fun commandD(file: File, vararg multiple: Int) {
    success = true
}

private fun commandE(file: File, flag: Boolean = false): Boolean {
    return flag
}

object TestObject {
    fun test() {
        success = true
    }
}