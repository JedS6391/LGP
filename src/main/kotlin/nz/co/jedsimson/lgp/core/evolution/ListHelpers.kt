package nz.co.jedsimson.lgp.core.evolution

import kotlin.streams.toList

// Extension methods for various functionality that is nice to have.

/**
 * Returns a view of a list between the range given.
 *
 * The range is mutable and will modify the underlying list.
 *
 * @param range A collection of indices that the slice should contain.
 * @return A list of elements whose indices fall between the range given.
 */
fun <T> MutableList<T>.slice(range: IntRange): MutableList<T> {
    return this.filterIndexed { idx, _ -> idx in range }.toMutableList()
}

/**
 * Computes the standard deviation of the list from the given [mean].
 *
 * @param mean The mean to calculate the standard deviation from.
 */
fun List<Double>.standardDeviation(mean: Double = this.average()): Double {
    val variance = this.map { x -> Math.pow(x - mean, 2.0) }.sum()

    return Math.pow((variance / this.size), 0.5)
}

/**
 * A version of [List.map] that applies the given [transform] in parallel.
 *
 * @param transform A transformation to apply to elements of the list.
 */
fun <T, R> List<T>.pmap(transform: (T) -> R): List<R> {
    return this.parallelStream().map(transform).toList()
}

/**
 * Produces a new list from the original list with elements grouped in pairs.
 *
 * e.g. [1, 2, 3, 4] -> [(1, 2), (3, 4)]
 *      [1, 2, 3]    -> [(1, 2)]
 */
fun <T> List<T>.pairwise(): List<Pair<T, T>> {
    return (0 until this.size - 1 step 2).map { idx ->
        Pair(this[idx], this[idx + 1])
    }
}