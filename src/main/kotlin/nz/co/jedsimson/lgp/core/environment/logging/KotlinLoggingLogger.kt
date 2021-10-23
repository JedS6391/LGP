package nz.co.jedsimson.lgp.core.environment.logging

import mu.KLogger
import mu.KotlinLogging

/**
 * A [Logger] implementation using [KotlinLogging].
 */
internal class KotlinLoggingLogger(val name: String) : Logger {
    private val kotlinLogger: KLogger = KotlinLogging.logger(name)

    override fun trace(message: () -> Any?) = kotlinLogger.trace(message)

    override fun debug(message: () -> Any?) = kotlinLogger.debug(message)

    override fun info(message: () -> Any?) = kotlinLogger.info(message)

    override fun warn(message: () -> Any?) = kotlinLogger.warn(message)

    override fun error(message: () -> Any?) = kotlinLogger.error(message)

    override fun error(throwable: Throwable, message: () -> Any?) = kotlinLogger.error(throwable, message)
}