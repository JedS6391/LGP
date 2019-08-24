package nz.co.jedsimson.lgp.core.evolution.operators.recombination

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.program.Program

/**
 * A search operator used during evolution to combine two individuals from a population.
 *
 * The individuals are mutated directly by the reference given, so calls to this function
 * directly modify the arguments.
 *
 * @param TProgram The type of programs being combined.
 * @param TOutput The type of the program output(s).
 * @property environment The environment evolution is being performed within.
 */
abstract class RecombinationOperator<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    protected val environment: EnvironmentFacade<TProgram, TOutput, TTarget>
) : Module {

    /**
     * Combines the two programs given using some recombination technique.
     *
     * @param mother The first individual.
     * @param father The second individual.
     */
    abstract fun combine(mother: Program<TProgram, TOutput>, father: Program<TProgram, TOutput>)
}