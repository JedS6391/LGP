package nz.co.jedsimson.lgp.core.environment.logging

interface Logger {
    fun trace(msg: () -> Any?)
    fun debug(msg: () -> Any?)
    fun info(msg: () -> Any?)
    fun warn(msg: () -> Any?)
    fun error(msg: () -> Any?)
    fun error(throwable: Throwable, msg: () -> Any?)
}