package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module

abstract class MutationOperator<T>(private val environment: Environment<T>) : Module {
    abstract fun mutate(individual: Program<T>)
}