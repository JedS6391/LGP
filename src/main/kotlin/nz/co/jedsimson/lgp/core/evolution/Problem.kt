package nz.co.jedsimson.lgp.core.evolution

import nz.co.jedsimson.lgp.core.environment.DefaultValueProvider
import nz.co.jedsimson.lgp.core.environment.Environment
import nz.co.jedsimson.lgp.core.environment.config.ConfigurationLoader
import nz.co.jedsimson.lgp.core.environment.constants.ConstantLoader
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.operations.OperationLoader
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunctionProvider
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.evolution.model.EvolutionModel
import nz.co.jedsimson.lgp.core.modules.ModuleContainer

data class Description(val description: String)

/**
 * Represents a solution to a problem.
 *
 * The interface is left open so that when defining one's problem, it is straight-forward
 * to define the format of the solution, as it will probably depend on the use case
 * on what the solution should actually contain.
 */
interface Solution<T> {

    /**
     * The name of the problem this solution is for.
     */
    val problem: String
}

/**
 * Exception given when a problem is attempted to be solved when it hasn't
 * been completely initialised.
 */
class ProblemNotInitialisedException(message: String) : Exception(message)

/**
 * Defines a problem and the components that should be used to solve that problem.
 */
abstract class Problem<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>> {

    /**
     * A name for this problem.
     */
    abstract val name: String

    /**
     * A brief description of the problem (e.g. it's function, feature range, etc.)
     */
    abstract val description: Description

    /**
     * A component that can provide configuration for the problem.
     */
    abstract val configLoader: ConfigurationLoader

    /**
     * A component that can provide constants for the problem.
     */
    abstract val constantLoader: ConstantLoader<TProgram>

    /**
     * A component that can provide operations for the problem.
     */
    abstract val operationLoader: OperationLoader<TProgram>

    /**
     * A component that provides default values to the register set.
     */
    abstract val defaultValueProvider: DefaultValueProvider<TProgram>

    /**
     * A fitness metric to be used for this problem.
     */
    abstract val fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput, TTarget>

    /**
     * A collection of modules that should be registered with the environment.
     */
    abstract val registeredModules: ModuleContainer<TProgram, TOutput, TTarget>

    /**
     * An environment built up of the components this problem uses.
     *
     * The environment should be initialised in the [initialiseEnvironment] method.
     */
    lateinit var environment: Environment<TProgram, TOutput, TTarget>

    /**
     * An evolutionary model that should be used to solve this problem.
     *
     * The model should be initialised in the [initialiseModel] method.
     */
    lateinit var model: EvolutionModel<TProgram, TOutput, TTarget>

    // TODO: Figure out a way to call initialisation methods automatically.

    /**
     * Initialises the environment for this problem.
     *
     * **NB:** This method must be called before [solve].
     */
    abstract fun initialiseEnvironment()

    /**
     * Initialises the model for this problem.
     *
     * **NB:** This method must be called before [solve].
     */
    abstract fun initialiseModel()

    /**
     * Solves the problem in some way and gives a solution.
     *
     * @return A solution for the problem.
     */
    abstract fun solve(): Solution<TProgram>
}