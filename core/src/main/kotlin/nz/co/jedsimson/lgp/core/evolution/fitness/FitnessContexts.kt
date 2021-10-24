package nz.co.jedsimson.lgp.core.evolution.fitness

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Outputs

/**
 * A collection of built-in [FitnessContext] implementations.
 */
object FitnessContexts {

    /**
     * Facilitates fitness evaluation for programs which have a single output.
     *
     * For programs with multiple outputs, [MultipleOutputFitnessContext] should be used.
     *
     * @constructor Creates a new [SingleOutputFitnessContext] with the given [EnvironmentFacade].
     */
    class SingleOutputFitnessContext<TData>(
        environment: EnvironmentFacade<TData, Outputs.Single<TData>, Targets.Single<TData>>
    ) : BaseFitnessContext<TData, Outputs.Single<TData>, Targets.Single<TData>>(environment) {

        override val information = ModuleInformation(
            description = "A built-in fitness context for evaluating the fitness of single-output programs."
        )
    }

    /**
     * Facilitates fitness evaluation for programs which have multiple outputs.
     *
     * For programs with a single output, [SingleOutputFitnessContext] should be used.
     */
    class MultipleOutputFitnessContext<TData>(
            environment: EnvironmentFacade<TData, Outputs.Multiple<TData>, Targets.Multiple<TData>>
    ) : BaseFitnessContext<TData, Outputs.Multiple<TData>, Targets.Multiple<TData>>(environment) {

        override val information = ModuleInformation(
            description = "A built-in fitness context for evaluating the fitness of multiple-output programs."
        )
    }
}