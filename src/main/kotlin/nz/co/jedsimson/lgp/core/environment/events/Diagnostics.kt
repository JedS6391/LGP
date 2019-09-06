package nz.co.jedsimson.lgp.core.environment.events

import java.lang.StringBuilder
import java.time.LocalDateTime
import kotlin.system.measureNanoTime

/**
 * Defines the context for a diagnostic event.
 *
 * A context can have any kind of information in it that is relevant to the event.
 */
typealias DiagnosticEventContext = Map<String, Any>

/**
 * Defines an event for diagnostic information.
 *
 * @property context Contextual information from when/where this event was raised.
 */
sealed class DiagnosticEvent(val context: DiagnosticEventContext) : Event() {

    /**
     * Defines a diagnostic event that provides debug information.
     *
     * @property message A message detailing the event.
     * @param context Optional contextual information from when/where this event was raised.
     */
    class Debug(val message: String, context: DiagnosticEventContext = mapOf()) : DiagnosticEvent(defaultContext() + context) {
        override fun toString(): String {
            return "Debug($message, $context)"
        }
    }

    /**
     * Defines a diagnostic event that provides trace information.
     *
     * @property message A message detailing the event.
     * @param context Optional contextual information from when/where this event was raised.
     */
    class Trace(val message: String, context: DiagnosticEventContext = mapOf()): DiagnosticEvent(defaultContext() + context) {
        override fun toString(): String {
            return "Trace($message, $context)"
        }
    }
}

/**
 * Provides a set of diagnostic event tracking functions.
 */
internal object Diagnostics {

    /**
     * Creates a [DiagnosticEvent.Debug] event with the provided [message] and [context].
     *
     * *Note:* The provided [context] will be merged with a default [DiagnosticEventContext].
     *
     * @param message A message detailing the event.
     * @param context Optional contextual information from when/where this event was raised.
     */
    @JvmStatic
    fun debug(message: String, context: DiagnosticEventContext = mapOf()) {
        EventDispatcher.dispatch(DiagnosticEvent.Debug(message, context))
    }

    /**
     * Creates a [DiagnosticEvent.Trace] event with the provided [message] and default [DiagnosticEventContext].
     *
     * @param message A message detailing the event.
     */
    fun trace(message: String) {
        EventDispatcher.dispatch(DiagnosticEvent.Trace(message))
    }

    /**
     * Creates a [DiagnosticEvent.Trace] event with the provided [message] and default [DiagnosticEventContext],
     * followed by executing the supplied [action].
     *
     * @param message A message detailing the event.
     * @param action An action to execute after dispatching the trace event.
     * @param T The type returned by [action].
     * @return The value produced by executing [action].
     */
    @JvmStatic
    fun <T> trace(message: String, action: () -> T): T {
        EventDispatcher.dispatch(DiagnosticEvent.Trace(message))

        return action()
    }

    /**
     * Creates a [DiagnosticEvent.Trace] event with the provided [message] after executing and timing the supplied action.
     *
     * @param message A message detailing the event.
     * @param action An action to execute after dispatching the trace event.
     * @param T The type returned by [action].
     * @return The value produced by executing [action].
     */
    fun <T> traceWithTime(message: String, action: () -> T): T {
        val (elapsedTime, result) = measureNanoTimeWithResult { action() }

        EventDispatcher.dispatch(DiagnosticEvent.Trace(
            message, mapOf("elapsed-nanoseconds" to elapsedTime)
        ))

        return result
    }
}

private fun defaultContext(): DiagnosticEventContext {
    return mapOf(
        "timestamp" to LocalDateTime.now(),
        "thread-id" to Thread.currentThread().id,
        "thread-name" to Thread.currentThread().name,
        "stack-trace" to Thread.currentThread().stackTrace.asString()
    )
}

private fun Array<StackTraceElement>.asString(): String {
    val sb = StringBuilder()

    // This is a bit naff - can't think of a better way to achieve this for now.
    this.map { e -> e.toString() }
        .filter { s ->
            !s.startsWith("nz.co.jedsimson.lgp.core.environment.events") &&
            !s.startsWith("java.lang.Thread.getStackTrace")
        }
        .forEach { s -> sb.appendln(s) }

    return sb.toString()
}

private fun <T> measureNanoTimeWithResult(action: () -> T): Pair<Long, T> {
    val start = System.nanoTime()
    val result = action()

    return Pair(System.nanoTime() - start, result)
}