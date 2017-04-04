import lgp.core.environment.DefaultValueProvider
import lgp.core.environment.Environment
import lgp.core.environment.config.JsonConfigLoader
import lgp.core.environment.constants.GenericConstantLoader
import lgp.core.environment.dataset.*
import lgp.core.environment.operations.DefaultOperationLoader
import lgp.core.evolution.registers.RegisterSet
import java.util.*

object Example {

    @JvmStatic fun main(args: Array<String>) {

        val configFilename = "/Users/jedsimson/Desktop/env.json"
        val datasetFilename = "/Users/jedsimson/Desktop/dataset.csv"

        // Load up configuration information from the JSON file
        val configLoader = JsonConfigLoader(
                filename = configFilename
        )

        val config = configLoader.load()

        // Set up a loader to load the data set in the CSV file we specified above
        // Note that the type parameter of the data is automatically inferred from
        // the function given for parsing.
        val datasetLoader = CsvDatasetLoader(
                filename = datasetFilename,
                parseFunction = String::toDouble
        )

        val dataset = datasetLoader.load()

        // The first column in this data set is the output
        dataset.setClassAttribute(0)

        // Set up a loader for loading the operations we want to use (specified in the configuration file)
        val operationLoader = DefaultOperationLoader<Double>(
                operationNames = config.operations
        )

        // Set up a loader for the constant values (specified in the configuration file)
        val constantLoader = GenericConstantLoader(
                constants = config.constants,
                parseFunction = String::toDouble
        )

        val defaultValueProvider = object : DefaultValueProvider<Double> {
            override val value: Double
                get() = 0.0
        }

        // Create a new environment with these loaders.
        val environment = Environment<Double>(
                configLoader,
                constantLoader,
                datasetLoader,
                operationLoader,
                defaultValueProvider
        )

        val registers = RegisterSet<Double>(
                // -1 is to care for class attribute
                inputRegisters = dataset.numAttributes() - 1,
                calculationRegisters = config.numCalculationRegisters,
                constants = constantLoader.load(),
                defaultValueProvider = defaultValueProvider
        )

    }
}
