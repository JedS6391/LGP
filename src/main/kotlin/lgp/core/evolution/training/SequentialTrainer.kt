package lgp.core.evolution.training

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.ResultAggregator
import lgp.core.evolution.model.EvolutionModel
import lgp.core.evolution.model.EvolutionResult
import lgp.core.evolution.model.RunBasedExportableResult
import lgp.core.evolution.training.TrainingMessages.ProgressUpdate

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
     */
    override suspend fun trainAsync(dataset: Dataset<TProgram>) : TrainingJob<TProgram, ProgressUpdate<TProgram>> {
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

        return TrainingJob(progressChannel, job)
    }

    private fun aggregateResults(run: Int, result: EvolutionResult<TProgram>) {
        val generationalResults = result.statistics.map { generation ->
            RunBasedExportableResult<TProgram>(run, generation)
        }

        this.aggregator.addAll(generationalResults)
    }
}