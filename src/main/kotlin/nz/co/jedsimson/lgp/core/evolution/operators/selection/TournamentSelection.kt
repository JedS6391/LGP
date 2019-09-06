package nz.co.jedsimson.lgp.core.evolution.operators.selection

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.choice
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.sample
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import kotlin.random.Random

/**
 * Performs Binary Tournament Selection as described in Linear Genetic Programming (Brameier, M., Banzhaf, W. 2001).
 *
 * The steps involved are:
 * 1. Randomly select 2 * N_ts individuals from the population without replacement.
 * 2. Perform two fitness tournaments of size N_ts.
 *
 * The algorithm removes each tournament winner from the population, meaning that the original
 * population given will have it's size decreased by 2 (due to two fitness tournaments),
 * and 2 winners will be produced.
 *
 * @property tournamentSize The size of the tournaments to be held (selection pressure).
 */
class BinaryTournamentSelection<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
        environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
        private val tournamentSize: Int
) : SelectionOperator<TProgram, TOutput, TTarget>(environment) {

    private val random = this.environment.randomState
    private val sampleSize = 2 * this.tournamentSize

    /**
     * Selects two individuals from the population given using tournament selection.
     */
    override fun select(population: MutableList<Program<TProgram, TOutput>>): List<Program<TProgram, TOutput>> {
        // Select individuals from the population without replacement.
        val selected = random.sample(population, this.sampleSize).toMutableList()

        // Perform two fitness tournaments of given tournament size.
        return (0..1).map {
            val (original, winner) = tournament(selected, this.random, this.tournamentSize)

            // Remove the winners
            population.remove(original)

            winner
        }
    }

    override val information = ModuleInformation("Performs Binary Tournament Selection on a population of individuals.")
}

/**
 * A [SelectionOperator] implementation that selects individuals using Tournament Selection.
 *
 * The number of tournaments held is determined by the configuration parameter [numberOfOffspring], meaning that many
 * individuals will be chosen from the population using tournaments of the supplied [tournamentSize].

 * The size of the tournaments is determined by [tournamentSize]. Each winner of a tournament
 * is removed from the set of individuals selected to participate in the tournaments.
 *
 * @property tournamentSize The size of the tournaments to be held (selection pressure).
 * @property numberOfOffspring The number of offspring to select.
 * @see <a href="https://en.wikipedia.org/wiki/Tournament_selection"></a>
 */
class TournamentSelection<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    private val tournamentSize: Int,
    private val numberOfOffspring: Int
) : SelectionOperator<TProgram, TOutput, TTarget>(environment) {

    private val random = this.environment.randomState

    init {
        require(numberOfOffspring < this.environment.configuration.populationSize) {
            "Number of offspring must be less than the population size."
        }
    }

    /**
     * Selects individuals by performing 2 * [numberOfOffspring] tournaments of size [tournamentSize].
     */
    override fun select(population: MutableList<Program<TProgram, TOutput>>): List<Program<TProgram, TOutput>> {
        return (0 until (2 * this.numberOfOffspring)).map {
            val result = tournament(population, this.random, this.tournamentSize)

            // TODO: Do we need to return a copy?
            result.clone
        }
    }

    override val information = ModuleInformation("Performs Tournament Selection on a population of individuals.")
}

/**
 * Provides a way to access the winner of tournament selection as the original and
 * the cloned winner.
 */
private data class TournamentResult<TProgram, TOutput : Output<TProgram>>(
    val original: Program<TProgram, TOutput>,
    val clone: Program<TProgram, TOutput>
)

/**
 * Performs Tournament Selection on a population of individuals.
 *
 * If [replacement] is false, then tournament winners will be removed from the population.
 *
 * @property individuals A population of individuals.
 * @property tournamentSize The size of the tournament.
 * @property replacement Determines whether winning individuals remain in the population.
 */
private fun <TProgram, TOutput : Output<TProgram>> tournament(
    individuals: MutableList<Program<TProgram, TOutput>>,
    random: Random,
    tournamentSize: Int,
    replacement: Boolean = false
): TournamentResult<TProgram, TOutput> {

    require(individuals.isNotEmpty()) { "Population has been exhausted while performing tournament." }

    var winner = random.choice(individuals)

    for (it in 0..tournamentSize - 2) {
        val contender = random.choice(individuals)

        if (contender.fitness < winner.fitness) {
            winner = contender
        }
    }

    if (!replacement) {
        individuals.remove(winner)
    }

    // We return the original individual and a copy of it.
    return TournamentResult(winner, winner.copy())
}