package nz.co.jedsimson.lgp.core.evolution.operators.selection

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.program.Program

/**
 * A search operator used during evolution to select a subset of individuals from a population.
 *
 * The subset of individuals determined by the search operator are used when applying the
 * [RecombinationOperator] and [MutationOperator] to move through the search space of LGP
 * programs for the problem being solved.
 *
 * Generally, it is expected that that the individuals selected will be removed from the original
 * population, and clones of those individuals returned. These individuals can then be subjected
 * to recombination/mutation before being introduced back into a population.
 *
 * @param TProgram The type of programs being selected.
 * @param TOutput The type of the program output(s).
 * @property environment The environment evolution is being performed within.
 */
abstract class SelectionOperator<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    protected val environment: EnvironmentFacade<TProgram, TOutput, TTarget>
) : Module {

    /**
     * Selects a subset of programs from the population given using some method of selection.
     *
     * @param population A collection of program individuals.
     * @return A subset of the population given.
     */
    abstract fun select(population: MutableList<Program<TProgram, TOutput>>): List<Program<TProgram, TOutput>>
}