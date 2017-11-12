package lgp.core.evolution

import lgp.core.environment.DefaultValueProvider
import lgp.core.environment.Environment
import lgp.core.environment.ModuleContainer
import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.constants.ConstantLoader
import lgp.core.environment.operations.OperationLoader
import lgp.core.evolution.fitness.FitnessFunction
import lgp.core.evolution.population.EvolutionModel

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
abstract class Problem<T> {

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
    abstract val configLoader: ConfigLoader

    /**
     * A component that can provide constants for the problem.
     */
    abstract val constantLoader: ConstantLoader<T>

    /**
     * A component that can provide operations for the problem.
     */
    abstract val operationLoader: OperationLoader<T>

    /**
     * A component that provides default values to the register set.
     */
    abstract val defaultValueProvider: DefaultValueProvider<T>

    /**
     * A fitness metric to be used for this problem.
     */
    abstract val fitnessFunction: FitnessFunction<T>

    /**
     * A collection of modules that should be registered with the environment.
     */
    abstract val registeredModules: ModuleContainer<T>

    /**
     * An environment built up of the components this problem uses.
     *
     * The environment should be initialised in the [initialiseEnvironment] method.
     */
    lateinit var environment: Environment<T>

    /**
     * An evolutionary model that should be used to solve this problem.
     *
     * The model should be initialised in the [initialiseModel] method.
     */
    lateinit var model: EvolutionModel<T>

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
    abstract fun solve(): Solution<T>
}