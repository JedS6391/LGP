package lgp.core.evolution.instructions

/**
 * Thrown when the [Arity] of an [Operation] does not match the number of
 * [Argument]s given to it.
 *
 * @param message A message to go along with the exception.
 */
class ArityException(message: String) : Exception(message)