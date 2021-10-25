package nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.events.Diagnostics
import nz.co.jedsimson.lgp.core.evolution.copy
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.RecombinationOperator
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
class LinearCrossover<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    private val maximumSegmentLength: Int,
    private val maximumCrossoverDistance: Int,
    private val maximumSegmentLengthDifference: Int
) : RecombinationOperator<TProgram, TOutput, TTarget>(environment) {

    private val random = this.environment.randomState
    private val crossoverPointProvider = CrossoverPointProvider<TProgram>(this.maximumCrossoverDistance, this.random)
    private val segmentProvider = SegmentProvider<TProgram>(
        this.maximumSegmentLength,
        this.maximumSegmentLengthDifference,
        this.environment.configuration.minimumProgramLength,
        this.environment.configuration.maximumProgramLength,
        this.random
    )
    private val segmentExchanger = SegmentExchanger<TProgram>()

    private val minimumProgramLength = this.environment.configuration.minimumProgramLength
    private val maximumProgramLength = this.environment.configuration.maximumProgramLength

    init {
        require(this.maximumSegmentLength > 0) { "Maximum segment length must be greater than zero" }
    }

    /**
     * Combines the two individuals given by exchanging two segments of instructions.
     *
     * For details of the algorithm, see page 89 here:
     * https://web.archive.org/web/20190905005257/https://pdfs.semanticscholar.org/31c8/a5e106b80c07c1c0f74bcf42de6d24de2bf1.pdf
     */
    override fun combine(mother: Program<TProgram, TOutput>, father: Program<TProgram, TOutput>) {
        // Ensure that we are not trying to combine two individuals that don't have a valid length.
        require(this.programLengthIsValid(mother)) { "Mother program length is not valid (length = ${mother.instructions.size})" }
        require(this.programLengthIsValid(father)) { "Father program length is not valid (length = ${father.instructions.size})" }

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

        val crossoverPoints = this.crossoverPointProvider.determineCrossoverPoints(
            firstIndividual,
            secondIndividual
        ) ?: return

        val segments = this.segmentProvider.determineSegments(
            firstIndividual,
            secondIndividual,
            crossoverPoints
        ) ?: return

        val (firstNewIndividual, secondNewIndividual) = this.segmentExchanger.buildNewIndividuals(
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

    override val information = ModuleInformation("Linear Crossover operator")
}
