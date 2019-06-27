package nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program

/**
 * Defines a strategy that can be used to perform a mutation on a [Program].
 */
abstract class MutationStrategy<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    protected val environment: EnvironmentFacade<TProgram, TOutput, TTarget>
) {

    /**
     * Mutates the individual given using some mutation method.
     *
     * @param individual A [Program] to mutate.
     */
    abstract fun mutate(individual: Program<TProgram, TOutput>)
}