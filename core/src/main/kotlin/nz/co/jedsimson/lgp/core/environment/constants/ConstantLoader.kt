package nz.co.jedsimson.lgp.core.environment.constants

import nz.co.jedsimson.lgp.core.environment.ComponentLoader

/**
 * An extended [ComponentLoader] that loads a collection of values of type T to be used as constants.
 *
 * The implementer of this interface has control over all aspects of the loading process, all
 * that is required is that when the inherited [ComponentLoader.load] method is called a collection
 * of values of the correct type is given.
 *
 * Constants could be loaded by parsing some values defined in a configuration file, loaded from some
 * database, or even hardcoded.
 *
 * @param T The type of the constants.
 */
interface ConstantLoader<out T> : ComponentLoader<List<T>>