package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module
import lgp.core.modules.ModuleInformation
import java.util.*

abstract class SelectionOperator<T>(val environment: Environment<T>) : Module {
    // TODO: What should this return?
    // Should selection be one at a time from a population and up-to the consuming population
    // to figure out what to do with it. Or should, it return a new collection of individuals
    // determined from the original population.
    abstract fun select(individuals: List<Program<T>>): List<Program<T>>
}

class TournamentSelection<T>(environment: Environment<T>, val tournamentSize: Int) : SelectionOperator<T>(environment) {
    override val information = ModuleInformation("Tournament Selection")

    override fun select(individuals: List<Program<T>>): List<Program<T>> {
        val offspring = (0..this.environment.config.numOffspring - 1).map {
            // TODO: Allow tournament size to be configured through config
            this.tournament(individuals, tournamentSize)
        }

        // TODO: Make sure a copy of the individuals is returned
        return offspring
    }

    fun tournament(individuals: List<Program<T>>, tournamentSize: Int): Program<T> {
        val rg = Random()
        var winner = rg.choice(individuals)

        for (k in 0..tournamentSize - 1) {
            val contender = rg.choice(individuals)

            if (contender.fitness < winner.fitness) {
                winner = contender
            }
        }

        // We need to return a copy so that the copy
        // can be mutated without changing the original.
        return winner.copy()
    }
}