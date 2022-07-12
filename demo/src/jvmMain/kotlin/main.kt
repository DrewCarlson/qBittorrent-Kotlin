package demo

import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main(vararg args: String): Unit = runBlocking {
    runProgram(args)
    exitProcess(-1)
}
