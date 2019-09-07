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

    init {
        require(tournamentSize > 0) {
            "Tournament size must be greater than zero."
        }
    }

    /**
     * Selects two individuals from the population given using tournament selection.
     */
    override fun select(population: MutableList<Program<TProgram, TOutput>>): List<Program<TProgram, TOutput>> {
        // Select individuals from the population without replacement.
        val selected = this.random.sample(population, this.sampleSize).toMutableList()

        // Perform two fitness tournaments of given tournament size.
        return (0..1).map {
            val winner = tournament(selected, this.random::choice, this.tournamentSize)

            // Remove the winner
            population.remove(winner)

            winner.copy()
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
 * By default, winners will be removed from the population, but this can be controlled via the
 * [removeWinnersFromPopulation] property.
 *
 * @property tournamentSize The size of the tournaments to be held (selection pressure).
 * @property numberOfOffspring The number of offspring to select.
 * @property removeWinnersFromPopulation Determines whether or not tournament winners should be removed from the population.
 * @see <a href="https://en.wikipedia.org/wiki/Tournament_selection"></a>
 */
class TournamentSelection<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    private val tournamentSize: Int,
    private val numberOfOffspring: Int,
    private val removeWinnersFromPopulation: Boolean = true
) : SelectionOperator<TProgram, TOutput, TTarget>(environment) {

    private val random = this.environment.randomState

    init {
        require(tournamentSize > 0) {
            "Tournament size must be greater than zero."
        }

        require(numberOfOffspring < this.environment.configuration.populationSize) {
            "Number of offspring must be less than the population size."
        }
    }

    /**
     * Selects individuals by performing 2 * [numberOfOffspring] tournaments of size [tournamentSize].
     */
    override fun select(population: MutableList<Program<TProgram, TOutput>>): List<Program<TProgram, TOutput>> {
        return (0 until (2 * this.numberOfOffspring)).map {
            val winner = tournament(population, this.random::choice, this.tournamentSize, this.removeWinnersFromPopulation)

            winner.copy()
        }
    }

    override val information = ModuleInformation("Performs Tournament Selection on a population of individuals.")
}

/**
 * Defines a function for selecting individuals from a population.
 */
internal typealias IndividualSelector<TProgram, TOutput> = (List<Program<TProgram, TOutput>>) -> Program<TProgram, TOutput>

/**
 * Performs Tournament Selection on a population of individuals.
 *
 * The given [selector] function is used to choose individuals for the tournament.
 *
 * If [removeWinnerFromPopulation] is true (the default value), then the tournament
 * winner will be removed from the population.
 *
 * @property population A population of individuals.
 * @property selector A function that can be used to select individuals for the tournament.
 * @property tournamentSize The size of the tournament.
 * @property removeWinnerFromPopulation Determines whether the winning individual remains in the population.
 */
internal fun <TProgram, TOutput : Output<TProgram>> tournament(
    population: MutableList<Program<TProgram, TOutput>>,
    selector: IndividualSelector<TProgram, TOutput>,
    tournamentSize: Int,
    removeWinnerFromPopulation: Boolean = true
): Program<TProgram, TOutput> {

    require(population.isNotEmpty()) { "Population has been exhausted while performing tournament." }

    var winner = selector(population)

    repeat(tournamentSize - 1) {
        val contender = selector(population)

        if (contender.fitness < winner.fitness) {
            winner = contender
        }
    }

    if (removeWinnerFromPopulation) {
        population.remove(winner)
    }

    return winner
}