package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module
import lgp.core.modules.ModuleInformation
import java.util.*

/**
 * A search operator used during evolution to select a subset of individuals from a population.
 *
 * The subset of individuals determined by the search operator are used when applying the
 * [RecombinationOperator] and [MutationOperator] to move through the search space of LGP
 * programs for the problem being solved.
 *
 * Generally, it is expected that that the individuals selected will be removed from the original
 * population, and clones of those individuals returned. These individuals can then be subjected
 * to recombination/mutation before being introduced back into a population.
 *
 * @param T The type of programs being selected.
 * @property environment The environment evolution is being performed within.
 */
abstract class SelectionOperator<T>(val environment: Environment<T>) : Module {
    /**
     * Selects a subset of programs from the population given using some method of selection.
     *
     * @param population A collection of program individuals.
     * @return A subset of the population given.
     */
    abstract fun select(population: MutableList<Program<T>>): List<Program<T>>
}

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
class BinaryTournamentSelection<T>(environment: Environment<T>,
                                   val tournamentSize: Int) : SelectionOperator<T>(environment) {

    private val random = this.environment.randomState

    /**
     * Selects two individuals from the population given using tournament selection.
     */
    override fun select(population: MutableList<Program<T>>): List<Program<T>> {
        // Select individuals from the population without replacement.
        val selected = random.sample(population, 2 * this.tournamentSize).toMutableList()

        // Perform two fitness tournaments of size tournamentSize.
        val winners = (0..1).map {
            val (original, winner) = tournament(selected, this.random, this.tournamentSize)

            // Remove the winners
            population.remove(original)

            winner
        }

        return winners
    }

    override val information = ModuleInformation("Performs Binary Tournament Selection on a population of individuals.")
}

/**
 * A [SelectionOperator] implementation that selects individuals using Tournament Selection.
 *
 * The number of tournaments held is determined by the configuration parameter
 * [lgp.core.environment.config.Config.numOffspring], meaning that `numOffspring`
 * individuals will be chosen from the population using tournaments of the given size.

 * The size of the tournaments is determined by [tournamentSize]. Each winner of a tournament
 * is removed from the set of individuals selected to participate in the tournaments.
 *
 * @property tournamentSize The size of the tournaments to be held (selection pressure).
 * @see <a href="https://en.wikipedia.org/wiki/Tournament_selection"></a>
 */
class TournamentSelection<T>(environment: Environment<T>,
                             val tournamentSize: Int) : SelectionOperator<T>(environment) {

    private val random = this.environment.randomState

    /**
     * Selects individuals by performing 2 * `numOffspring` tournaments of size [tournamentSize].
     */
    override fun select(population: MutableList<Program<T>>): List<Program<T>> {
        return (0..(2 * this.environment.config.numOffspring - 1)).map {
            tournament(population, this.random, this.tournamentSize).clone
        }
    }

    override val information = ModuleInformation("Performs Tournament Selection on a population of individuals.")
}

/**
 * Provides a way to access the winner of tournament selection as the original and
 * the cloned winner.
 */
internal data class TournamentResult<T>(val original: Program<T>, val clone: Program<T>)

/**
 * Performs Tournament Selection on a population of individuals.
 *
 * If [replacement] is false, then tournament winners will be removed from the population.
 *
 * @param T The type of the programs participating in the tournament.
 * @property individuals A population of individuals.
 * @property tournamentSize The size of the tournament.
 * @property replacement Determines whether winning individuals remain in the population.
 */
internal fun <T> tournament(
        individuals: MutableList<Program<T>>,
        random: Random,
        tournamentSize: Int,
        replacement: Boolean = false
): TournamentResult<T> {

    var winner = random.choice(individuals)

    (0..tournamentSize - 2).forEach { _ ->
        val contender = random.choice(individuals)

        if (contender.fitness < winner.fitness) {
            winner = contender
        }
    }

    if (!replacement)
        individuals.remove(winner)

    // We return the original individual and a copy of it.
    return TournamentResult(winner, winner.copy())
}

/**
 * Chooses k unique random elements from the population given.
 *
 * @see <a href="https://hg.python.org/cpython/file/2.7/Lib/random.py#l295"></a>
 */
fun <T> Random.sample(population: List<T>, k: Int): List<T> {

    val n = population.size
    val log = { a: Double, b: Double -> (Math.log(a) / Math.log(b)) }

    if (k < 0 || k > n)
        throw IllegalArgumentException("Negative sample or sample larger than population given.")

    val result = mutableListOf<T>()

    (0..(k - 1)).map { idx ->
        // Just fill the list with the first element of the population as a placeholder.
        result.add(idx, population[0])
    }

    var setSize = 21

    if (k > 5) {
        val power = Math.ceil(log((k * 3).toDouble(), 4.0))

        setSize += Math.pow(4.0, power).toInt()
    }

    if (n <= setSize) {
        val pool = population.toMutableList()

        for (i in (0 until k)) {
            val j = (this.nextDouble() * (n - i)).toInt()

            result[i] = pool[j]
            pool[j] = pool[n - i - 1]
        }
    } else {
        val selected = mutableSetOf<Int>()

        for (i in (0 until k)) {
            var j = (this.nextDouble() * n).toInt()

            while (j in selected) {
                j = (this.nextDouble() * n).toInt()
            }

            selected.add(j)
            result[i] = population[j]
        }
    }

    return result
}

/**
 * Return a random element from the given list.
 */
fun <T> Random.choice(list: List<T>): T {
    return list[(this.nextDouble() * list.size).toInt()]
}