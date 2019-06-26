package nz.co.jedsimson.lgp.core.environment

import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.constants.ConstantLoader
import nz.co.jedsimson.lgp.core.environment.operations.OperationLoader
import nz.co.jedsimson.lgp.core.evolution.ResultAggregator
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunctionProvider
import nz.co.jedsimson.lgp.core.modules.*
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.instructions.Operation
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import kotlin.random.Random

/**
 * Acts as a facade for simplifying access to details of an [Environment].
 *
 * An [Environment] is central during the operation of the system, as different sub-systems
 * rely on it for providing random state, configuration, module instances, and numerous other things.
 *
 * This facade provides the public definition for an [Environment] that consumers can rely on.
 */
interface EnvironmentFacade<TProgram, TOutput : Output<TProgram>>  {

    /**
     * Provides access to the environments random state.
     *
     * This allows the entire framework to share the random state (as a singleton) to allow deterministic runs.
     */
    val randomState: Random

    /**
     * Provides a function that can measure the fitness of a program.
     */
    val fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput>

    /**
     * Contains the various configuration options available to the LGP system.
     */
    val configuration: Configuration

    /**
     * A set of constants loaded using the [ConstantLoader] given at construction time.
     */
    val constants: List<TProgram>

    /**
     * A set of operations loaded using the [OperationLoader] given at construction time.
     */
    val operations: List<Operation<TProgram>>

    /**
     * Provides mechanisms for collecting results during the lifetime of an environment.
     */
    val resultAggregator: ResultAggregator<TProgram>

    /**
     * Provides access to modules that are registered in the environment.
     *
     * The [ModuleFactory] acts as a service locator within the system, allowing sub-systems to get the dependencies they need.
     */
    val moduleFactory: ModuleFactory<TProgram, TOutput>

    /**
     * Provides access to the set of registers that programs in the environment will use.
     */
    val registerSet: RegisterSet<TProgram>

    /**
     * Registers the modules given by a container.
     *
     * This acts as an aggregate form of the [registerModule] function.
     *
     * @param container A container that specifies modules to be registered.
     */
    fun registerModules(container: ModuleContainer<TProgram, TOutput>)

    /**
     * Register a module builder with a particular module type.
     *
     * @param type The type of module to associate this builder with.
     * @param builder A function that can create the module.
     */
    fun registerModule(type: RegisteredModuleType, builder: ModuleBuilder<TProgram, TOutput>)

    /**
     * Produces a clone of the current environment.
     *
     * @return A new [EnvironmentFacade] instance that is a copy of the current one.
     */
    fun copy(): EnvironmentFacade<TProgram, TOutput>
}