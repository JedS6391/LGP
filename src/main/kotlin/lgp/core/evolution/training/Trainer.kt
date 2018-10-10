package lgp.core.evolution.training

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.model.EvolutionModel
import lgp.core.evolution.model.EvolutionResult

/**
 * Represents the result of training a model using a runner.
 *
 * @property evaluations A collection of results from evolution.
 */
data class TrainingResult<T>(val evaluations: List<EvolutionResult<T>>, val models: List<EvolutionModel<T>>)

/**
 * A message sent from a [Trainer] to a subscriber (by calling [TrainingJob.subscribeToUpdates].
 */
interface TrainingUpdateMessage

/**
 * Contains the different messages that can be sent from a [Trainer] when training asynchronously.
 */
object TrainingMessages {
    /**
     * Represents a training progress update.
     */
    class ProgressUpdate(val progress: Double) : TrainingUpdateMessage
}

/**
 * Represents an asynchronous training operation.
 *
 * When using the built-in [Trainer] implementations in an asynchronous manner, a [TrainingJob] will be returned
 * which provides mechanisms to retrieve the [TrainingResult] and subscribe to [TrainingUpdateMessage]s sent
 * from the [Trainer].
 */
class TrainingJob<TProgram, TMessage : TrainingUpdateMessage> internal constructor(
    private val trainingUpdateChannel: ConflatedBroadcastChannel<TMessage>,
    private val training: Deferred<TrainingResult<TProgram>>
) {
    /**
     * Retrieves the result of training.
     *
     * If the job has already been completed then the method will not suspend. Otherwise,
     * the method will suspend until training is complete.
     *
     * @returns The result of the training phase(s).
     */
    suspend fun result(): TrainingResult<TProgram> {
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
    fun subscribeToUpdates(callback: (TMessage) -> Unit) {
        val subscription = trainingUpdateChannel.openSubscription()

        GlobalScope.launch {
            subscription.consumeEach(callback)
        }
    }
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
 * @param TProgram The type of programs being evolved.
 * @property environment The environment evolution is performed within.
 * @property model The model of evolution to use.
 */
abstract class Trainer<TProgram, TMessage : TrainingUpdateMessage>(
    val environment: Environment<TProgram>,
    val model: EvolutionModel<TProgram>
) {

    /**
     * Trains the model and gathers results from training.
     *
     * @returns The results of the training phase(s).
     */
    abstract fun train(dataset: Dataset<TProgram>): TrainingResult<TProgram>

    abstract suspend fun trainAsync(dataset: Dataset<TProgram>): TrainingJob<TProgram, TMessage>
}
