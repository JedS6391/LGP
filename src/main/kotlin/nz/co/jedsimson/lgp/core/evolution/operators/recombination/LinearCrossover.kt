package nz.co.jedsimson.lgp.core.evolution.operators.recombination

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.events.Diagnostics
import nz.co.jedsimson.lgp.core.environment.randInt
import nz.co.jedsimson.lgp.core.evolution.copy
import nz.co.jedsimson.lgp.core.evolution.removeRange
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import kotlin.math.abs
import kotlin.math.min

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
class LinearCrossover<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    private val maximumSegmentLength: Int,
    private val maximumCrossoverDistance: Int,
    private val maximumSegmentLengthDifference: Int
) : RecombinationOperator<TProgram, TOutput, TTarget>(environment) {

    @Suppress("PrivatePropertyName")
    private val MAX_ITERATIONS = 20

    private val random = this.environment.randomState

    // Make life easier with some local variables
    private val minimumProgramLength = this.environment.configuration.minimumProgramLength
    private val maximumProgramLength = this.environment.configuration.maximumProgramLength

    /**
     * Combines the two individuals given by exchanging two segments of instructions.
     *
     * For details of the algorithm, see page 89 here:
     * https://web.archive.org/web/20190905005257/https://pdfs.semanticscholar.org/31c8/a5e106b80c07c1c0f74bcf42de6d24de2bf1.pdf
     */
    override fun combine(mother: Program<TProgram, TOutput>, father: Program<TProgram, TOutput>) {
        // Ensure that we are not trying to combine two individuals that don't have a valid length.
        if (!this.programLengthIsValid(mother) || !this.programLengthIsValid(father)) {
            throw IllegalArgumentException(
                "Mother or father program length is not valid (mother = ${mother.instructions.size}, father = ${father.instructions.size})"
            )
        }

        Diagnostics.debug("LinearCrossover-start", mapOf(
            "mother" to mother,
            "father" to father
        ))

        // First make sure that the mother is shorter than the father, since we are treating the mother as gp[1]
        // and the father as gp[2]. We also take a copy to ensure that the mother and father stay unmodified
        // until the end of the operation.
        var firstIndividual = mother.instructions.copy()
        var secondIndividual = father.instructions.copy()

        if (firstIndividual.size > secondIndividual.size) {
            val temp = firstIndividual
            firstIndividual = secondIndividual
            secondIndividual = temp
        }

        val crossoverPoints = this.determineCrossoverPoints(firstIndividual, secondIndividual) ?: return

        val segments = this.determineSegments(
            firstIndividual,
            secondIndividual,
            crossoverPoints
        ) ?: return

        val (firstNewIndividual, secondNewIndividual) = this.buildNewIndividuals(
            firstIndividual,
            secondIndividual,
            crossoverPoints,
            segments
        )

        // Replace the instructions of the original individuals to reflect the changes made using linear crossover.
        mother.instructions = firstNewIndividual
        father.instructions = secondNewIndividual

        Diagnostics.debug("LinearCrossover-end", mapOf(
            "crossoverPoints" to crossoverPoints,
            "segments" to segments,
            "mother" to mother,
            "father" to father
        ))
    }

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
    private fun determineCrossoverPoints(
        firstIndividual: MutableList<Instruction<TProgram>>,
        secondIndividual: MutableList<Instruction<TProgram>>
    ): Pair<CrossoverPoint, CrossoverPoint>? {
        // 1. Randomly select an instruction position i[k] (crossover point) in program gp[k] (k in {1, 2})
        // with len(gp[1]) <= len(gp[2]) and distance |i[1] - i[2]| <= min(len(gp[1]) -1, dc_max)

        // Randomly select some initial crossover points for each individual.
        var firstCrossoverPoint = random.nextInt(firstIndividual.size)
        var secondCrossoverPoint = random.nextInt(secondIndividual.size)

        // We restrict the loop to 20 iterations, just in case we get a really unlucky combination
        // of random instructions and end up looping indefinitely.
        var iteration = 0

        while (iteration++ < MAX_ITERATIONS &&
                !this.crossoverPointsAreValid(firstCrossoverPoint, secondCrossoverPoint, firstIndividual.size)) {
            firstCrossoverPoint = random.nextInt(firstIndividual.size)
            secondCrossoverPoint = random.nextInt(secondIndividual.size)
        }

        return when {
            iteration >= MAX_ITERATIONS -> null
            else -> Pair(firstCrossoverPoint, secondCrossoverPoint)
        }
    }

    /**
     * Determines a segment in each of [firstIndividual] and [secondIndividual] that begin at the provided [crossoverPoints].
     *
     * The segments computed will be no longer than [maximumSegmentLength] and the difference in length between the
     * two segments no greater than [maximumSegmentLengthDifference]. It is also ensured that the first segment
     * will be no longer than the second segment.
     *
     * *Note that the search for segments is bounded by [MAX_ITERATIONS]. If [MAX_ITERATIONS] is exceeded,
     * no segments will be returned (indicated by a null return).*
     *
     * @param firstIndividual The first individual to determine a segment for.
     * @param secondIndividual The second individual to determine a segment for.
     * @param crossoverPoints A pair of crossover points (one for [firstIndividual] and one for [secondIndividual]).
     * @return A pair of [Segment], one for [firstIndividual] and one for [secondIndividual].
     */
    private fun determineSegments(
        firstIndividual: MutableList<Instruction<TProgram>>,
        secondIndividual: MutableList<Instruction<TProgram>>,
        crossoverPoints: Pair<CrossoverPoint, CrossoverPoint>
    ): Pair<Segment<TProgram>, Segment<TProgram>>? {
        val (firstCrossoverPoint, secondCrossoverPoint) = crossoverPoints

        // 2. Select an instruction segment s[k] starting at position i[k] with length
        // 1 <= len(s[k]) <= min(len(gp[k]) - i[k], ls_max)
        var firstSegmentLength = random.randInt(1, min(firstIndividual.size - firstCrossoverPoint, this.maximumSegmentLength))
        var secondSegmentLength = random.randInt(1, min(secondIndividual.size - secondCrossoverPoint, this.maximumSegmentLength))

        // Take the segments: mother[i1:i1 + s1Len], father[i2:i2 + s2Len]
        var firstSegment = firstIndividual.slice(firstCrossoverPoint until (firstCrossoverPoint + firstSegmentLength))
        var secondSegment = secondIndividual.slice(secondCrossoverPoint until (secondCrossoverPoint + secondSegmentLength))

        // 3. While the difference in segment length |len(s1) - len(s2)| > ds_max reselect segment length len(s2).
        // We also take care of 4. Assure len(s1) <= len(s2). Again, we restrict the number of iterations
        // to avoid a potentially infinite loop.
        var iterations = 0

        while (iterations++ < MAX_ITERATIONS && !this.segmentSizesAreValid(firstSegment.size, secondSegment.size)) {
            firstSegmentLength = random.randInt(1, min(firstIndividual.size - firstCrossoverPoint, this.maximumSegmentLength))
            secondSegmentLength = random.randInt(1, min(secondIndividual.size - secondCrossoverPoint, this.maximumSegmentLength))

            firstSegment = firstIndividual.slice(firstCrossoverPoint until (firstCrossoverPoint + firstSegmentLength))
            secondSegment = secondIndividual.slice(secondCrossoverPoint until (secondCrossoverPoint + secondSegmentLength))
        }

        if (iterations >= MAX_ITERATIONS) {
            return null
        }

        // 5. If len(gp[2]) - (len(s2) - len(s1)) < l_min or len(gp[1]) + (len(s2) - len(s1)) > l_max then...
        if (secondIndividual.size - (secondSegment.size - firstSegment.size) < this.minimumProgramLength ||
                firstIndividual.size + (secondSegment.size - firstSegment.size) > this.maximumProgramLength) {

            // (a) Select len(s2) := len(s1) or len(s1) := len(s2) with equal probabilities.
            if (random.nextDouble() < 0.5) {
                firstSegmentLength = secondSegmentLength
            } else {
                secondSegmentLength = firstSegmentLength
            }

            // (b) If i[1] + len(s1) > len(gp[1]) then len(s1) := len(s2) := len(gp[1]) - i[1]
            if (firstCrossoverPoint + firstSegmentLength > firstIndividual.size) {
                secondSegmentLength = firstIndividual.size - firstCrossoverPoint
                firstSegmentLength = secondSegmentLength
            }

            // Need to recompute the segments with the new lengths.
            firstSegment = firstIndividual.slice(firstCrossoverPoint until (firstCrossoverPoint + firstSegmentLength))
            secondSegment = secondIndividual.slice(secondCrossoverPoint until (secondCrossoverPoint + secondSegmentLength))
        }

        return Pair(firstSegment, secondSegment)
    }

    /**
     * Constructs two new individuals based on the original [firstIndividual] and [secondIndividual] using
     * the [crossoverPoints] and [segments] previously determined.
     *
     * @param firstIndividual The first individual.
     * @param secondIndividual The second individual.
     * @param crossoverPoints Previously determined crossover points.
     * @param segments Previously determined segments to exchange.
     * @return A pair of new individuals, after exchanging the provided segments in each.
     */
    private fun buildNewIndividuals(
        firstIndividual: MutableList<Instruction<TProgram>>,
        secondIndividual: MutableList<Instruction<TProgram>>,
        crossoverPoints: Pair<CrossoverPoint, CrossoverPoint>,
        segments: Pair<Segment<TProgram>, Segment<TProgram>>
    ): Pair<MutableList<Instruction<TProgram>>, MutableList<Instruction<TProgram>>> {
        // 6. Exchange segment s1 in program gp[1] by segment s2 from program gp[2] and vice versa.

        // We take a copy of the original individuals so that we have a reference to the full set of instructions.
        val originalFirstIndividual = firstIndividual.toMutableList()
        val originalSecondIndividual = secondIndividual.toMutableList()

        val (firstCrossoverPoint, secondCrossoverPoint) = crossoverPoints
        val (firstSegment, secondSegment) = segments

        val firstSegmentEnd = firstCrossoverPoint + firstSegment.size
        val secondSegmentEnd = secondCrossoverPoint + secondSegment.size

        // First we remove the segments from each individual.
        firstIndividual.removeRange(firstCrossoverPoint until firstSegmentEnd)
        secondIndividual.removeRange(secondCrossoverPoint until secondSegmentEnd)

        // Then start constructing a new set of instructions for each program, up to the start of each segment.
        val firstNewIndividual = originalFirstIndividual.slice(0 until firstCrossoverPoint).toMutableList()
        val secondNewIndividual = originalSecondIndividual.slice(0 until secondCrossoverPoint).toMutableList()

        // Exchange the segments:
        // The segment from the first individual is added to the second individual, and vice versa.
        firstNewIndividual.addAll(secondSegment)
        secondNewIndividual.addAll(firstSegment)

        // Add everything from the original individuals that comes after the end of each segment.
        firstNewIndividual.addAll(
            originalFirstIndividual.slice(firstSegmentEnd until originalFirstIndividual.size)
        )
        secondNewIndividual.addAll(
            originalSecondIndividual.slice(secondSegmentEnd until originalSecondIndividual.size)
        )

        return Pair(firstNewIndividual, secondNewIndividual)
    }

    /**
     * Determines if the length of the supplied [program] is valid or not.
     *
     * @param program A program to check the length validity of.
     * @return Whether or not the program length is valid or not.
     */
    private fun programLengthIsValid(program: Program<TProgram, TOutput>): Boolean {
        // Program length should always fall within these bounds.
        // If it doesn't then something has gone wrong somewhere.
        return (
            program.instructions.size >= this.minimumProgramLength &&
            program.instructions.size <= this.maximumProgramLength
        )
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
    private fun crossoverPointsAreValid(
        firstCrossoverPoint: CrossoverPoint,
        secondCrossoverPoint: CrossoverPoint,
        firstIndividualSize: Int
    ): Boolean {
        // The points chosen must be close enough to each other to satisfy the maximum crossover distance constraint.
        return abs(firstCrossoverPoint - secondCrossoverPoint) <= min(firstIndividualSize - 1, this.maximumCrossoverDistance)
    }

    /**
     * Determines if the given segment sizes are valid.
     *
     * The difference in length between two segments must not be greater than the [maximumSegmentLengthDifference],
     * as well as the length of the first segment being no greater than the length of the second segment.
     *
     * @param firstSegmentSize The size of the first segment.
     * @param secondSegmentSize The size of the second segment.
     */
    private fun segmentSizesAreValid(firstSegmentSize: Int, secondSegmentSize: Int): Boolean {
        return (
            abs(firstSegmentSize - secondSegmentSize) <= this.maximumSegmentLengthDifference &&
            firstSegmentSize <= secondSegmentSize
        )
    }

    override val information = ModuleInformation("Linear Crossover operator")
}

// These aliases are used to make the code clearer to read.

/**
 * A point in a [Program] at which crossover will occur.
 */
private typealias CrossoverPoint = Int

/**
 * A segment of instructions in a [Program] that will be exchanged during crossover.
 */
private typealias Segment<T> = List<Instruction<T>>

