package nz.co.jedsimson.lgp.core.environment

import nz.co.jedsimson.lgp.core.modules.Module
import java.lang.Exception

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

/**
 * Defines a provider of [TComponent] instances.
 */
interface ComponentProvider<out TComponent> {

    /**
     * The component that this provider can provide.
     */
    val component: TComponent
}

/**
 * An implementation of [ComponentProvider] that will cache the component after initialisation.
 *
 * @property componentName The name of the component.
 * @property componentInitialisationFunction A function that will initialise the component.
 */
class MemoizedComponentProvider<out TComponent>(
    private val componentName: String,
    private val componentInitialisationFunction: () -> TComponent
) : ComponentProvider<TComponent> {

    override val component by lazy {
        try {
            componentInitialisationFunction()
        }
        catch (cause: Exception) {
            throw ComponentLoadException("Failed to load a $componentName component.", cause)
        }
    }
}

/**
 * Exception given when a [ComponentLoader] fails to load the component it is responsible for loading.
 *
 * @param message A message that describes what caused the component loading to fail.
 * @param innerException An optional exception that was the cause of the component load failure.
 */
class ComponentLoadException(message: String, innerException: Exception?) : Exception(message, innerException)