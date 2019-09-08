package nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.events.Diagnostics
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.MutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategy
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategyFactory
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
 * @property mutationStrategyFactory A factory that can be used to get a [MutationStrategy] to delegate the mutation to.
 * @constructor Creates a new [MicroMutationOperator] with the given [environment], [registerMutationRate], [operatorMutationRate], [constantMutationFunc], and [mutationStrategyFactory].
 */
class MicroMutationOperator<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    private val registerMutationRate: Double,
    private val operatorMutationRate: Double,
    private val constantMutationFunc: ConstantMutationFunction<TProgram>,
    private val mutationStrategyFactory: MutationStrategyFactory<TProgram, TOutput, TTarget>
) : MutationOperator<TProgram, TOutput, TTarget>(environment) {

    init {
        if ((registerMutationRate + operatorMutationRate) > 1.0) {
            throw IllegalArgumentException(
                "register mutation rate + operator mutation rate must be less than or equal to 1.0 (was ${registerMutationRate + operatorMutationRate})"
            )
        }
    }

    /**
     * Creates a new [MicroMutationOperator] with the given [environment], [registerMutationRate], [operatorMutationRate], and [constantMutationFunc].
     *
     * The [MicroMutationOperator] will use the default mutation strategy factory ([MicroMutationStrategyFactory]).
     */
    constructor(
        environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
        registerMutationRate: Double,
        operatorMutationRate: Double,
        constantMutationFunc: ConstantMutationFunction<TProgram>
    ) : this(
        environment,
        registerMutationRate,
        operatorMutationRate,
        constantMutationFunc,
        MicroMutationStrategyFactory(
            environment,
            registerMutationRate,
            operatorMutationRate,
            constantMutationFunc
        )
    )

    /**
     * Performs a single, effective micro mutation to the individual given.
     */
    override fun mutate(individual: Program<TProgram, TOutput>) {
        // 1. Randomly select an (effective) instruction from program gp.
        Diagnostics.traceWithTime("MicroMutationOperator:find-effective-program") {
            individual.findEffectiveProgram()
        }

        // Can't do anything if there are no effective instructions
        if (individual.effectiveInstructions.isEmpty()) {
            return
        }

        val mutationStrategy = this.mutationStrategyFactory.getStrategyForIndividual(individual)

        Diagnostics.traceWithTime("MicroMutationOperator:mutation-strategy-execution") {
            mutationStrategy.mutate(individual)
        }
    }

    override val information = ModuleInformation("Algorithm 6.2 ((effective) micro mutation).")
}