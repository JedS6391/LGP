package nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.EffectiveCalculationRegisterResolvers
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategy
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategyFactory
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.registers.RandomRegisterGenerator

private enum class MicroMutationType {
    Register,
    Operator,
    Constant
}

/**
 * Responsible for creating [MutationStrategy] instances to be used to perform micro-mutations.
 *
 * @property environment An environment that the mutation is occurring in.
 * @property registerMutationRate The rate with which registers should be mutated.
 * @property operatorMutationRate The rate with which operates should be mutated.
 * @property constantMutationFunc A function that can mutate values in the domain of [TProgram].
 */
internal class MicroMutationStrategyFactory<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    private val environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    private val registerMutationRate: Double,
    private val operatorMutationRate: Double,
    private val constantMutationFunc: ConstantMutationFunction<TProgram>
) : MutationStrategyFactory<TProgram, TOutput, TTarget>() {

    private val random = this.environment.randomState

    override fun getStrategyForIndividual(
        individual: Program<TProgram, TOutput>
    ): MutationStrategy<TProgram, TOutput, TTarget> {

        // 2. Randomly select mutation type register | operator | constant with
        // probability p_regmut | p_opermut | p_constmut and with p_regmut +
        // p_opermut + p_constmut = 1.
        // Assumption: p_constmut = 1 - (p_regmut + p_opermut).
        val p = random.nextDouble()

        val mutationType = when {
            (p < this.registerMutationRate) -> {
                MicroMutationType.Register
            }
            (registerMutationRate <= p && p <= (this.registerMutationRate + this.operatorMutationRate)) -> {
                MicroMutationType.Operator
            }
            else -> {
                MicroMutationType.Constant
            }
        }

        val registerGenerator = RandomRegisterGenerator(this.environment.randomState, individual.registers)

        return when (mutationType) {
            MicroMutationType.Register -> MicroMutationStrategies.RegisterMicroMutationStrategy(
                environment,
                registerGenerator,
                EffectiveCalculationRegisterResolvers::baseResolver
            )

            MicroMutationType.Operator -> MicroMutationStrategies.OperatorMicroMutationStrategy(
                environment,
                registerGenerator
            )

            MicroMutationType.Constant -> MicroMutationStrategies.ConstantMicroMutationStrategy(
                environment,
                this.constantMutationFunc
            )
        }
    }
}