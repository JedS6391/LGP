package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module

/**
 * The best individual and final population from the result of evolution.
 *
 * @property best The best program at the end of the evolution process.
 * @property individuals The population at the end of the evolution process.
 */
data class EvolutionResult<T>(val best: Program<T>, val individuals: List<Program<T>>)

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
    abstract fun evolve(): EvolutionResult<TProgram>
}