package nz.co.jedsimson.lgp.core.evolution.fitness

import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Outputs

/**
 * Provides the functionality to compute the fitness of an individual program on a set of input-output examples.
 *
 * The fitness value should always be a simple double but the way in which the value
 * is determined can be customised depending on the type of program/fitness cases.
 *
 * A fitness function is really just a function that maps a set of program outputs
 * with a set of examples in some way. It is encapsulated in a class to make the interaction
 * with it slightly nicer and more straightforward (especially when used from Java).
 *
 * An implementation of the class can be directly queried for fitness using the `()` operator.
 */
abstract class FitnessFunction<TData, TOutput : Output<TData>, TTarget : Target<TData>> {

    /**
     * Computes the fitness based on the given [outputs] and [cases].
     *
     * @param outputs A set of predicted program outputs.
     * @param cases A set of expected outputs.
     * @return A double value that represents the error measure between the predicted/expected outputs.
     */
    abstract fun fitness(outputs: List<TOutput>, cases: List<FitnessCase<TData, TTarget>>): Double

    /**
     * Allows [fitness] to be called directly using `()` syntax (e.g. `fitnessFunctionInstance(outputs, cases)`).
     */
    operator fun invoke(outputs: List<TOutput>, cases: List<FitnessCase<TData, TTarget>>): Double {
        return this.fitness(outputs, cases)
    }
}

/**
 * A function that provides a [FitnessFunction] implementation on request.
 */
typealias FitnessFunctionProvider<TData, TOutput, TTarget> = () -> FitnessFunction<TData, TOutput, TTarget>

/**
 * A [FitnessFunction] for [Program]s with a single output.
 */
typealias SingleOutputFitnessFunction<TData> = FitnessFunction<TData, Outputs.Single<TData>, Targets.Single<TData>>

/**
 * A [FitnessFunction] for [Program]s with multiple outputs.
 */
typealias MultipleOutputFitnessFunction<TData> = FitnessFunction<TData, Outputs.Multiple<TData>, Targets.Multiple<TData>>