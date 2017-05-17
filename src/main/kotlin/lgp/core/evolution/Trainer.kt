package lgp.core.evolution

import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.population.EvolutionModel
import lgp.core.evolution.population.EvolutionResult
import lgp.core.evolution.population.pmap

/**
 * Represents the result of training a model using a runner.
 *
 * @property evaluations A collection of results from evolution.
 */
data class TrainingResult<T>(val evaluations: List<EvolutionResult<T>>, val models: List<EvolutionModel<T>>)

/**
 * A service capable of training evolutionary models in a particular environment.
 *
 * @param T The type of programs being evolved.
 * @property environment The environment evolution is performed within.
 * @property model The model of evolution to use.
 */
abstract class Trainer<T>(val environment: Environment<T>, val model: EvolutionModel<T>) {

    /**
     * Trains the model and gathers results from training.
     *
     * @returns The results of the training phase(s).
     */
    abstract fun train(dataset: Dataset<T>): TrainingResult<T>
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
    class SequentialTrainer<T>(environment: Environment<T>, model: EvolutionModel<T>, val runs: Int)
        : Trainer<T>(environment, model) {

        val models = (0..runs - 1).map {
            // Create `runs` untrained models.
            this.model.copy()
        }

        /**
         * Builds [runs] different models on the training set.
         */
        override fun train(dataset: Dataset<T>): TrainingResult<T> {

            val results = this.models.map { model ->
                model.train(dataset)
            }

            return TrainingResult(results, this.models)
        }
    }

    /**
     *  Runs the model for a given number of runs, in a parallel manner.
     *
     *  The model will be copied [runs] time and each copy will be executed in
     *  its own thread. The results of each model evaluation will be gathered
     *  once all the threads complete.
     *
     *  This runner has significant overhead compared to the [SequentialTrainer], due
     *  to the extra memory needed for evaluating multiple models at once, as well as
     *  the overhead of the threads used.
     *
     *  @property runs The number of times to run the given model.
     */
    class DistributedTrainer<T>(environment: Environment<T>, model: EvolutionModel<T>, val runs: Int)
        : Trainer<T>(environment, model) {

        val models = (0..runs - 1).map {
            this.model.copy()
        }

        override fun train(dataset: Dataset<T>): TrainingResult<T> {
            // TODO: Might be worth making this more robust with regards to failure.
            val results = this.models.pmap { model ->
                model.train(dataset)
            }

            return TrainingResult(results, this.models)
        }
    }

}