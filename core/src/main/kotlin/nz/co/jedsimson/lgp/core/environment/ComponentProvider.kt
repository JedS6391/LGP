package nz.co.jedsimson.lgp.core.environment

import nz.co.jedsimson.lgp.core.environment.logging.Logger
import nz.co.jedsimson.lgp.core.environment.logging.LoggerProvider

/**
 * Defines a provider of [TComponent] instances.
 */
interface ComponentProvider<out TComponent> {

    /**
     * The component that this provider is responsible for.
     */
    val component: TComponent
}

/**
 * An implementation of [ComponentProvider] that will cache the component after initialisation.
 *
 * @param TComponent The type of the component that this provider is responsible for.
 * @property componentName The name of the component.
 * @property componentInitialisationFunction A function that will initialise the component.
 * @constructor Creates a new [MemoizedComponentProvider] for components with the given [componentName],
 *              which can be created using the given [componentInitialisationFunction].
 */
class MemoizedComponentProvider<out TComponent>(
    private val componentName: String,
    private val componentInitialisationFunction: (logger: Logger) -> TComponent
) : ComponentProvider<TComponent> {

    private val logger = LoggerProvider.getLogger(MemoizedComponentProvider::class.qualifiedName!!)

    override val component by lazy {
        try {
            logger.debug { "Initialising $componentName component" }

            componentInitialisationFunction(logger)
        }
        catch (cause: Exception) {
            logger.error(cause) { "Failed to load $componentName component" }

            throw ComponentLoadException("Failed to load $componentName component.", cause)
        }
    }
}