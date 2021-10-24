package nz.co.jedsimson.lgp.core.environment.logging

/**
 * Defines mechanisms for logging.
 */
interface Logger {
    /**
     * Writes [message] at the `Trace` level, if enabled.
     */
    fun trace(message: () -> Any?)

    /**
     * Writes [message] at the `Debug` level, if enabled.
     */
    fun debug(message: () -> Any?)

    /**
     * Writes [message] at the `Info` level, if enabled.
     */
    fun info(message: () -> Any?)

    /**
     * Writes [message] at the `Warn` level, if enabled.
     */
    fun warn(message: () -> Any?)

    /**
     * Writes [message] at the `Error` level, if enabled.
     */
    fun error(message: () -> Any?)

    /**
     * Writes [message] at the `Error` level with details of [throwable], if enabled.
     */
    fun error(throwable: Throwable, message: () -> Any?)
}