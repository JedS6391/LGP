package lgp.core.evolution.training

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.ResultAggregator
import lgp.core.evolution.model.EvolutionModel
import lgp.core.evolution.model.EvolutionResult
import lgp.core.evolution.model.RunBasedExportableResult
import lgp.core.evolution.training.TrainingMessages.ProgressUpdate

/**
 * Represents an asynchronous sequential training operation.
 *
 * When using the built-in [SequentialTrainer] implementations in an asynchronous manner, a [SequentialTrainingJob]
 * will be returned which provides mechanisms to retrieve the [TrainingResult] and subscribe to [TrainingUpdateMessage]s
 * sent from the [SequentialTrainer].
 *
 * @param trainingUpdateChannel A channel that can be used to communicate from the trainer to subscribers.
 * @param training A deferred training result.
 */
class SequentialTrainingJob<TProgram> internal constructor(
    private val trainingUpdateChannel: ConflatedBroadcastChannel<ProgressUpdate<TProgram>>,
    private val training: Deferred<TrainingResult<TProgram>>
) : TrainingJob<TProgram, ProgressUpdate<TProgram>>() {

    /**
     * Retrieves the result of training.
     *
     * If the job has already been completed then the method will not suspend. Otherwise,
     * the method will suspend until training is complete.
     *
     * @returns The result of the training phase(s).
     */
    override suspend fun result(): TrainingResult<TProgram> {
        // Don't need to block if the job is complete already.
        if (training.isCompleted) {
            return training.getCompleted()
        }

        return training.await()
    }

    /**
     * Subscribes a [callback] function that will be executed each time a [TrainingUpdateMessage] is received.
     *
     * The callback will be passed the message and allow the subscriber to use the value as it wishes.
     *
     * @param callback The function to execute when a [TrainingUpdateMessage] is received.
     */
    override fun subscribeToUpdates(callback: (ProgressUpdate<TProgram>) -> Unit) {
        val subscription = trainingUpdateChannel.openSubscription()

        GlobalScope.launch {
            subscription.consumeEach(callback)
        }
    }
}

/**
 * Trains the model for a given number of runs, in a sequential manner.
 *
 * @property runs The number of times to train the given model.
 */
class SequentialTrainer<TProgram>(
    environment: Environment<TProgram>,
    model: EvolutionModel<TProgram>,
    val runs: Int
) : Trainer<TProgram, ProgressUpdate<TProgram>>(environment, model) {

    private val models = (0 until runs).map {
        // Create `runs` untrained models.
        this.model.copy()
    }

    private val aggregator: ResultAggregator<TProgram> = this.environment.resultAggregator

    /**
     * Builds [runs] different models on the training set.
     *
     * **Note:** This function will block until the training is complete.
     * To training in a non-blocking fashion, use the [trainAsync] function.
     */
    override fun train(dataset: Dataset<TProgram>): TrainingResult<TProgram> {

        val results = aggregator.use {
            this.models.mapIndexed { run, model ->
                val result = model.train(dataset)
                this.aggregateResults(run, result)

                result
            }
        }

        return TrainingResult(results, this.models)
    }

    /**
     * Asynchronously builds [runs] different models on the training set.
     *
     * The general flow is:
     * 1. Call [trainAsync] to get a [TrainingJob]
     * 2. Optionally subscribe to training progress updates using [TrainingJob.subscribeToUpdates]
     * 3. Wait for the training to complete using [TrainingJob.result]
     *
     * This implementation will still run each training task sequentially, but it allows the training
     * execution to be suspended so that other tasks can be performed.
     */
    override suspend fun trainAsync(dataset: Dataset<TProgram>) : SequentialTrainingJob<TProgram> {
        // This channel will be used to communicate updates to any training progress subscribers.
        val progressChannel = ConflatedBroadcastChannel<ProgressUpdate<TProgram>>()

        // Our worker co-routine will perform the training and return a result when it is complete.
        // As it makes progress, updates will be sent through the channel to any subscribers.
        val job = GlobalScope.async {
            // Progress is from 0-100.
            progressChannel.send(
                ProgressUpdate(0.0, null)
            )

            val results = aggregator.use {
                this@SequentialTrainer.models.mapIndexed { run, model ->
                    val result = model.train(dataset)
                    this@SequentialTrainer.aggregateResults(run, result)

                    val progress = ((run + 1).toDouble() / runs.toDouble()) * 100.0

                    progressChannel.send(
                        ProgressUpdate(progress, result)
                    )

                    result
                }
            }

            TrainingResult(results, this@SequentialTrainer.models)
        }

        return SequentialTrainingJob(progressChannel, job)
    }

    private fun aggregateResults(run: Int, result: EvolutionResult<TProgram>) {
        val generationalResults = result.statistics.map { generation ->
            RunBasedExportableResult<TProgram>(run, generation)
        }

        this.aggregator.addAll(generationalResults)
    }
}