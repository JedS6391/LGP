package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module

abstract class RecombinationOperator<T>(private val environment: Environment<T>) : Module {
    // This recombination operator combines the mother and the father, mutating
    // the original individuals.
    abstract fun combine(mother: Program<T>, father: Program<T>)
}