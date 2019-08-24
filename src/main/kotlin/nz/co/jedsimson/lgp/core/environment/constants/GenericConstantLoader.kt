package nz.co.jedsimson.lgp.core.environment.constants

import nz.co.jedsimson.lgp.core.environment.ComponentLoaderBuilder
import nz.co.jedsimson.lgp.core.environment.MemoizedComponentProvider
import nz.co.jedsimson.lgp.core.modules.ModuleInformation

/**
 * An implementation of [ConstantLoader] that can load raw constants into a specified type.
 *
 * The type parameter controls what type the raw constants should be converted into, using a
 * suitable parsing function. This function will be used to map raw constant values to a value
 * in the domain of the type parameter.
 *
 * **NOTE:** This implementation caches the results of loading constants, because the loader
 * is immutable - that is, once it is created it can not be altered to load a different set of
 * constants. This is to ensure that the set of constants does not change within an LGP run.
 *
 * @param T The type to load constants as.
 * @property constants A collection of raw constants.
 * @property parseFunction A function that maps a raw constant to some value in the domain of this loaders type.
 */
open class GenericConstantLoader<out T>(
    private val constants: List<String>,
    private val parseFunction: (String) -> T
) : ConstantLoader<T> {

    /**
     * Creates an instance of [GenericConstantLoader] using the given builder.
     *
     * @property builder An instance of [GenericConstantLoader.Builder].
     */
    private constructor(builder: Builder<T>) : this(builder.constants, builder.parseFunction)

    // We only evaluate the constants once and then the result is cached for further loads.
    private val constantProvider = MemoizedComponentProvider("Constant") {
        this.constants.map { value ->
            this.parseFunction(value)
        }
    }

    /**
     * A custom [ComponentLoaderBuilder] implementation for building a [GenericConstantLoader] instance.
     *
     * The builder allows for a collection of constants (as strings) to be specified, which
     * will be parsed into the type specified as a type parameter.
     */
    class Builder<U> : ComponentLoaderBuilder<GenericConstantLoader<U>> {

        lateinit var constants: List<String>
        lateinit var parseFunction: (String) -> U

        /**
         * Sets the constants to be parsed as the type given.
         *
         * @param values The constant values as strings.
         * @return A builder with the updated information.
         */
        fun constants(values: List<String>) = apply {
            this.constants = values
        }

        /**
         * Sets the function to use to parse each constant.
         *
         * @param func A function to parse raw constants.
         * @return A builder with the updated information.
         */
        fun parseFunction(func: (String) -> U) = apply {
            this.parseFunction = func
        }

        /**
         * Builds an instance of [GenericConstantLoader] with the information given to the builder.
         *
         * @throws [UninitializedPropertyAccessException] When a required property of the builder has not been set.
         * @return A [GenericConstantLoader] with the information given to the builder.
         */
        override fun build(): GenericConstantLoader<U> {
            return GenericConstantLoader(this)
        }
    }

    /**
     * Loads a collection of constants.
     *
     * @return A collection of constants parsed to the correct type.
     */
    override fun load(): List<T> {
        return this.constantProvider.component
    }

    override val information = ModuleInformation(
        description = "A loader than can parse specified constants into any type specified using a given function."
    )
}
