package nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro

import kotlin.random.Random
import kotlin.random.asJavaRandom


/**
 * A function that can be used to mutate a constant value.
 *
 * Used by the [MicroMutationOperator] implementation.
 */
typealias ConstantMutationFunction<T> = (T) -> T

/**
 * A collection of [ConstantMutationFunction] implementations for use by a [MicroMutationOperator].
 */
object ConstantMutationFunctions {

    /**
     * A [ConstantMutationFunction] which simply returns the original constant value unchanged.
     */
    @JvmStatic
    fun <T> identity(): ConstantMutationFunction<T> {
        return { v -> v }
    }

    /**
     * A [ConstantMutationFunction] which returns the original constant with a small amount of gaussian noise added.
     *
     * @param randomState The system random number generator.
     */
    @JvmStatic
    fun randomGaussianNoise(randomState: Random): ConstantMutationFunction<Double> {
        return { v ->
            v + (randomState.asJavaRandom().nextGaussian() * 1)
        }
    }
}