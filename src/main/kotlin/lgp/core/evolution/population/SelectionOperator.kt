package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module

abstract class SelectionOperator<T>(private val environment: Environment<T>) : Module {
    // TODO: What should this return?
    // Should selection be one at a time from a population and up-to the consuming population
    // to figure out what to do with it. Or should, it return a new collection of individuals
    // determined from the original population.
    abstract fun select(population: Population<T>): Program<T>
}