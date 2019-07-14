package nz.co.jedsimson.lgp.core.environment.events

/**
 * Defines a listener for a particular event type.
 *
 * @param TEvent The type of event this listener can handle.
 */
interface EventListener<in TEvent : Event> {

    /**
     * Handles [event] with behaviour defined by the listener.
     *
     * @param event An event to handle.
     */
    fun handle(event: TEvent)
}