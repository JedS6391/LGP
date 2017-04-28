package lgp.examples

import lgp.core.environment.*
import lgp.core.environment.config.JsonConfigLoader
import lgp.core.environment.constants.GenericConstantLoader
import lgp.core.environment.dataset.CsvDatasetLoader
import lgp.core.environment.operations.DefaultOperationLoader
import lgp.core.evolution.fitness.FitnessFunctions
import lgp.core.evolution.population.*
import lgp.lib.BaseInstructionGenerator
import lgp.lib.BaseProgramGenerator
import java.util.*

/**
 * An example of setting up an environment to use LGP to find programs for the function `x^2 + 2x + 2`.
 *
 * This example serves as a good way to learn how to use the system and to ensure that everything
 * is working correctly, as some percentage of the time, perfect individuals should be found.
 */
class SimpleFunction {
    companion object Main {
        // Locations of configuration and data set files.
        private val configFilename = this::class.java.classLoader.getResource("simple_function_env.json").file
        private val datasetFilename = this::class.java.classLoader.getResource("simple_function.csv").file

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

            // Set up a loader for loading the operations we want to use (specified in the configuration file)
            val operationLoader = DefaultOperationLoader<Double>(
                    operationNames = config.operations
            )

            // Set up a loader for the constant values (specified in the configuration file)
            val constantLoader = GenericConstantLoader<Double>(
                    constants = config.constants,
                    parseFunction = String::toDouble
            )

            // Fill calculation registers with the value 1.0
            val defaultValueProvider = DefaultValueProviders.constantValueProvider<Double>(1.0)

            // Use mean-squared error function for fitness evaluations.
            val mse = FitnessFunctions.MSE()

            // Create a new environment with these components.
            val environment = Environment<Double>(
                    configLoader,
                    constantLoader,
                    datasetLoader,
                    operationLoader,
                    defaultValueProvider,
                    fitnessFunction = mse
            )

            // Set up registered modules
            val container = ModuleContainer(
                    modules = mapOf(
                            RegisteredModuleType.InstructionGenerator to {
                                BaseInstructionGenerator(environment)
                            },
                            RegisteredModuleType.ProgramGenerator to {
                                BaseProgramGenerator(environment, sentinelTrueValue = 1.0)
                            },
                            RegisteredModuleType.SelectionOperator to {
                                TournamentSelection(environment, tournamentSize = 2)
                            },
                            RegisteredModuleType.RecombinationOperator to {
                                Crossover(
                                        environment,
                                        maximumSegmentLength = 6,
                                        maximumCrossoverDistance = 5,
                                        maximumSegmentLengthDifference = 3
                                )
                            },
                            RegisteredModuleType.MacroMutationOperator to {
                                MacroMutationOperator(
                                        environment,
                                        insertionRate = 0.67,
                                        deletionRate = 0.33
                                )
                            },
                            RegisteredModuleType.MicroMutationOperator to {
                                MicroMutationOperator(
                                        environment,
                                        registerMutationRate = 0.5,
                                        operatorMutationRate = 0.3,
                                        constantMutationFunc = { v ->
                                            // Add random gaussian noise to constant with standard deviation of 1
                                            // from the current value.
                                            v + (Random().nextGaussian() * 1)
                                        }
                                )
                            }
                    )
            )

            environment.registerModules(container)

            // Find the best individual with these parameters.
            val population = Population(environment)
            population.evolve()
        }
    }
}

