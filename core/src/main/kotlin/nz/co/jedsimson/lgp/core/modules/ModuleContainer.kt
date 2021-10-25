package nz.co.jedsimson.lgp.core.modules

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.program.Output

/**
 * A function that can be used to build a given [Module].
 */
typealias ModuleBuilder<TProgram, TOutput, TTarget> = (EnvironmentFacade<TProgram, TOutput, TTarget>) -> Module

/**
 * A container that provides modules that need to be registered with an environment.
 *
 * @property modules A mapping of modules that can be registered to a function that constructs that module.
 * @constructor Creates a new [ModuleContainer] with the given set of [modules].
 */
data class ModuleContainer<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    val modules: MutableMap<RegisteredModuleType, ModuleBuilder<TProgram, TOutput, TTarget>>
) {

    /**
     * Access to the environment that can be used when executing one of the containers [ModuleBuilder].
     */
    lateinit var environment: EnvironmentFacade<TProgram, TOutput, TTarget>
}