package nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.events.Diagnostics
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.MutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategyFactory
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategy
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import java.lang.IllegalArgumentException

/**
 * A [MutationOperator] implementation that performs effective macro mutations.
 *
 * For more information, see Algorithm 6.1 from Linear Genetic Programming (Brameier, M., Banzhaf, W. 2001).
 *
 * Note that [insertionRate] + [deletionRate] should be equal to 1.
 *
 * @param environment An environment that the mutation is occurring in.
 * @property insertionRate The probability with which instructions should be inserted.
 * @property deletionRate The probability with which instructions should be deleted.
 * @property mutationStrategyFactory A factory that can be used to get a [MutationStrategy] to delegate the mutation to.
 * @constructor Creates a new [MacroMutationOperator] with the given [environment], [insertionRate], [deletionRate], and [mutationStrategyFactory].
 */
class MacroMutationOperator<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    private val insertionRate: Double,      // p_ins
    private val deletionRate: Double,       // p_del
    private val mutationStrategyFactory: MutationStrategyFactory<TProgram, TOutput, TTarget>
) : MutationOperator<TProgram, TOutput, TTarget>(environment) {

    init {
        if ((insertionRate + deletionRate) != 1.0) {
            throw IllegalArgumentException(
                "insertion rate + deletion rate must be equal to 1.0 (was ${insertionRate + deletionRate})"
            )
        }
    }

    /**
     * Creates a new [MacroMutationOperator] with the given [environment], [insertionRate], and [deletionRate].
     *
     * The [MacroMutationOperator] will use the default mutation strategy factory ([MacroMutationStrategyFactory]).
     */
    constructor(
        environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
        insertionRate: Double,
        deletionRate: Double
    ) : this(
        environment,
        insertionRate,
        deletionRate,
        MacroMutationStrategyFactory(environment, insertionRate, deletionRate)
    )

    /**
     * Performs a single, effective macro mutation to the individual given.
     */
    override fun mutate(individual: Program<TProgram, TOutput>) {
        // Make sure the individuals effective program is found before mutating, since
        // we need it to perform effective mutations.
        Diagnostics.traceWithTime("MacroMutationOperator:find-effective-program") {
            individual.findEffectiveProgram()
        }

        val mutationStrategy = this.mutationStrategyFactory.getStrategyForIndividual(individual)

        Diagnostics.traceWithTime("MacroMutationOperator:mutation-strategy-execution") {
            mutationStrategy.mutate(individual)
        }
    }

    override val information = ModuleInformation("Algorithm 6.1 ((effective) instruction mutation).")
}