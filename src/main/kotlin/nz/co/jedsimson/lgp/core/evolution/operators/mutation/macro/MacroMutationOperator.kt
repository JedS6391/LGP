package nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro

import nz.co.jedsimson.lgp.core.environment.Environment
import nz.co.jedsimson.lgp.core.environment.EnvironmentDefinition
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.MutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategyFactory
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program

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
 */
class MacroMutationOperator<TProgram, TOutput : Output<TProgram>>(
    environment: EnvironmentDefinition<TProgram, TOutput>,
    private val insertionRate: Double,      // p_ins
    private val deletionRate: Double        // p_del
) : MutationOperator<TProgram, TOutput>(environment) {

    private val mutationStrategyFactory = MacroMutationStrategyFactory(
        this.environment,
        this.insertionRate,
        this.deletionRate
    )

    init {
        // Give a nasty runtime message if we get invalid parameters.
        assert((insertionRate + deletionRate) == 1.0)
    }

    /**
     * Performs a single, effective macro mutation to the individual given.
     */
    override fun mutate(individual: Program<TProgram, TOutput>) {
        // Make sure the individuals effective program is found before mutating, since
        // we need it to perform effective mutations.
        individual.findEffectiveProgram()

        val mutationStrategy = this.mutationStrategyFactory.getStrategyForIndividual(individual)

        mutationStrategy.mutate(individual)
    }

    override val information = ModuleInformation("Algorithm 6.1 ((effective) instruction mutation).")
}