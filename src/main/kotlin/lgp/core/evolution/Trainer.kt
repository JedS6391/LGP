package lgp.core.evolution

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.model.EvolutionModel
import lgp.core.evolution.model.EvolutionResult
import lgp.core.evolution.model.RunBasedExportableResult
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Represents the result of training a model using a runner.
 *
 * @property evaluations A collection of results from evolution.
 */
data class TrainingResult<T>(val evaluations: List<EvolutionResult<T>>, val models: List<EvolutionModel<T>>)

/**
 * Represents an asynchronous training operation.
 *
 * When using the built-in trainers in an asynchronous manner, a [TrainingJob] will be returned
 * which provides access to the underlying result and a communication channel for training progress.
 */
class TrainingJob<T> internal constructor(
    val trainingProgress: ConflatedBroadcastChannel<TrainingProgressMessage.ProgressUpdate>,
    private val training: Deferred<TrainingResult<T>>
) {
    /**
     * Retrieves the result of training.
     *
     * If the job has already been completed then the method will not block. Otherwise,
     * the method will block until training is complete.
     *
     * @returns The result of the training phase(s).
     */
    suspend fun result(): TrainingResult<T> {
        // Don't need to block if the job is complete already.
        if (training.isCompleted) {
            return training.getCompleted()
        }

        return training.await()
    }

    /**
     * Subscribes a [callback] function that will be executed each time the training progress is updated.
     *
     * The callback will be passed the current training progress and allow the subscriber
     * to use that progress value how it requires.
     *
     * @param callback The function to execute when a training progress update is received.
     */
    fun subscribeToProgress(callback: (TrainingProgressMessage.ProgressUpdate) -> Unit) {
        val subscription = trainingProgress.openSubscription()

        GlobalScope.launch {
            subscription.consumeEach(callback)
        }
    }
}

sealed class TrainingProgressMessage {
    /**
     * Represents a training progress update.
     */
    class ProgressUpdate(val progress: Double) : TrainingProgressMessage()
}

/*
private fun trainingProgressActor() = GlobalScope.actor<TrainingProgressMessage> {
    var progress = 0.0

    for (msg in channel) {
        when (msg) {
            is TrainingProgressMessage.ProgressUpdate -> {
                progress = msg.progress
            }
            is TrainingProgressMessage.ProgressRequest -> {
                msg.response.send(progress)
            }
        }
    }
}
*/

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

    abstract suspend fun trainAsync(dataset: Dataset<T>): TrainingJob<T>
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
    class SequentialTrainer<T>(
        environment: Environment<T>,
        model: EvolutionModel<T>,
        val runs: Int
    ) : Trainer<T>(environment, model) {

        private val models = (0 until runs).map {
            // Create `runs` untrained models.
            this.model.copy()
        }

        private val aggregator: ResultAggregator<T> = this.environment.resultAggregator

        /**
         * Builds [runs] different models on the training set.
         */
        override fun train(dataset: Dataset<T>): TrainingResult<T> {

            val results = aggregator.use {
                this.models.mapIndexed { run, model ->
                    val result = model.train(dataset)
                    this.aggregateResults(run, result)

                    result
                }
            }

            return TrainingResult(results, this.models)
        }

        override suspend fun trainAsync(dataset: Dataset<T>) : TrainingJob<T> {
            val progressChannel = ConflatedBroadcastChannel<TrainingProgressMessage.ProgressUpdate>()

            val job = GlobalScope.async {
                // Progress is from 0-100.
                progressChannel.send(TrainingProgressMessage.ProgressUpdate(0.0))

                val results = aggregator.use {
                    this@SequentialTrainer.models.mapIndexed { run, model ->
                        val result = model.train(dataset)
                        this@SequentialTrainer.aggregateResults(run, result)

                        val progress = ((run + 1).toDouble() / runs.toDouble()) * 100.0

                        progressChannel.send(
                            TrainingProgressMessage.ProgressUpdate(progress)
                        )

                        result
                    }
                }

                TrainingResult(results, this@SequentialTrainer.models)
            }

            return TrainingJob(progressChannel, job)
        }

        private fun aggregateResults(run: Int, result: EvolutionResult<T>) {
            val generationalResults = result.statistics.map { generation ->
                RunBasedExportableResult<T>(run, generation)
            }

            this.aggregator.addAll(generationalResults)
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
    class DistributedTrainer<T>(
        environment: Environment<T>,
        model: EvolutionModel<T>,
        val runs: Int
    ) : Trainer<T>(environment, model) {

        // Construct `runs` deep copies of the models. We need deep copies so that
        // each model can have its own environment. This is necessary for providing
        // deterministic runs when using a fixed seed, otherwise there will be contention
        // issues between a single environments RNG (e.g. non-deterministic request order)
        private val models = (0 until runs).map {
            this.model.deepCopy()
        }.toList()

        private val aggregator: ResultAggregator<T> = this.environment.resultAggregator

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
            private val run: Int,
            private val model: EvolutionModel<TProgram>,
            private val dataset: Dataset<TProgram>,
            private val aggregator: ResultAggregator<TProgram>
        ) : Callable<EvolutionResult<TProgram>> {

            /**
             * Trains the model and returns the result of the evolutionary process.
             */
            override fun call(): EvolutionResult<TProgram> {
                val result = this.model.train(this.dataset)

                // Aggregate results for this thread.
                val generationalResults = result.statistics.map { generation ->
                    RunBasedExportableResult<TProgram>(this.run, generation)
                }

                this.aggregator.addAll(generationalResults)

                return result
            }
        }

        /**
         * Builds [runs] different models on the training set, training them in parallel.
         */
        override fun train(dataset: Dataset<T>): TrainingResult<T> {
            // Submit all tasks to the executor. Each model will have a task created for it
            // that the executor is responsible for executing.
            val futures = this.models.mapIndexed { run, model ->
                this.executor.submit(
                    ModelTrainerTask(run, model, dataset, this.aggregator)
                )
            }

            // Collect the results -- waiting when necessary.
            val results = this.aggregator.use {
                futures.map { future ->
                    future.get()
                }
            }

            // We're done so we can shut the executor down.
            this.executor.shutdown()

            return TrainingResult(results, this.models)
        }

        override suspend fun trainAsync(dataset: Dataset<T>): TrainingJob<T> {
            // TODO: Provide an async distributed training method.
            throw NotImplementedError()
        }
    }
}