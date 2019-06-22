package nz.co.jedsimson.lgp.core.evolution.operators

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