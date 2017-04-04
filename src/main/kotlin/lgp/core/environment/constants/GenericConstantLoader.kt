package lgp.core.environment.constants

import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.modules.ModuleInformation

class GenericConstantLoader<T> : ConstantLoader<T> {

    val constants: List<String>
    val parseFunction: (String) -> T

    public constructor(constants: List<String>, parseFunction: (String) -> T) {
        this.constants = constants
        this.parseFunction = parseFunction
    }

    private constructor(builder: Builder<T>) : this(builder.constants, builder.parseFunction)

    /**
     * A custom [ComponentLoaderBuilder] for this [ComponentLoader].
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
        fun constants(values: List<String>): Builder<U> {
            this.constants = values

            return this
        }

        /**
         * Sets the function to use to parse each constant.
         */
        fun parseFunction(func: (String) -> U): Builder<U> {
            this.parseFunction = func

            return this
        }

        /**
         * Builds an instance of [GenericConstantLoader] with the information given to the builder.
         *
         * @throws [UninitializedPropertyAccessException] when a required property of the builder has not been set.
         * @return a [GenericConstantLoader] with the information given to the builder.
         */
        override fun build(): GenericConstantLoader<U> {
            return GenericConstantLoader(this)
        }
    }

    override fun load(): List<T> {
        return this.constants.map { value ->
            this.parseFunction(value)
        }
    }

    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "A loader than can parse specified constants into any type specified using a given function."
    }
}
