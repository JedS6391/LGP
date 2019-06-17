package nz.co.jedsimson.lgp.core.environment.operations

import nz.co.jedsimson.lgp.core.environment.ComponentLoaderBuilder
import nz.co.jedsimson.lgp.core.environment.MemoizedComponentProvider
import nz.co.jedsimson.lgp.core.program.instructions.Operation
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.modules.ModuleInformation

class InvalidOperationSpecificationException(message: String) : Exception(message)

/**
 * A simple operation loader implementation that simply loads a set of operations
 * specified by the name of the package + class it can be found in.
 *
 * @param T Type of the operations to be loaded.
 */
class DefaultOperationLoader<T> constructor(private val operationNames: List<String>) : OperationLoader<T> {

    private constructor(builder: Builder<T>) : this(builder.operationNames)

    private val operationProvider = MemoizedComponentProvider("Operation") {
        this.operationNames.map {
            name ->
            this.loadOperation(name)
        }
    }

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
        return this.operationProvider.component
    }

    private fun loadOperation(name: String): Operation<T> {
        try {
            // Load the class as a raw class (i.e. Class<*>).
            val clazz = this.javaClass.classLoader.loadClass(name)

            // Try to create an instance: If we're loading an implementation of
            // `Operation<T>` then we should be able to safely cast it to the base
            // `Module` type.
            val instance = clazz.newInstance() as? Module
                    ?: throw InvalidOperationSpecificationException("$name is not a valid Module.")

            // By this time we know that our instance is at least a `Module` impl., so we can try
            // cast it to `Operation<T>`. It is possible that the cast will fail, such as when
            // a `Module` impl is given that is not an operation (e.g. a `ComponentLoader`).
            @Suppress("UNCHECKED_CAST")
            val operation = instance as? Operation<T>
                    ?: throw InvalidOperationSpecificationException("$name is not a valid Operation.")

            return operation
        }
        catch (classNotFound: ClassNotFoundException) {
            // The class specified probably doesn't exist. Wrap this error up nicely.
            throw InvalidOperationSpecificationException("The class $name could not be found.")
        }
    }

    override val information = ModuleInformation(
        description = "A loader than can load operations from a collection of strings naming those operations by class."
    )
}
