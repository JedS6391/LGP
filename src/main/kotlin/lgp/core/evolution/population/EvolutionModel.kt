package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.modules.Module

/**
 * A container for any statistics from evolution.
 *
 * The interface simply maps a string (some description of the statistic) to a value of any type.
 *
 * @property data A mapping of a description to some statistic.
 */
data class EvolutionStatistics(val data: Map<String, Any>)

/**
 * The best individual and final population from the result of evolution.
 *
 * @property best The best program at the end of the evolution process.
 * @property individuals The population at the end of the evolution process.
 * @property statistics Any statistics from evolution. It is expected to be on a per generation basis.
 */
data class EvolutionResult<T>(
        val best: Program<T>,
        val individuals: List<Program<T>>,
        val statistics: List<EvolutionStatistics>
)

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

    abstract fun copy(): EvolutionModel<TProgram>
}