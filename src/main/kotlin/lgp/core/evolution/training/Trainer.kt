package lgp.core.evolution.training

import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.fitness.Output
import lgp.core.evolution.model.EvolutionModel
import lgp.core.evolution.model.EvolutionResult

/**
 * Represents the result of training a model using a runner.
 *
 * @property evaluations A collection of results from evolution.
 */
data class TrainingResult<TProgram, TOutput : Output<TProgram>>(
    val evaluations: List<EvolutionResult<TProgram, TOutput>>,
    val models: List<EvolutionModel<TProgram, TOutput>>
)

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
    class ProgressUpdate<TProgram, TOutput : Output<TProgram>>(
        /**
         * The current training progress, from 0% to 100%.
         */
        val progress: Double,

        /**
         * The most recent [EvolutionResult] created by the training process.
         *
         * Will be null when the update does not correspond to the completion of training a model.
         */
        val result: EvolutionResult<TProgram, TOutput>?
    ) : TrainingUpdateMessage
}

/**
 * Represents an asynchronous training operation.
 *
 * As different [Trainer] implementations will have different requirements, it is up to the
 * implementor to define how the job should handle various requests.
 *
 * @param TProgram The type of program being evolved.
 * @Param TMessage
 */
abstract class TrainingJob<TProgram, TOutput : Output<TProgram>, TMessage : TrainingUpdateMessage> {

    /**
     * Retrieves the result of training.
     *
     * @returns The result of the training phase(s).
     */
    abstract suspend fun result(): TrainingResult<TProgram, TOutput>

    /**
     * Subscribes a [callback] function that will be executed each time a [TrainingUpdateMessage] is received.
     *
     * The callback will be passed the message and allow the subscriber to use the value as it wishes.
     *
     * **Note:** If the [Trainer] implementation does not provide a communication channel then this
     * function can be left as not implemented.
     *
     * @param callback The function to execute when a [TrainingUpdateMessage] is received.
     */
    abstract fun subscribeToUpdates(callback: (TMessage) -> Unit)
}

/**
 * A service capable of training evolutionary models in a particular environment.
 *
 * @param TProgram The type of programs being evolved.
 * @property environment The environment evolution is performed within.
 * @property model The model of evolution to use.
 */
abstract class Trainer<TProgram, TOutput : Output<TProgram>, TMessage : TrainingUpdateMessage>(
    val environment: Environment<TProgram, TOutput>,
    val model: EvolutionModel<TProgram, TOutput>
) {

    /**
     * Trains the model and gathers results from training.
     *
     * @returns The results of the training phase(s).
     */
    abstract fun train(dataset: Dataset<TProgram>): TrainingResult<TProgram, TOutput>

    /**
     * Asynchronously trains the model and gathers results from training.
     *
     * @returns The results of the training phase(s).
     */
    abstract suspend fun trainAsync(dataset: Dataset<TProgram>): TrainingJob<TProgram, TOutput, TMessage>
}
