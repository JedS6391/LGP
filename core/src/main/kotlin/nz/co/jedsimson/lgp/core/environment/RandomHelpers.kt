package nz.co.jedsimson.lgp.core.environment

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random

/**
 * Return a random element from the given list.
 */
fun <T> Random.choice(list: List<T>): T {
    val randomMultiplier = this.nextDouble()

    return list[(randomMultiplier * list.size).toInt()]
}

/**
 * Chooses a random integer in the range [min, max] (i.e. min <= x <= max).
 *
 * @param min The lower, inclusive bound of the random integer.
 * @param max The upper, inclusive bound of the random integer.
 * @return A random integer between min and max inclusive.
 */
fun Random.randInt(min: Int, max: Int): Int {
    return this.nextInt(max - min + 1) + min
}

/**
 * Chooses k unique random elements from the population given.
 *
 * @see <a href="https://hg.python.org/cpython/file/2.7/Lib/random.py#l295">Python random.sample reference</a>
 */
fun <T> Random.sample(population: List<T>, k: Int): List<T> {

    val n = population.size
    val log = { a: Double, b: Double -> (ln(a) / ln(b)) }

    if (k < 0 || k > n) {
        throw IllegalArgumentException("Negative sample or sample larger than population given.")
    }

    val result = mutableListOf<T>()

    (0 until k).map { idx ->
        // Just fill the list with the first element of the population as a placeholder.
        result.add(idx, population[0])
    }

    var setSize = 21

    if (k > 5) {
        val power = ceil(log((k * 3).toDouble(), 4.0))

        setSize += 4.0.pow(power).toInt()
    }

    if (n <= setSize) {
        val pool = population.toMutableList()

        for (i in (0 until k)) {
            val j = (this.nextDouble() * (n - i)).toInt()

            result[i] = pool[j]
            pool[j] = pool[n - i - 1]
        }
    } else {
        val selected = mutableSetOf<Int>()

        for (i in (0 until k)) {
            var j = (this.nextDouble() * n).toInt()

            while (j in selected) {
                j = (this.nextDouble() * n).toInt()
            }

            selected.add(j)
            result[i] = population[j]
        }
    }

    return result
}