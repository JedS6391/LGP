package nz.co.jedsimson.lgp.core.environment.events

import java.util.ArrayList
import kotlin.reflect.KClass

/**
 * Responsible for managing event listeners that will be communicated to via the [EventDispatcher].
 */
object EventRegistry {

    /**
     * The set of registered listeners.
     */
    internal val listeners: MutableMap<KClass<out Event>, MutableList<EventListener<Event>>> = mutableMapOf()

    /**
     * Gets the number of registered listeners.
     */
    val numberOfListeners: Int
        get() {
            return listeners.values.sumBy { listenersPerEvent ->
                listenersPerEvent.count()
            }
        }

    /**
     * Registers [listener] for events of type [TEvent].
     *
     * @param listener A listener to register.
     */
    inline fun <reified TEvent : Event> register(listener: EventListener<TEvent>) {
        register(TEvent::class, listener)
    }

    /**
     * Registers [listener] for events that have the given [eventClass].
     *
     * @param eventClass A class of type [TEvent] to register a listener against.
     * @param listener A listener to register.
     */
    fun <TEvent : Event> register(eventClass: KClass<out TEvent>, listener: EventListener<TEvent>) {
        val eventListeners = listeners.getOrPut(eventClass) { ArrayList() }

        eventListeners.add(listener as EventListener<Event>)
    }
}