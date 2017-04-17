package lgp.core.environment

import lgp.core.environment.config.Config
import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.constants.ConstantLoader
import lgp.core.environment.dataset.Dataset
import lgp.core.environment.dataset.DatasetLoader
import lgp.core.environment.operations.OperationLoader
import lgp.core.evolution.fitness.FitnessFunction
import lgp.core.evolution.instructions.Operation
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

enum class RegisteredModuleType {
    InstructionGenerator,
    ProgramGenerator
}

data class ModuleContainer(val modules: Map<RegisteredModuleType, () -> Module>) {

    // All instances are provided as singletons
    private val instanceCache = mutableMapOf<RegisteredModuleType, Module>()

    fun <TModule> instance(type: RegisteredModuleType): TModule {
        if (type in instanceCache)
            return instanceCache[type] as TModule

        val moduleBuilder = this.modules[type]
        val module = moduleBuilder?.invoke()

        instanceCache[type] = module!!

        return module as TModule
    }

}

open class Environment<T> {

    // Dependencies that we require at construction time
    private val configLoader: ConfigLoader
    private val constantLoader: ConstantLoader<T>
    private val datasetLoader: DatasetLoader<T>
    private val operationLoader: OperationLoader<T>
    private val defaultValueProvider: DefaultValueProvider<T>
    val fitnessFunction: FitnessFunction<T>

    // Dependencies that come from the loaders given to the environment and are not necessarily
    // needed until the environment is initialised.
    lateinit var config: Config
    lateinit var constants: List<T>
    lateinit var dataset: Dataset<T>
    lateinit var operations: List<Operation<T>>

    lateinit var registerSet: RegisterSet<T>

    lateinit var container: ModuleContainer

    // TODO: Should a default environment be provided?
    constructor(configLoader: ConfigLoader, constantLoader: ConstantLoader<T>,
                datasetLoader: DatasetLoader<T>, operationLoader: OperationLoader<T>,
                defaultValueProvider: DefaultValueProvider<T>, fitnessFunction: FitnessFunction<T>,
                initialise: Boolean = true) {

        this.configLoader = configLoader
        this.constantLoader = constantLoader
        this.datasetLoader = datasetLoader
        this.operationLoader = operationLoader
        this.defaultValueProvider = defaultValueProvider
        this.fitnessFunction = fitnessFunction

        // Kick off initialisation
        if (initialise)
            this.initialise()
    }

    private fun initialise() {
        // Load the components each loader is responsible for.
        this.config = this.configLoader.load()
        this.constants = this.constantLoader.load()
        this.dataset = this.datasetLoader.load()
        this.operations = this.operationLoader.load()

        // TODO: Instead of initialising, allow user to register?
        this.initialiseRegisterSet()
    }

    private fun initialiseRegisterSet() {
        // The environment takes care of its own base register set that is not modified by programs.
        // This means that anything that can access the environment has access to a blank register set.
        // TODO: Pass environment to register set and make it a dependency that must be registered.
        this.registerSet = RegisterSet(
                // -1 is to care for class attribute
                inputRegisters = this.dataset.numAttributes() - 1,
                calculationRegisters = this.config.numCalculationRegisters,
                constants = this.constants,
                defaultValueProvider = this.defaultValueProvider
        )
    }

    private fun validateModules(container: ModuleContainer): Boolean {
        // Check all module types have an implementation
        return RegisteredModuleType.values().map { type ->
            type in container.modules
        }.all { b -> b}
    }

    fun registerModules(container: ModuleContainer) {
        if (!this.validateModules(container)) {
            throw Exception("All module types must have a module implementation registered to them.")
        }

        this.container = container
    }

    fun <TModule> registeredModule(type: RegisteredModuleType): TModule {
        return this.container.instance<TModule>(type)
    }

}