package lgp.core.environment

import lgp.core.environment.config.Config
import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.constants.ConstantLoader
import lgp.core.environment.dataset.DatasetLoader
import lgp.core.environment.operations.OperationLoader
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.ModuleLoader


public open class Environment<T> {

    private var configLoader: ConfigLoader? = null
    private var constantLoader: ConstantLoader<T>? = null
    private var datasetLoader: DatasetLoader<T>? = null
    private var operationLoader: OperationLoader<T>? = null
    private var defaultValueProvider: DefaultValueProvider<T>? = null
    private val moduleLoader: ModuleLoader

    private var config: Config? = null

    constructor(configLoader: ConfigLoader, constantLoader: ConstantLoader<T>,
                datasetLoader: DatasetLoader<T>, operationLoader: OperationLoader<T>,
                defaultValueProvider: DefaultValueProvider<T>) {

        this.configLoader = configLoader
        this.constantLoader = constantLoader
        this.datasetLoader = datasetLoader
        this.operationLoader = operationLoader
        this.defaultValueProvider = defaultValueProvider
        this.moduleLoader = ModuleLoader()

        this.load()
    }

    private fun load() {
        this.config = this.configLoader?.load()
    }
}