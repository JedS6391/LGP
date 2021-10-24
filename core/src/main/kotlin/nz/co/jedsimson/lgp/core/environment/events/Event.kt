package nz.co.jedsimson.lgp.core.environment.events

import java.util.*

/**
 * A base primitive for building events. All events should derive from [Event].
 *
 * @property identifier A unique identifier for this event.
 * @constructor Creates a new [Event] with the given [identifier].
 */
open class Event(val identifier: UUID) {

    /**
     * Creates a new [Event] with a random identifier.
     */
    constructor() : this(UUID.randomUUID())
}