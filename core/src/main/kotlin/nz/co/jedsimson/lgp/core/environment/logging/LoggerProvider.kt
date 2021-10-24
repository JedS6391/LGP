package nz.co.jedsimson.lgp.core.environment.logging

/**
 * Provides access to a [Logger] instance.
 */
class LoggerProvider {
    companion object {
        /**
         * Gets a logger for [name].
         */
        fun getLogger(name: String) = LoggerProvider().getLogger(name)
    }

    /**
     * Gets a logger for [name].
     */
    fun getLogger(name: String): Logger = KotlinLoggingLogger(name)
}