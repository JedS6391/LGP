package nz.co.jedsimson.lgp.core.environment.dataset

import java.util.Random

/**
 * Can generate a sequence of values evenly spread between a range.
 *
 * This generator is useful for generating values between two bounds
 * with a certain interval between them (e.g. [-5, 5] in 0.5 steps.
 */
class SequenceGenerator {

    /**
     * Generates a sequence of values between [start] and [end], with an interval of [step] between each value.
     *
     * If [inclusive] is true, then the range will include the end point (i.e. [start, end]); otherwise,
     * the range will be exclusive (i.e. [start, end))
     *
     * @param start The starting point of the range of values.
     * @param end The ending point of the range of values.
     * @param step The interval between each value in the sequence.
     * @param inclusive When true, the range will include [end].
     */
    fun generate(
            start: Double,
            end: Double,
            step: Double,
            inclusive: Boolean = false
    ): Sequence<Double> = generateSequence(start) { v ->
        v + step
    }.takeWhile { v ->
        if (inclusive) {
            v <= end
        }
        else {
            v < end
        }
    }
}

/**
 * Can generate a number of uniformly distributed values.
 *
 * @param randomState An optional [Random] instance to be used as the RNG. This is provided to allow for
 *                    a deterministic sequence of values to be generated.
 */
class UniformlyDistributedGenerator(val randomState: Random = Random()) {

    /**
     * Generates [n] values that are uniformly distributed between [start] and [end].
     *
     * @param n Number of values to generate.
     * @param start Lower bound on the range of values.
     * @param end Upper bound on the range of values.
     */
    fun generate(n: Int, start: Double, end: Double): Sequence<Double> = sequence {
        (0 until n).map {
            val r = this@UniformlyDistributedGenerator.randomState.nextDouble()

            // Scaled to range
            yield(r * (end - start) + start)
        }
    }
}