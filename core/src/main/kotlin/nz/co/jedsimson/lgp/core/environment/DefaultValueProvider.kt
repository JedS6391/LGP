package nz.co.jedsimson.lgp.core.environment

import java.util.Random

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

/**
 * A collection of useful [DefaultValueProvider] implementations.
 */
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

    /**
     * A [DefaultValueProvider] that gives a random, uniformly distributed double value in [0.0, 1.0).
     *
     * @returns A provider that gives a random double value.
     */
    @JvmStatic
    fun randomDoubleValueProvider(randomState: Random) = object : DefaultValueProvider<Double> {

        override val value: Double
            get() = randomState.nextDouble()
    }

    /**
     * A [DefaultValueProvider] that gives a random, normally distributed value with mean 0.0 and std. dev. 1.0.
     *
     * @return A provider that gives a random double value.
     */
    @JvmStatic
    fun randomGaussianValueProvider(randomState: Random) = object : DefaultValueProvider<Double> {

        override val value: Double
            get() = randomState.nextGaussian()
    }

    /**
     * A [DefaultValueProvider] that executes the given function when a value is requested.
     *
     * This allows for arbitrary logic to be provided by simply giving a lambda function
     * that can be executed.
     *
     * @param T The type of value that this provider gives.
     * @param func A function that takes no arguments and returns a value in the domain of T.
     * @return A provider that gives the result of a function execution as its value.
     */
    @JvmStatic
    fun <T> lambdaValueProvider(func: () -> T) = object : DefaultValueProvider<T> {
        override val value: T
            get() = func()
    }
}