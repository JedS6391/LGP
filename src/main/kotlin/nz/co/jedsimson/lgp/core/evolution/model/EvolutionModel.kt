package nz.co.jedsimson.lgp.core.evolution.model

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Dataset
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.modules.Module

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
open class EvolutionResult<TProgram, TOutput : Output<TProgram>>(
    val best: Program<TProgram, TOutput>,
    val individuals: List<Program<TProgram, TOutput>>,
    val statistics: List<EvolutionStatistics>
)

/**
 * A result given when calling [EvolutionModel.test].
 *
 * @property predicted The outputs of the program trained by the model.
 * @property expected The expected outputs as defined by a [Dataset].
 */
data class TestResult<TProgram, TOutput : Output<TProgram>>(
    val predicted: List<TOutput>,
    val expected: List<Target<TProgram>>
)

/**
 * A model that can be used to perform evolution within in a specific environment.
 *
 * @param TProgram The type of programs this models evolves.
 * @param TOutput The type of the output for programs this models evolves.
 * @param TTarget The type of the targets in the data set.
 * @property environment The environment evolution takes place within.
 */
abstract class EvolutionModel<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    protected val environment: EnvironmentFacade<TProgram, TOutput, TTarget>
) : Module {

    /**
     * Train the model on the given data set.
     *
     * A result is given which describes the process of evolution during training.
     *
     * @param dataset A set of features and target values.
     * @returns A description of the evolution process during training.
     */
    abstract fun train(dataset: Dataset<TProgram, TTarget>): EvolutionResult<TProgram, TOutput>

    /**
     * Tests the model on a given data set and returns the program outputs.
     *
     * @param dataset A set of features and target values.
     * @returns The output of the program for each sample in the data set.
     */
    abstract fun test(dataset: Dataset<TProgram, TTarget>): TestResult<TProgram, TOutput>

    /**
     * Produces a copy of this [EvolutionModel].
     *
     * The copy will use the same [EnvironmentFacade] as this [EvolutionModel].
     */
    abstract fun copy(): EvolutionModel<TProgram, TOutput, TTarget>

    /**
     * Produces a copy of this [EvolutionModel].
     *
     * The copy should also have its own copy of the [EnvironmentFacade].
     */
    abstract fun deepCopy(): EvolutionModel<TProgram, TOutput, TTarget>
}
