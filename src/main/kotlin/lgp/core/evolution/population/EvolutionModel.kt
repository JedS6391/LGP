package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module

abstract class EvolutionModel<T>(val environment: Environment<T>) : Module {
    internal abstract fun initialise()
    internal abstract fun evolve()
}