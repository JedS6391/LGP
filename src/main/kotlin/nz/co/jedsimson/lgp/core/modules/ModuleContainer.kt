package nz.co.jedsimson.lgp.core.modules

import nz.co.jedsimson.lgp.core.environment.Environment
import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.program.Output

/**
 * Exception thrown when no [Module] is registered for a requested [RegisteredModuleType].
 */
class MissingModuleException(message: String) : Exception(message)

/**
 * Exception thrown when a [Module] is cast as a type that is not valid for it.
 */
class ModuleCastException(message: String) : Exception(message)

/**
 * Represents the different modules that are able to be registered with an environment.
 *
 * Any module that is able to be registered with the [Environment] as a *registered component*
 * should have a module type defined for it using this interface.
 *
 * @see [CoreModuleType] for an example implementation.
 */
interface RegisteredModuleType

/**
 * A mapping for core modules to a module type value.
 */
enum class CoreModuleType : RegisteredModuleType {

    /**
     * An [InstructionGenerator] implementation.
     */
    InstructionGenerator,

    /**
     * A [ProgramGenerator] implementation.
     */
    ProgramGenerator,

    /**
     * A [SelectionOperator] implementation.
     */
    SelectionOperator,

    /**
     * A [RecombinationOperator] implementation.
     */
    RecombinationOperator,

    /**
     * A [MacroMutationOperator] implementation.
     */
    MacroMutationOperator,

    /**
     * A [MicroMutationOperator] implementation.
     */
    MicroMutationOperator,

    /**
     * A [FitnessContext] implementation.
     */
    FitnessContext
}

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
     * An environment that can be used when executing one of the containers [ModuleBuilder].
     */
    lateinit var environment: EnvironmentFacade<TProgram, TOutput, TTarget>
}