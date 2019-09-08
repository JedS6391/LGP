package nz.co.jedsimson.lgp.core.evolution.training

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Dataset
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.evolution.ResultAggregator
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.evolution.model.EvolutionModel
import nz.co.jedsimson.lgp.core.evolution.model.EvolutionResult
import nz.co.jedsimson.lgp.core.evolution.model.RunBasedExportableResult
import nz.co.jedsimson.lgp.core.evolution.training.TrainingMessages.ProgressUpdate

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
@ExperimentalCoroutinesApi
class SequentialTrainingJob<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>> internal constructor(
    private val trainingUpdateChannel: ConflatedBroadcastChannel<ProgressUpdate<TProgram, TOutput>>,
    private val training: Deferred<TrainingResult<TProgram, TOutput, TTarget>>
) : TrainingJob<TProgram, TOutput, TTarget, ProgressUpdate<TProgram, TOutput>>() {

    /**
     * Retrieves the result of training.
     *
     * If the job has already been completed then the function will not suspend. Otherwise,
     * the function will suspend until training is complete.
     *
     * @returns The result of the training phase(s).
     */
    override suspend fun result(): TrainingResult<TProgram, TOutput, TTarget> {
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
    override fun subscribeToUpdates(callback: (ProgressUpdate<TProgram, TOutput>) -> Unit) {
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
@ExperimentalCoroutinesApi
class SequentialTrainer<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    model: EvolutionModel<TProgram, TOutput, TTarget>,
    val runs: Int
) : Trainer<TProgram, TOutput, TTarget, ProgressUpdate<TProgram, TOutput>>(environment, model) {

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
    override fun train(dataset: Dataset<TProgram, TTarget>): TrainingResult<TProgram, TOutput, TTarget> {

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
     * 3. Perform other tasks unrelated to the training.
     * 4. Wait for the training to complete using [TrainingJob.result]
     *
     * This implementation will still run each training task sequentially, but it allows the training
     * execution to be suspended so that other tasks can be performed.
     */
    override suspend fun trainAsync(dataset: Dataset<TProgram, TTarget>) : SequentialTrainingJob<TProgram, TOutput, TTarget> {
        // This channel will be used to communicate updates to any training progress subscribers.
        val progressChannel = ConflatedBroadcastChannel<ProgressUpdate<TProgram, TOutput>>()

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

    private fun aggregateResults(run: Int, result: EvolutionResult<TProgram, TOutput>) {
        val generationalResults = result.statistics.map { generation ->
            RunBasedExportableResult<TProgram>(run, generation)
        }

        this.aggregator.addAll(generationalResults)
    }
}