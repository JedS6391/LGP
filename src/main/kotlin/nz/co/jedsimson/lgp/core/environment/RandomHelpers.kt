package nz.co.jedsimson.lgp.core.environment

import kotlin.random.Random

/**
 * Return a random element from the given list.
 */
fun <T> Random.choice(list: List<T>): T {
    return list[(this.nextDouble() * list.size).toInt()]
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
