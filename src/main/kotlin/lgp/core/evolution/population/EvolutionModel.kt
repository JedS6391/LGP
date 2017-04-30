package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module

/**
 * A model that can be used to perform evolution within in a specific environment.
 *
 * @param TProgram The type of programs this models evolves.
 * @property environment The environment evolution takes place within.
 */
abstract class EvolutionModel<TProgram>(val environment: Environment<TProgram>) : Module {

    /**
     * Starts the process of evolution using this model.
     */
    internal abstract fun evolve()
}