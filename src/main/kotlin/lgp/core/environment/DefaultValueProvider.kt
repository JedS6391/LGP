package lgp.core.environment

import java.util.*

/**
 * An implementation will be able to provide default values to some consumer.
 *
 * @param TValue The type of the values that will be provided.
 */
interface DefaultValueProvider<out TValue> {
    /**
     * A value that will be provided to any consumers.
     */
    val value: TValue
}

object DefaultValueProviders {

    /**
     * A [DefaultValueProvider] that always returns the value it is initialised with.
     *
     * @param T The type of value that this provider gives.
     * @param value The value the provider should give.
     * @returns A provider that always gives [value].
     */
    @JvmStatic
    fun <T> constantValueProvider(value: T) = object : DefaultValueProvider<T> {
        override val value: T
            get() = value
    }

    @JvmStatic
    fun randomDoubleValueProvider() = object : DefaultValueProvider<Double> {
        private val rg = Random()

        override val value: Double
            get() = rg.nextDouble()
    }

    @JvmStatic
    fun randomGaussianValueProvider() = object : DefaultValueProvider<Double> {
        private val rg = Random()

        override val value: Double
            get() = rg.nextGaussian()
    }

    @JvmStatic
    fun <T> lambdaValueProvider(func: () -> T) = object : DefaultValueProvider<T> {
        override val value: T
            get() = func()
    }
}