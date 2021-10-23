package nz.co.jedsimson.lgp.core.environment.logging

import mu.KLogger
import mu.KotlinLogging


class LoggerProvider {
    companion object {
        fun getLogger(name: String) = LoggerProvider().getLogger(name)
    }

    fun getLogger(name: String): Logger
    {
        val kotlinLogger = KotlinLogging.logger(name)

        return KotlinLoggingAdapter(kotlinLogger)
    }

    class KotlinLoggingAdapter(private val kotlinLogger: KLogger) : Logger {
        override fun trace(msg: () -> Any?) = kotlinLogger.trace(msg)

        override fun debug(msg: () -> Any?) = kotlinLogger.debug(msg)

        override fun info(msg: () -> Any?) = kotlinLogger.info(msg)

        override fun warn(msg: () -> Any?) = kotlinLogger.warn(msg)

        override fun error(msg: () -> Any?) = kotlinLogger.error(msg)

        override fun error(throwable: Throwable, msg: () -> Any?) = kotlinLogger.error(throwable, msg)
    }
}