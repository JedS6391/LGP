package lgp.core.evolution

import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.population.EvolutionModel
import lgp.core.evolution.population.EvolutionResult
import java.util.concurrent.Callable
import java.util.concurrent.Executors

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
 * A collection of built-in evolution trainers.
 */
object Trainers {

    /**
     * Trains the model for a given number of runs, in a sequential manner.
     *
     * @property runs The number of times to train the given model.
     */
    class SequentialTrainer<T>(environment: Environment<T>, model: EvolutionModel<T>, val runs: Int)
        : Trainer<T>(environment, model) {

        private val models = (0 until runs).map {
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
     *  Trains the model for a given number of runs, in a parallel manner.
     *
     *  The model will be copied [runs] time and each copy will be trained in
     *  its own thread. The results of each model evaluation will be gathered
     *  once all the threads complete.
     *
     *  This runner has significant overhead compared to the [SequentialTrainer], due
     *  to the extra memory needed for evaluating multiple models at once, as well as
     *  the overhead of the threads used.
     *
     *  It should be noted that this trainer will create [runs] copies of the environment --
     *  one for each model. This allows each model to have its own environment which can be
     *  isolated within the thread it is executed. This is mainly done to facilitate deterministic
     *  multi-threaded training, where it is necessary to make 100% sure that any state shared between
     *  threads does not affect the outcome of the training.
     *
     *  @property runs The number of times to train the given model.
     */
    class DistributedTrainer<T>(environment: Environment<T>, model: EvolutionModel<T>, val runs: Int)
        : Trainer<T>(environment, model) {

        // Construct `runs` deep copies of the models. We need deep copies so that
        // each model can have its own environment. This is necessary for providing
        // deterministic runs when using a fixed seed, otherwise there will be contention
        // issues between a single environments RNG (e.g. non-deterministic request order)
        private val models = (0 until runs).map {
            this.model.deepCopy()
        }.toList()

        // We use an ExecutorService to execute the runs in different threads.
        private val executor = Executors.newFixedThreadPool(runs)

        /**
         * Encapsulates the training of a model so it can be executed by an [ExecutorService] implementation.
         *
         * @param model An [EvolutionModel] instance that will be trained.
         * @param dataset The [Dataset] instance that will be used to train the model.
         *
         * @suppress
         */
        class ModelTrainerTask<TProgram>(
                private val model: EvolutionModel<TProgram>,
                private val dataset: Dataset<TProgram>
        ) : Callable<EvolutionResult<TProgram>> {

            /**
             * Trains the model and returns the result of the evolutionary process.
             */
            override fun call(): EvolutionResult<TProgram> {
                return this.model.train(this.dataset)
            }
        }

        /**
         * Builds [runs] different models on the training set, training them in parallel.
         */
        override fun train(dataset: Dataset<T>): TrainingResult<T> {
            // Submit all tasks to the executor. Each model will have a task created for it
            // that the executor is responsible for executing.
            val futures = this.models.map { model ->
                this.executor.submit(ModelTrainerTask(model, dataset))
            }

            // Collect the results -- waiting when necessary.
            val results = futures.map { future ->
                future.get()
            }

            // We're done so we can shut the executor down.
            this.executor.shutdown()

            return TrainingResult(results, this.models)
        }
    }
}