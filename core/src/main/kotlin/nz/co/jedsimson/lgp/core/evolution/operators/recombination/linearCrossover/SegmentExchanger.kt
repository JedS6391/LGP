package nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover

import nz.co.jedsimson.lgp.core.evolution.removeRange
import nz.co.jedsimson.lgp.core.program.instructions.Instruction

/**
 * Responsible for exchanging segments between two individuals during linear crossover.
 */
internal class SegmentExchanger<TProgram> {

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
    fun buildNewIndividuals(
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
}