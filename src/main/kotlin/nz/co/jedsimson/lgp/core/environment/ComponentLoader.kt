package nz.co.jedsimson.lgp.core.environment

import nz.co.jedsimson.lgp.core.modules.Module

/**
 * A builder that can build a [ComponentLoader].
 *
 * Primarily used when creating an implementation of [ComponentLoader] so that the loader
 * provides a simple way to be built, particularly when used through Java. In Kotlin, it is
 * preferred to use named arguments to the constructor of the loader.
 *
 * @param TComponentLoader A type that this builder is responsible for building.
 */
interface ComponentLoaderBuilder<out TComponentLoader> {
    fun build(): TComponentLoader
}

/**
 * A [Module] that is able to load components.
 *
 * The implementer controls how a component the loader is responsible for is loaded
 * and consumers should be indifferent to the implementation details.
 *
 * @param TComponent The type of component that this loader is able to load.
 */
interface ComponentLoader<out TComponent> : Module {
    /**
     * Loads a component.
     *
     * @returns A component of type [TComponent].
     */
    fun load(): TComponent
}