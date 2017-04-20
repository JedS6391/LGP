package lgp.core.environment.operations

import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.evolution.instructions.Operation
import lgp.core.modules.ModuleInformation
import lgp.core.modules.ModuleLoader

/**
 * A simple operation loader implementation that simply loads a set of operations
 * specified by the name of the package + class it can be found in.
 *
 * @param T Type of the operations to be loaded.
 */
class DefaultOperationLoader<T> constructor(val operationNames: List<String>): OperationLoader<T> {

    private constructor(builder: Builder<T>) : this(builder.operationNames)

    /**
     * Builds an instance of [DefaultOperationLoader].
     *
     * @param U Type of the operations to be loaded.
     */
    class Builder<U> : ComponentLoaderBuilder<DefaultOperationLoader<U>> {

        lateinit var operationNames: List<String>

        /**
         * Sets the names of the operations to load.
         *
         * @param names Collection of strings naming the package/class where an [Operation] can be found.
         * @return A builder with the updated information.
         */
        fun operations(names: List<String>): Builder<U> {
            this.operationNames = names

            return this
        }

        /**
         * Builds an instance of [DefaultOperationLoader] with the configuration
         * information given to the builder.
         *
         * @throws [UninitializedPropertyAccessException] when a required property of the builder has not been set.
         * @returns An instance of [DefaultOperationLoader].
         */
        override fun build(): DefaultOperationLoader<U> {
            return DefaultOperationLoader(this)
        }
    }

    /**
     * Loads a collection of [Operation]s with the names given to the loader.
     *
     * @returns A list of [Operation]s.
     */
    override fun load(): List<Operation<T>> {
        val moduleLoader = ModuleLoader()

        return this.operationNames.map { name ->
            moduleLoader.instanceOf(name) as Operation<T>
        }
    }

    override val information = ModuleInformation(
        description = "A loader than can load operations from a collection of strings naming those operations by class."
    )
}
