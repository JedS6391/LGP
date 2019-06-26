package nz.co.jedsimson.lgp.core.evolution.operators.recombination

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.randInt
import nz.co.jedsimson.lgp.core.evolution.operators.slice
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program


/**
 * A [RecombinationOperator] that implements Linear Crossover for two individuals.
 *
 * For more information, see Algorithm 5.1 from Linear Genetic Programming (Brameier, M., Banzhaf, W. 2001).
 *
 * @property maximumSegmentLength An upper bound on the size of the segments exchanged between the individuals.
 * @property maximumCrossoverDistance An upper bound on the number of instructions between the two chosen segments.
 * @property maximumSegmentLengthDifference An upper bound on the difference between the two segment lengths.
 * @see <a href="http://www.springer.com/gp/book/9780387310299">http://www.springer.com/gp/book/9780387310299</a>
 */
class LinearCrossover<TProgram, TOutput : Output<TProgram>>(
        environment: EnvironmentFacade<TProgram, TOutput>,
        private val maximumSegmentLength: Int,
        private val maximumCrossoverDistance: Int,
        private val maximumSegmentLengthDifference: Int
) : RecombinationOperator<TProgram, TOutput>(environment) {

    private val random = this.environment.randomState

    // Make life easier with some local variables
    private val minimumProgramLength = this.environment.configuration.minimumProgramLength
    private val maximumProgramLength = this.environment.configuration.maximumProgramLength

    /**
     * Combines the two individuals given by exchanging two segments of instructions.
     */
    override fun combine(mother: Program<TProgram, TOutput>, father: Program<TProgram, TOutput>) {
        // 1. Randomly select an instruction position i[k] (crossover point) in program gp[k] (k in {1, 2})
        // with len(gp[1]) <= len(gp[2]) and distance |i[1] - i[2]| <= min(len(gp[1]) -1, dc_max)

        // First make sure that the mother is shorter than the father, since we are treating
        // the mother as gp[1] and the father as gp[2].
        // TODO: Is there a better way to swap in Kotlin?
        var ind1 = mother.instructions
        var ind2 = father.instructions

        if (ind1.size > ind2.size) {
            val temp = ind1
            ind1 = ind2
            ind2 = temp
        }

        // Randomly select some initial crossover points for each individual.
        var i1 = random.nextInt(ind1.size)
        var i2 = random.nextInt(ind2.size)

        // Make sure the points chosen are close enough to each other to satisfy
        // the maximum crossover distance constraint.
        // We restrict the loop to 20 iterations, just in case we get a really unlucky combination
        // of random instructions and end up looping indefinitely.
        var i = 0

        while (Math.abs(i1 - i2) > Math.min(ind1.size - 1, maximumCrossoverDistance) && i++ < 20) {
            i1 = random.nextInt(ind1.size)
            i2 = random.nextInt(ind2.size)
        }

        // 2. Select an instruction segment s[k] starting at position i[k] with length
        // 1 <= len(s[k]) <= min(len(gp[k]) - i[k], ls_max)
        var s1Len = random.randInt(1, Math.min(ind1.size - i1, maximumSegmentLength))
        var s2Len = random.randInt(1, Math.min(ind2.size - i2, maximumSegmentLength))

        // Take the segments: mother[i1:i1 + s1Len], father[i2:i2 + s2Len]
        var s1 = ind1.slice(i1..(i1 + s1Len))
        var s2 = ind2.slice(i2..(i2 + s2Len))

        // 3. While the difference in segment length |len(s1) - len(s2)| > ds_max reselect segment length len(s2).
        // We also take care of 4. Assure len(s1) <= len(s2). Again, we restrict the number of iterations
        // to avoid a potentially infinite loop.
        i = 0

        while ((Math.abs(s1.size - s2.size) > maximumSegmentLengthDifference || s1.size > s2.size) && i++ < 20) {
            s1Len = random.randInt(1, Math.min(ind1.size - i1, maximumSegmentLength))
            s2Len = random.randInt(1, Math.min(ind2.size - i2, maximumSegmentLength))

            s1 = ind1.slice(i1..(i1 + s1Len))
            s2 = ind2.slice(i2..(i2 + s2Len))
        }

        // Invariant: len(s1) <= len(s2) (4.)
        assert(s1.size <= s2.size)

        // 5. If len(gp[2]) - (len(s2) - len(s1)) < l_min or len(gp[1]) + (len(s2) - len(s1)) > l_max then...
        if (ind2.size - (s2.size - s1.size) < minimumProgramLength ||
                ind1.size + (s2.size - s1.size) > maximumProgramLength) {

            // (a) Select len(s2) := len(s1) or len(s1) := len(s2) with equal probabilities.
            if (random.nextDouble() < 0.5) {
                s1Len = s2Len
            } else {
                s2Len = s1Len
            }

            // (b) If i[1] + len(s1) > len(gp[1]) then len(s1) := len(s2) := len(gp[1]) - i[1]
            if (i1 + s1.size > ind1.size) {
                s2Len = ind1.size - i1
                s1Len = s2Len
            }

            // Need to recompute the segments with the new lengths.
            s1 = ind1.slice(i1..(i1 + s1Len))
            s2 = ind2.slice(i2..(i2 + s2Len))
        }

        // 6. Exchange segment s1 in program gp[1] by segment s2 from program gp[2] and vice versa.

        // First we remove the segments from each individual.
        ind1.slice(i1..(i1 + s1.size)).clear()
        ind2.slice(i2..(i2 + s2.size)).clear()

        // Then start constructing a new set of instructions for each program, up to the start of each segment.
        val newInd1 = ind1.slice(0..i1)
        val newInd2 = ind2.slice(0..i2)

        // Exchange the segments:
        // The segment from the first individual is added to the second individual, and vice versa.
        newInd1.addAll(s2)
        newInd2.addAll(s1)

        // Add everything from the original individuals that comes after the end of each segment.
        newInd1.addAll(ind1.slice((i1 + s1.size)..ind1.lastIndex))
        newInd2.addAll(ind2.slice((i2 + s2.size)..ind2.lastIndex))

        // Replace the instructions of the original individuals to reflect the
        // changes made using linear crossover.
        mother.instructions = newInd1
        father.instructions = newInd2
    }

    override val information = ModuleInformation("Linear Crossover operator")
}