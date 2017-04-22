package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module
import lgp.core.modules.ModuleInformation
import java.util.*

abstract class RecombinationOperator<T>(val environment: Environment<T>) : Module {
    // This recombination operator combines the mother and the father, mutating
    // the original individuals.
    abstract fun combine(mother: Program<T>, father: Program<T>)
}

// Implements Algorithm 5.1 from Linear Genetic Programming (Brameier, M., Banzhaf, W. 2001)
class Crossover<T>(environment: Environment<T>,
                   val maximumSegmentLength: Int,
                   val maximumCrossoverDistance: Int,
                   val maximumSegmentLengthDifference: Int
) : RecombinationOperator<T>(environment) {

    private val rg = Random()

    // Make life easier with some local variables
    val minimumProgramLength = this.environment.config.minimumProgramLength
    val maximumProgramLength = this.environment.config.maximumProgramLength

    override fun combine(mother: Program<T>, father: Program<T>) {
        // TODO: Tidy this up...
        // 1. Randomly select an instruction position i[k] (crossover point) in program gp[k] (k in {1, 2})
        // with len(gp[1]) <= len(gp[2]) and distance |i[1] - i[2]| <= min(len(gp[1]) -1, dc_max)

        // First make sure that the mother is shorter than the father
        var ind1 = mother.instructions
        var ind2 = father.instructions

        if (ind1.size > ind2.size) {
            var (ind1, ind2) = swap(ind1, ind2)
        }

        // Find crossover points
        var i1 = rg.nextInt(ind1.size)
        var i2 = rg.nextInt(ind2.size)

        // Make sure points are close enough to each other
        while (Math.abs(i1 - i2) > Math.min(ind1.size - 1, maximumCrossoverDistance)) {
            i1 = rg.nextInt(ind1.size)
            i2 = rg.nextInt(ind2.size)
        }

        // 2. Select an instruction segment s[k] starting at position i[k] with length
        // 1 <= len(s[k]) <= min(len(gp[k]) - i[k], ls_max)
        var s1Len = rg.randInt(1, Math.min(ind1.size - i1, maximumSegmentLength))
        var s2Len = rg.randInt(1, Math.min(ind2.size - i2, maximumSegmentLength))

        // Take the segments: mother[i1:i1 + s1Len], father[i2:i2 + s2Len]
        var s1 = ind1.slice(i1..(i1 + s1Len))
        var s2 = ind2.slice(i2..(i2 + s2Len))

        // 3. While the difference in segment length |len(s1) - len(s2)| > ds_max reselect segment length len(s2).
        // We also take care of 4. Assure len(s1) <= len(s2)
        while (Math.abs(s1.size - s2.size) > maximumSegmentLengthDifference || s1.size > s2.size) {
            s1Len = rg.randInt(1, Math.min(ind1.size - i1, maximumSegmentLength))
            s2Len = rg.randInt(1, Math.min(ind2.size - i2, maximumSegmentLength))

            s1 = ind1.slice(i1..(i1 + s1Len))
            s2 = ind2.slice(i2..(i2 + s2Len))
        }

        // Invariant: len(s1) <= len(s2) (4.)
        assert(s1.size <= s2.size)

        // 5. If len(gp[2]) - (len(s2) - len(s1)) < l_min or len(gp[1]) + (len(s2) - len(s1)) > l_max then...
        if (ind2.size - (s2.size - s1.size) < minimumProgramLength ||
                ind1.size + (s2.size - s1.size) > maximumProgramLength) {

            // (a) Select len(s2) := len(s1) or len(s1) := len(s2) with equal probabilities.
            if (rg.nextDouble() < 0.5) {
                s1Len = s2Len
            } else {
                s2Len = s1Len
            }

            // (b) If i[1] + len(s1) > len(gp[1]) then len(s1) := len(s2) := len(gp[1]) - i[1]
            if (i1 + s1.size > ind1.size) {
                s2Len = ind1.size - i1
                s1Len = s2Len
            }
        }

        // 6. Exchange segment s1 in program gp[1] by segment s2 from program gp[2] and vice versa.
        ind1.slice(i1..(i1 + s1.size)).clear()
        ind2.slice(i2..(i2 + s2.size)).clear()

        val newInd1 = ind1.slice(0..i1)
        val newInd2 = ind2.slice(0..i2)

        newInd1.addAll(s2)
        newInd2.addAll(s1)

        newInd1.addAll(ind1.slice((i1 + s1.size)..ind1.lastIndex))
        newInd2.addAll(ind2.slice((i2 + s2.size)..ind2.lastIndex))

        mother.instructions = newInd1
        father.instructions = newInd2
    }

    override val information = ModuleInformation("Linear Crossover operator")
}

fun <T> swap(a: T, b: T): Pair<T, T> {
    return Pair(b, a)
}

fun Random.randInt(min: Int, max: Int): Int {
    return this.nextInt(max - min + 1) + min
}

fun <T> MutableList<T>.slice(range: IntRange): MutableList<T> {
    return this.filterIndexed { idx, _ -> idx in range}.toMutableList()
}