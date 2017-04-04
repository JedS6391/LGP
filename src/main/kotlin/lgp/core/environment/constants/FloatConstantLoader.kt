package lgp.core.environment.constants

import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.modules.ModuleInformation

class FloatConstantLoader constructor(val constants: List<String>): ConstantLoader<Double> {
    // TODO: Subclass `GenericConstantLoader` and use that to parse

    private constructor(builder: Builder) : this(builder.constants)

    /**
     * A custom [ComponentLoaderBuilder] for this [ComponentLoader].
     *
     * The builder allows for a collection of constants (as strings) to be specified, which
     * will be parsed into floats.
     */
    class Builder : ComponentLoaderBuilder<FloatConstantLoader> {

        lateinit var constants: List<String>

        /**
         * Sets the constants to be parsed as floats.
         *
         * @param values The constant values as strings.
         * @return A builder with the updated information.
         */
        fun constants(values: List<String>): Builder {
            this.constants = values

            return this
        }

        /**
         * Builds an instance of [FloatConstantLoader] with the information given to the builder.
         *
         * @throws [UninitializedPropertyAccessException] when a required property of the builder has not been set.
         * @return a [FloatConstantLoader] with the information given to the builder.
         */
        override fun build(): FloatConstantLoader {
            return FloatConstantLoader(this)
        }
    }

    override fun load(): List<Double> {
        return this.constants.map(String::toDouble)
    }

    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "A loader than can parse specified constants into floats."
    }
}