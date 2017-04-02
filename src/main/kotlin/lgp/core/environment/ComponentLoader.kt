package lgp.core.environment

import lgp.core.modules.Module

/**
 * Defines an implementation that can build a [ComponentLoader].
 *
 * Primarily used when making an implementation of [ComponentLoader] so that the
 * [ComponentLoader] provides a simple way to be built.
 */
interface ComponentLoaderBuilder<out TComponentLoader> {
    fun build(): TComponentLoader
}

/**
 * Defines a [Module] that is able to load components that are required by the environment.
 *
 * @param TComponent the type of component that this loader is able to load.
 */
interface ComponentLoader<out TComponent> : Module {
    /**
     * Loads a component.
     *
     * @returns a component of type [TComponent].
     */
    fun load(): TComponent
}