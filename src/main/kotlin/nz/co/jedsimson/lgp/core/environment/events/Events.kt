package nz.co.jedsimson.lgp.core.environment.events

/**
 * Defines an event for diagnostic information.
 *
 * @property context Contextual information from when/where this event was raised.
 */
sealed class DiagnosticEvent(val context: Map<String, String>) : Event() {

    /**
     * Defines a diagnostic event that provides debug information.
     *
     * @property message A message detailing the event.
     * @param context Contextual information from when/where this event was raised.
     */
    class Debug(val message: String, context: Map<String, String>) : DiagnosticEvent(context)
}