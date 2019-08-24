package nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy

import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program

/**
 * Responsible for creating [MutationStrategy] instances to be used to perform macro-mutations.*
 */
abstract class MutationStrategyFactory<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>> {

    /**
     * Gets the [MutationStrategy] that should be applied to the given [Program].
     *
     * @param individual A [Program] that the mutation will be applied to.
     */
    abstract fun getStrategyForIndividual(
        individual: Program<TProgram, TOutput>
    ): MutationStrategy<TProgram, TOutput, TTarget>
}