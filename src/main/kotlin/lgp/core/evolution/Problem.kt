package lgp.core.evolution

import lgp.core.environment.DefaultValueProvider
import lgp.core.environment.Environment
import lgp.core.environment.ModuleContainer
import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.constants.ConstantLoader
import lgp.core.environment.dataset.DatasetLoader
import lgp.core.environment.operations.OperationLoader
import lgp.core.evolution.fitness.FitnessFunction
import lgp.core.evolution.population.EvolutionModel

data class Description(val description: String)

interface Solution<T> {
    val problem: String
}

class ProblemNotInitialisedException(message: String) : Exception(message)

abstract class Problem<T> {
    abstract val name: String
    abstract val description: Description

    abstract val configLoader: ConfigLoader
    abstract val constantLoader: ConstantLoader<T>
    abstract val datasetLoader: DatasetLoader<T>
    abstract val operationLoader: OperationLoader<T>
    abstract val defaultValueProvider: DefaultValueProvider<T>
    abstract val fitnessFunction: FitnessFunction<T>

    abstract val registeredModules: ModuleContainer

    lateinit var environment: Environment<T>
    lateinit var model: EvolutionModel<T>

    abstract fun initialiseEnvironment()
    abstract fun initialiseModel()
    abstract fun solve(): Solution<T>
}