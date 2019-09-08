package nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover

import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Responsible for providing crossover points within two individuals that are being combined using linear crossover.
 *
 * @property maximumCrossoverDistance An upper bound on the number of instructions between the two chosen segments.
 * @property random A random number generator.
 */
internal class CrossoverPointProvider<TProgram>(
    private val maximumCrossoverDistance: Int,
    private val random: Random
) {

    @Suppress("PrivatePropertyName")
    private val MAX_ITERATIONS = 20

    /**
     * Determines a crossover point for each of [firstIndividual] and [secondIndividual] with a distance
     * difference that satisfies the supplied [maximumCrossoverDistance].
     *
     * *Note that the search for crossover points is bounded by [MAX_ITERATIONS]. If [MAX_ITERATIONS] is exceeded,
     * no segments will be returned (indicated by a null return).*
     *
     * @param firstIndividual The first individual to determine a crossover point for.
     * @param secondIndividual The second individual to determine a crossover point for.
     * @return A pair of [CrossoverPoint], one for [firstIndividual] and one for [secondIndividual].
     */
    fun determineCrossoverPoints(
        firstIndividual: MutableList<Instruction<TProgram>>,
        secondIndividual: MutableList<Instruction<TProgram>>
    ): Pair<CrossoverPoint, CrossoverPoint>? {
        // 1. Randomly select an instruction position i[k] (crossover point) in program gp[k] (k in {1, 2})
        // with len(gp[1]) <= len(gp[2]) and distance |i[1] - i[2]| <= min(len(gp[1]) -1, dc_max)

        // Randomly select some initial crossover points for each individual.
        var firstCrossoverPoint = this.random.nextInt(firstIndividual.size)
        var secondCrossoverPoint = this.random.nextInt(secondIndividual.size)

        // We restrict the loop to 20 iterations, just in case we get a really unlucky combination
        // of random instructions and end up looping indefinitely.
        var iteration = 0

        while (iteration++ < MAX_ITERATIONS &&
                !this.crossoverPointsAreValid(firstCrossoverPoint, secondCrossoverPoint, firstIndividual.size)) {
            firstCrossoverPoint = this.random.nextInt(firstIndividual.size)
            secondCrossoverPoint = this.random.nextInt(secondIndividual.size)
        }

        return when {
            iteration >= MAX_ITERATIONS -> null
            else -> Pair(firstCrossoverPoint, secondCrossoverPoint)
        }
    }

    /**
     * Determines if the given crossover points are valid.
     *
     * The distance between two crossover points must not be greater than the [maximumCrossoverDistance].
     *
     * @param firstCrossoverPoint The first crossover point.
     * @param secondCrossoverPoint The second crossover point.
     * @param firstIndividualSize The size of the first individual that crossover is being performed on.
     * @return Whether or not the crossover points are valid.
     */
    fun crossoverPointsAreValid(
        firstCrossoverPoint: CrossoverPoint,
        secondCrossoverPoint: CrossoverPoint,
        firstIndividualSize: Int
    ): Boolean {
        // The points chosen must be close enough to each other to satisfy the maximum crossover distance constraint.
        return abs(firstCrossoverPoint - secondCrossoverPoint) <= min(firstIndividualSize - 1, this.maximumCrossoverDistance)
    }
}

/**
 * A point in a [Program] at which crossover will occur.
 */
internal typealias CrossoverPoint = Int
