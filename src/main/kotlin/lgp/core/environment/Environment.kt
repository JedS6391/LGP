package lgp.core.environment

import lgp.core.environment.config.Config
import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.constants.ConstantLoader
import lgp.core.environment.dataset.Dataset
import lgp.core.environment.dataset.DatasetLoader
import lgp.core.environment.operations.OperationLoader
import lgp.core.evolution.instructions.Operation
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.ModuleLoader


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

    constructor(configLoader: ConfigLoader, constantLoader: ConstantLoader<T>,
                datasetLoader: DatasetLoader<T>, operationLoader: OperationLoader<T>,
                defaultValueProvider: DefaultValueProvider<T>, initialise: Boolean = true) {

        this.configLoader = configLoader
        this.constantLoader = constantLoader
        this.datasetLoader = datasetLoader
        this.operationLoader = operationLoader
        this.defaultValueProvider = defaultValueProvider
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

        this.initialiseRegisterSet()
    }

    private fun initialiseRegisterSet() {
         this.registerSet = RegisterSet(
                // -1 is to care for class attribute
                inputRegisters = this.dataset.numAttributes() - 1,
                calculationRegisters = this.config.numCalculationRegisters,
                constants = this.constants,
                defaultValueProvider = this.defaultValueProvider
        )

    }
}