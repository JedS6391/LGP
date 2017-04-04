package lgp.core.environment.constants

import lgp.core.environment.ComponentLoader

/**
 * Loads a collection of values of type T as constants.
 *
 * @param T The type of the constants.
 */
interface ConstantLoader<T> : ComponentLoader<List<T>>