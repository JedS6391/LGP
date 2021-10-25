package nz.co.jedsimson.lgp.core.evolution.operators.mutation

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program

/**
 * A search operator used during evolution to mutate an individual from a population.
 *
 * The individual is mutated in place, that is a call to [MutationOperator.mutate] will directly
 * modify the given individual.
 *
 * @param TProgram The type of programs being mutated.
 * @param TOutput The type of the program output(s).
 * @property environment The environment evolution is being performed within.
 */
abstract class MutationOperator<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    protected val environment: EnvironmentFacade<TProgram, TOutput, TTarget>
) : Module {

    /**
     * Mutates the individual given using some mutation method.
     */
    abstract fun mutate(individual: Program<TProgram, TOutput>)
}
