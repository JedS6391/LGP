package lgp.core.environment

import lgp.core.environment.config.Config
import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.constants.ConstantLoader
import lgp.core.environment.dataset.Dataset
import lgp.core.environment.dataset.DatasetLoader
import lgp.core.environment.operations.OperationLoader
import lgp.core.evolution.fitness.FitnessFunction
import lgp.core.evolution.instructions.Operation
import lgp.core.evolution.population.Population
import lgp.core.evolution.population.ProgramGenerator
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.ModuleLoader
import lgp.lib.BaseProgramGenerator


public open class Environment<T> {

    private val configLoader: ConfigLoader
    private val constantLoader: ConstantLoader<T>
    private val datasetLoader: DatasetLoader<T>
    private val operationLoader: OperationLoader<T>
    private val defaultValueProvider: DefaultValueProvider<T>
    private val moduleLoader: ModuleLoader

    lateinit var config: Config
    lateinit var constants: List<T>
    lateinit var dataset: Dataset<T>
    lateinit var operations: List<Operation<T>>
    lateinit var registerSet: RegisterSet<T>
    lateinit var programGenerator: ProgramGenerator<T>
    lateinit var fitnessFunction: FitnessFunction<T>
    lateinit var population: Population<T>

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
        this.moduleLoader = ModuleLoader()

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
        this.initialiseProgramGenerator()
        this.initialisePopulation()
    }

    private fun initialiseRegisterSet() {
        // The environment takes care of its own base register set that is not modified by programs.
        // This means that anything that can access the environment has access to a blank register set.
        this.registerSet = RegisterSet(
                // -1 is to care for class attribute
                inputRegisters = this.dataset.numAttributes() - 1,
                calculationRegisters = this.config.numCalculationRegisters,
                constants = this.constants,
                defaultValueProvider = this.defaultValueProvider
        )
    }

    private fun initialiseProgramGenerator() {
        // TODO: Read implementation from config and load with module loader
        this.programGenerator = BaseProgramGenerator(this)
    }

    private fun initialisePopulation() {
        this.population = Population(this)
    }
}