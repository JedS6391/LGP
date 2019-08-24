package nz.co.jedsimson.lgp.core.environment.events

import java.util.ArrayList
import kotlin.reflect.KClass

/**
 * Responsible for dispatching events to a set of registered listeners.
 */
object EventDispatcher {

    private val listeners: MutableMap<KClass<out Event>, MutableList<EventListener<Event>>> = mutableMapOf()

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

    /**
     * Dispatches [event] to all currently registered listeners.
     *
     * @param event An event to dispatch.
     */
    fun dispatch(event: Event) {
        listeners[event::class]?.forEach { listener ->
            listener.handle(event)
        }
    }
}