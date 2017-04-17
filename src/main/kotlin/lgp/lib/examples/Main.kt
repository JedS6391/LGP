package lgp.lib.examples

import lgp.core.environment.DefaultValueProvider
import lgp.core.environment.Environment
import lgp.core.environment.ModuleContainer
import lgp.core.environment.RegisteredModuleType
import lgp.core.environment.config.JsonConfigLoader
import lgp.core.environment.constants.DoubleConstantLoader
import lgp.core.environment.constants.GenericConstantLoader
import lgp.core.environment.dataset.CsvDatasetLoader
import lgp.core.environment.operations.DefaultOperationLoader
import lgp.core.evolution.fitness.FitnessCase
import lgp.core.evolution.fitness.FitnessContext
import lgp.core.evolution.fitness.FitnessFunction
import lgp.core.evolution.instructions.Operation
import lgp.core.evolution.population.Population
import lgp.core.evolution.population.Program
import lgp.core.modules.Module
import lgp.lib.BaseInstructionGenerator
import lgp.lib.BaseProgramGenerator


class Main {
    companion object Example {

        // Locations of configuration and dataset files.
        private const val configFilename = "/Users/jedsimson/Desktop/env.json"
        private const val datasetFilename = "/Users/jedsimson/Desktop/dataset.csv"

        @JvmStatic fun main(args: Array<String>) {
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
            val constantLoader = GenericConstantLoader<Double>(
                    constants = config.constants,
                    parseFunction = String::toDouble
            )

            val defaultValueProvider = object : DefaultValueProvider<Double> {
                override val value: Double
                    get() = 0.0
            }

            // Mean-squared error fitness function
            val mse: FitnessFunction<Double> = { program, cases ->
                var fitness = 0.0

                for (case in cases) {
                    // Make sure the registers are in a default state
                    program.registers.reset()

                    // Load the case
                    program.registers.writeInstance(case)

                    program.execute()

                    // Just use the first register as output
                    val actual: Double = program.registers.read(0)
                    val expected: Double = case.classAttribute().value

                    fitness += Math.pow((actual - expected), 2.0)
                }

                ((1.0 / cases.size.toDouble()) * fitness)
            }

            // Create a new environment with these loaders.
            val environment = Environment<Double>(
                    configLoader,
                    constantLoader,
                    datasetLoader,
                    operationLoader,
                    defaultValueProvider,
                    fitnessFunction = mse
            )

            val container = ModuleContainer(
                    modules = mapOf(
                            RegisteredModuleType.InstructionGenerator to { BaseInstructionGenerator(environment) },
                            RegisteredModuleType.ProgramGenerator to { BaseProgramGenerator(environment) }
                    )
            )

            environment.registerModules(container)

            val population = Population(environment)
            population.evolve()
        }
    }
}

