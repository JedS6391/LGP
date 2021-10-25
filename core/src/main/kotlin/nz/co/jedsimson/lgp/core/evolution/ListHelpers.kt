package nz.co.jedsimson.lgp.core.evolution

import kotlin.streams.toList

// Extension methods for various functionality that is nice to have.

/**
 * Takes a copy of this list.
 */
fun <T> MutableList<T>.copy(): MutableList<T> {
    return this.toMutableList()
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

/**
 * Removes any elements from this list with indices that fall within the given range.
 *
 * *Note*: The method will perform no removal if the range start and end are equal.
 *
 * @param range A range of indices of elements to remove from this list.
 * @throws IndexOutOfBoundsException when:
 * - the range start is greater than or equal to the size of the list
 * - the range end is greater than the size of the list
 * - the range start is greater than the range end
 */
fun <T> MutableList<T>.removeRange(range: IntRange) {
    val from = range.first
    val to = range.last

    when {
        // Nothing to do
        from == to -> return
        // Invalid range
        from >= this.size -> throw IndexOutOfBoundsException("from ($from) >= size ($size)")
        to > this.size    -> throw IndexOutOfBoundsException("to ($to) > size ($size)")
        from > to         -> throw IndexOutOfBoundsException("from ($from) > to ($to)")
    }

    val filtered = this.filterIndexed { idx, _ -> idx !in range }

    this.clear()

    this.addAll(filtered)
}