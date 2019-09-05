package nz.co.jedsimson.lgp.core.environment.events

/**
 * Responsible for dispatching events to a set of registered listeners.
 */
object EventDispatcher {

    /**
     * Dispatches [event] to all currently registered listeners.
     *
     * @param event An event to dispatch.
     */
    fun dispatch(event: Event) {
        EventRegistry.listeners[event::class]?.forEach { listener ->
            listener.handle(event)
        }
    }
}