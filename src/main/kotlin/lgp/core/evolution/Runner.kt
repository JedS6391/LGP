package lgp.core.evolution

import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.population.EvolutionModel
import lgp.core.evolution.population.EvolutionResult
import lgp.core.evolution.population.pmap

/**
 * Represents the result of running evolution.
 *
 * @property evaluations A collection of results from evolution.
 */
data class RunResult<T>(val evaluations: List<EvolutionResult<T>>)

/**
 * A service capable of running evolution with a particular model in a particular environment.
 *
 * @param T The type of programs being evolved.
 * @property environment The environment evolution is performed within.
 * @property model The model of evolution to use.
 */
abstract class Runner<T>(val environment: Environment<T>, val model: EvolutionModel<T>) {
    // TODO: Better name for this class.

    /**
     * Runs the model and gathers results.
     *
     * @returns The results of the run(s).
     */
    abstract fun run(dataset: Dataset<T>): RunResult<T>

    // TODO: Should probably provide ability to run training/testing phases.
}

/**
 * A collection of built-in evolution runners.
 */
object Runners {

    /**
     * Runs the model for a given number of runs, in a sequential manner.
     *
     * @property runs The number of times to run the given model.
     */
    class SequentialRunner<T>(environment: Environment<T>, model: EvolutionModel<T>, val runs: Int)
        : Runner<T>(environment, model) {

        /**
         * Runs the model [runs] times.
         */
        override fun run(dataset: Dataset<T>): RunResult<T> {

            val results = (0..runs - 1).toList().map {
                this.model.train(dataset)
            }

            return RunResult(results)
        }
    }

    /**
     *  Runs the model for a given number of runs, in a parallel manner.
     *
     *  The model will be copied [runs] time and each copy will be executed in
     *  its own thread. The results of each model evaluation will be gathered
     *  once all the threads complete.
     *
     *  This runner has significant overhead compared to the [SequentialRunner], due
     *  to the extra memory needed for evaluating multiple models at once, as well as
     *  the overhead of the threads used.
     *
     *  @property runs The number of times to run the given model.
     */
    class DistributedRunner<T>(environment: Environment<T>, model: EvolutionModel<T>, val runs: Int)
        : Runner<T>(environment, model) {

        override fun run(dataset: Dataset<T>): RunResult<T> {
            // TODO: Might be worth making this more robust with regards to failure.
            val results = (0..runs - 1).toList().pmap {
                this.model.copy().train(dataset)
            }

            return RunResult(results)
        }
    }

}