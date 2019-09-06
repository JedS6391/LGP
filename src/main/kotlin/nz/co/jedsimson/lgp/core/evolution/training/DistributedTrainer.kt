package nz.co.jedsimson.lgp.core.evolution.training

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Dataset
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.events.Diagnostics
import nz.co.jedsimson.lgp.core.evolution.ResultAggregator
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.evolution.model.EvolutionModel
import nz.co.jedsimson.lgp.core.evolution.model.EvolutionResult
import nz.co.jedsimson.lgp.core.evolution.model.RunBasedExportableResult
import nz.co.jedsimson.lgp.core.evolution.training.TrainingMessages.ProgressUpdate
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Represents an asynchronous distributed training operation.
 *
 * When using the built-in [DistributedTrainer] implementations in an asynchronous manner, a [DistributedTrainingJob]
 * will be returned which provides mechanisms to retrieve the [TrainingResult] and subscribe to [TrainingUpdateMessage]s
 * sent from the [DistributedTrainer].
 *
 * @param trainingUpdateChannel A channel that can be used to communicate from the trainer to subscribers.
 * @param trainers A collection of deferred training results.
 * @param models The set of models that are being trained, used in the final result.
 * @param aggregator The [ResultAggregator] used by the [DistributedTrainer].
 */
@ExperimentalCoroutinesApi
class DistributedTrainingJob<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>> internal constructor(
    private val trainingUpdateChannel: ConflatedBroadcastChannel<ProgressUpdate<TProgram, TOutput>>,
    private val trainers: List<Deferred<EvolutionResult<TProgram, TOutput>>>,
    private val models: List<EvolutionModel<TProgram, TOutput, TTarget>>,
    private val aggregator: ResultAggregator<TProgram>
) : TrainingJob<TProgram, TOutput, TTarget, ProgressUpdate<TProgram, TOutput>>() {

    private var trainingCompleted: Boolean = false

    /**
     * Retrieves the result of training.
     *
     * If training has already been completed then the function will not suspend. Otherwise,
     * the function will suspend until training is complete.
     *
     * @returns The result of the training phase(s).
     */
    override suspend fun result(): TrainingResult<TProgram, TOutput, TTarget> {
        if (trainingCompleted) {
            val results = trainers.map { trainer -> trainer.getCompleted() }

            return TrainingResult(results, this.models)
        }

        // We need to make sure that aggregator is closed once all the co-routines have completed.
        val results = this.aggregator.use {
            trainers.map { trainer -> trainer.await() }
        }

        // Further invocations don't need to wait for the training to complete.
        trainingCompleted = true

        return TrainingResult(results, this.models)
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
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class DistributedTrainer<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    model: EvolutionModel<TProgram, TOutput, TTarget>,
    val runs: Int
) : Trainer<TProgram, TOutput, TTarget, ProgressUpdate<TProgram, TOutput>>(environment, model) {

    // Construct `runs` deep copies of the models. We need deep copies so that
    // each model can have its own environment. This is necessary for providing
    // deterministic runs when using a fixed seed, otherwise there will be contention
    // issues between a single environments RNG (e.g. non-deterministic request order)
    private val models by lazy {
        (0 until runs).map {
            this.model.deepCopy()
        }.toList()
    }

    private val aggregator: ResultAggregator<TProgram> = this.environment.resultAggregator

    // We use an ExecutorService to execute the runs in different threads when training synchronously.
    private val executor = Executors.newFixedThreadPool(runs)

    /**
     * Encapsulates the training of a model so it can be executed by an executor.
     *
     * @param model An [EvolutionModel] instance that will be trained.
     * @param dataset The [Dataset] instance that will be used to train the model.
     *
     * @suppress
     */
    private class ModelTrainerTask<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
        private val run: Int,
        private val model: EvolutionModel<TProgram, TOutput, TTarget>,
        private val dataset: Dataset<TProgram, TTarget>,
        private val aggregator: ResultAggregator<TProgram>
    ) : Callable<EvolutionResult<TProgram, TOutput>> {

        /**
         * Trains the model and returns the result of the evolutionary process.
         */
        override fun call(): EvolutionResult<TProgram, TOutput> {
            // Give this trainer a unique name
            Thread.currentThread().name = "trainer-${run + 1}"

            Diagnostics.trace("DistributedTrainer:ModelTrainerTask:start-run-${run + 1}")

            val result = this.model.train(this.dataset)

            Diagnostics.trace("DistributedTrainer:ModelTrainerTask:end-run-${run + 1}")

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
    override fun train(dataset: Dataset<TProgram, TTarget>): TrainingResult<TProgram, TOutput, TTarget> {
        // Submit all tasks to the executor. Each model will have a task created for it
        // that the executor is responsible for executing.
        val futures = this.models.mapIndexed { run, model ->
            this.executor.submit(
                ModelTrainerTask(
                    run,
                    model,
                    dataset,
                    this.aggregator
                )
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

    /**
     * Asynchronously builds [runs] different models on the training set, training them in parallel.
     */
    override suspend fun trainAsync(dataset: Dataset<TProgram, TTarget>): DistributedTrainingJob<TProgram, TOutput, TTarget> {
        // This channel will be used to communicate updates to any training progress subscribers.
        val progressChannel = ConflatedBroadcastChannel<ProgressUpdate<TProgram, TOutput>>()

        // Here be dragons...
        // Once you understand how co-routines in Kotlin work this code isn't too bad to understand and actually
        // achieves the goal in a pretty straight-forward way (despite being a little bit verbose).
        // Essentially, there is `runs` number of co-routines all training their own models. When a co-routine
        // is done training, it will send a message to the actor informing it to update the progress. The
        // actor is the source-of-truth with regards to progress which stops us from locking, etc.
        // When an actor updates its internal progress, it will send a message across the progress channel
        // which will allow all subscribers to get notified of the progress update from the source-of-truth.
        val progressActor = initialiseProgressUpdateActor(progressChannel)

        // Send an initial message to make sure that the initial zero progress is broadcast to any subscribers.
        progressActor.send(
            ProgressUpdate(Double.MIN_VALUE, null)
        )

        // Fire off all of the training co-routines.
        val jobs = this.models.mapIndexed { run, model ->
            initialiseTrainingJobAsync(run, model, dataset, progressActor)
        }

        return DistributedTrainingJob(progressChannel, jobs, this.models, this.aggregator)
    }

    /**
     * Initialises a [SendChannel] for progress updates that a [TrainingProgressUpdateActor] will be listening to.
     *
     * The actor will broadcast its internal progress as a [ProgressUpdate] message on [progressChannel].
     *
     * @param progressChannel A channel for the actor to communicate with subscribers.
     */
    private fun initialiseProgressUpdateActor(
        progressChannel: ConflatedBroadcastChannel<ProgressUpdate<TProgram, TOutput>>
    ) = GlobalScope.actor<ProgressUpdate<TProgram, TOutput>> {
        with (TrainingProgressUpdateActor(this@DistributedTrainer.runs, progressChannel)) {
            consumeEach { message ->
                onReceive(message)
            }
        }
    }

    /**
     * Creates a new co-routine that will performing evolution of a single model.
     *
     * @param run The logical run this job corresponds to.
     * @param model The model to train.
     * @param dataset The training dataset.
     * @param progressActor An actor that co-ordinates training progress update messages.
     */
    private fun initialiseTrainingJobAsync(
        run: Int,
        model: EvolutionModel<TProgram, TOutput, TTarget>,
        dataset: Dataset<TProgram, TTarget>,
        progressActor: SendChannel<ProgressUpdate<TProgram, TOutput>>
    ) = GlobalScope.async {
        val task = ModelTrainerTask(run, model, dataset, this@DistributedTrainer.aggregator)

        val result = task.call()

        // We don't control the progress so the value sent to the actor is arbitrary.
        progressActor.send(
            ProgressUpdate(Double.MIN_VALUE, result)
        )

        result
    }
}

/**
 * Encapsulates the responsibility of co-ordinating updates to the training progress.
 *
 * The actor will receive messages and update its internal progress, acting as a single
 * source-of-truth for the training progress. It will also notify subscribes of the progress
 * when a progress update occurs.
 *
 * This also lifts the burden of managing progress updates from the training tasks, so they don't
 * have to worry about how many runs have been completed, etc --- they just focus on training models.
 *
 * @param totalRuns The total number of training runs (i.e. trainers).
 * @param progressChannel A channel to communicate training progress updates to subscribers on.
 */
@ExperimentalCoroutinesApi
private class TrainingProgressUpdateActor<TProgram, TOutput : Output<TProgram>>(
    private val totalRuns: Int,
    private val progressChannel: ConflatedBroadcastChannel<ProgressUpdate<TProgram, TOutput>>
) {
    private var completedTrainers = 0
    // The progress that all trainers share. Any updates should be broadcast on the progress channel.
    private var progress = 0.0

    /**
     * Processes a [ProgressUpdate] message by updating the internal progress and notifying subscribes of the update.
     *
     * @param message A message indicating a progress update.
     */
    suspend fun onReceive(message: ProgressUpdate<TProgram, TOutput>) {
        // Basically, we ignore the progress value in the message for any legitimate updates
        // and let the actor control the progress.
        this.completedTrainers = if (message.result != null) {
            this.completedTrainers + 1
        } else {
            this.completedTrainers
        }

        this.progress = (completedTrainers.toDouble() / this.totalRuns.toDouble()) * 100.0

        // Let any subscribers know about the new update.
        this.progressChannel.send(
            ProgressUpdate(progress, message.result)
        )
    }
}