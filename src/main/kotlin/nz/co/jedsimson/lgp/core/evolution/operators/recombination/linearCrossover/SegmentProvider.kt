package nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover

import nz.co.jedsimson.lgp.core.environment.randInt
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Responsible for providing segments within two individuals that will be exchanged using linear crossover.
 *
 * @property maximumSegmentLength An upper bound on the size of the segments exchanged between the individuals.
 * @property maximumSegmentLengthDifference An upper bound on the difference between the two segment lengths.
 * @property minimumProgramLength The minimum program length that must be adhered to.
 * @property maximumProgramLength The maximum program length that must be adhered to.
 * @property random A random number generator.
 */
internal class SegmentProvider<TProgram>(
    private val maximumSegmentLength: Int,
    private val maximumSegmentLengthDifference: Int,
    private val minimumProgramLength: Int,
    private val maximumProgramLength: Int,
    private val random: Random
) {

    @Suppress("PrivatePropertyName")
    private val MAX_ITERATIONS = 20

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
    fun determineSegments(
        firstIndividual: MutableList<Instruction<TProgram>>,
        secondIndividual: MutableList<Instruction<TProgram>>,
        crossoverPoints: Pair<CrossoverPoint, CrossoverPoint>
    ): Pair<Segment<TProgram>, Segment<TProgram>>? {
        val (firstCrossoverPoint, secondCrossoverPoint) = crossoverPoints

        // 2. Select an instruction segment s[k] starting at position i[k] with length
        // 1 <= len(s[k]) <= min(len(gp[k]) - i[k], ls_max)
        var firstSegmentLength = this.random.randInt(1, min(firstIndividual.size - firstCrossoverPoint, this.maximumSegmentLength))
        var secondSegmentLength = this.random.randInt(1, min(secondIndividual.size - secondCrossoverPoint, this.maximumSegmentLength))

        // Take the segments: mother[i1:i1 + s1Len], father[i2:i2 + s2Len]
        var firstSegment = firstIndividual.slice(firstCrossoverPoint until (firstCrossoverPoint + firstSegmentLength))
        var secondSegment = secondIndividual.slice(secondCrossoverPoint until (secondCrossoverPoint + secondSegmentLength))

        // 3. While the difference in segment length |len(s1) - len(s2)| > ds_max reselect segment length len(s2).
        // We also take care of 4. Assure len(s1) <= len(s2). Again, we restrict the number of iterations
        // to avoid a potentially infinite loop.
        var iterations = 0

        while (iterations++ < MAX_ITERATIONS && !this.segmentSizesAreValid(firstSegment.size, secondSegment.size)) {
            firstSegmentLength = this.random.randInt(1, min(firstIndividual.size - firstCrossoverPoint, this.maximumSegmentLength))
            secondSegmentLength = this.random.randInt(1, min(secondIndividual.size - secondCrossoverPoint, this.maximumSegmentLength))

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
            if (this.random.nextDouble() < 0.5) {
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
     * Determines if the given segment sizes are valid.
     *
     * The difference in length between two segments must not be greater than the [maximumSegmentLengthDifference],
     * as well as the length of the first segment being no greater than the length of the second segment.
     *
     * @param firstSegmentSize The size of the first segment.
     * @param secondSegmentSize The size of the second segment.
     */
    fun segmentSizesAreValid(firstSegmentSize: Int, secondSegmentSize: Int): Boolean {
        return (
            abs(firstSegmentSize - secondSegmentSize) <= this.maximumSegmentLengthDifference &&
            firstSegmentSize <= secondSegmentSize
        )
    }
}

/**
 * A segment of instructions in a [Program] that will be exchanged during crossover.
 */
internal typealias Segment<T> = List<Instruction<T>>

