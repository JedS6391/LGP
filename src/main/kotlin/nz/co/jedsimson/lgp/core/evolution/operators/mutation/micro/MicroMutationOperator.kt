package nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro

import nz.co.jedsimson.lgp.core.environment.EnvironmentDefinition
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.MutationOperator
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program

/**
 * A [MutationOperator] implementation that performs effective micro mutations.
 *
 * For more information, see Algorithm 6.2 from Linear Genetic Programming (Brameier, M., Banzhaf, W. 2001).
 *
 * Note that the constant mutation rate is 1 - ([registerMutationRate] - [operatorMutationRate]), which should
 * be taken into account when choosing values for these parameters.
 *
 * @property registerMutationRate The rate with which registers should be mutated.
 * @property operatorMutationRate The rate with which operates should be mutated.
 * @property constantMutationFunc A function that can mutate values in the domain of [TProgram].
 */
class MicroMutationOperator<TProgram, TOutput : Output<TProgram>>(
    environment: EnvironmentDefinition<TProgram, TOutput>,
    private val registerMutationRate: Double,
    private val operatorMutationRate: Double,
    private val constantMutationFunc: ConstantMutationFunction<TProgram>
) : MutationOperator<TProgram, TOutput>(environment) {

    private val mutationStrategyFactory = MicroMutationStrategyFactory(
        this.environment,
        this.registerMutationRate,
        this.operatorMutationRate,
        this.constantMutationFunc
    )

    /**
     * Performs a single, effective micro mutation to the individual given.
     */
    override fun mutate(individual: Program<TProgram, TOutput>) {
        // 1. Randomly select an (effective) instruction from program gp.
        individual.findEffectiveProgram()

        // Can't do anything if there are no effective instructions
        if (individual.effectiveInstructions.isEmpty()) {
            return
        }

        val mutationStrategy = this.mutationStrategyFactory.getStrategyForIndividual(individual)

        mutationStrategy.mutate(individual)
    }

    override val information = ModuleInformation("Algorithm 6.2 ((effective) micro mutation).")
}