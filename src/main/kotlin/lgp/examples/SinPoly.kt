package lgp.examples

import lgp.core.environment.*
import lgp.core.environment.config.JsonConfigLoader
import lgp.core.environment.constants.GenericConstantLoader
import lgp.core.environment.dataset.CsvDatasetLoader
import lgp.core.environment.operations.DefaultOperationLoader
import lgp.core.evolution.Runners
import lgp.core.evolution.fitness.FitnessFunctions
import lgp.core.evolution.population.*
import lgp.lib.BaseInstructionGenerator
import lgp.lib.BaseProgram
import lgp.lib.BaseProgramGenerator
import lgp.lib.BaseProgramSimplifier
import java.util.*

class SinPoly {
    companion object Main {
        // Locations of configuration and data set files.
        private val configFilename = this::class.java.classLoader.getResource("sinpoly_env.json").file
        private val datasetFilename = this::class.java.classLoader.getResource("sinpoly.csv").file

        @JvmStatic fun main(args: Array<String>) {
            val configLoader = JsonConfigLoader(
                    filename = configFilename
            )

            val config = configLoader.load()

            val datasetLoader = CsvDatasetLoader(
                    filename = datasetFilename,
                    parseFunction = String::toDouble
            )

            val operationLoader = DefaultOperationLoader<Double>(
                    operationNames = config.operations
            )

            val constantLoader = GenericConstantLoader(
                    constants = config.constants,
                    parseFunction = String::toDouble
            )

            val defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)

            val sse = FitnessFunctions.SSE()

            val environment = Environment(
                    configLoader,
                    constantLoader,
                    datasetLoader,
                    operationLoader,
                    defaultValueProvider,
                    fitnessFunction = sse
            )

            // Set up registered modules
            val container = ModuleContainer(
                    modules = mutableMapOf(
                            CoreModuleType.InstructionGenerator to {
                                BaseInstructionGenerator(environment)
                            },
                            CoreModuleType.ProgramGenerator to {
                                BaseProgramGenerator(environment, sentinelTrueValue = 1.0)
                            },
                            CoreModuleType.SelectionOperator to {
                                TournamentSelection(environment, tournamentSize = 4)
                            },
                            CoreModuleType.RecombinationOperator to {
                                LinearCrossover(
                                        environment,
                                        maximumSegmentLength = 6,
                                        maximumCrossoverDistance = 5,
                                        maximumSegmentLengthDifference = 3
                                )
                            },
                            CoreModuleType.MacroMutationOperator to {
                                MacroMutationOperator(
                                        environment,
                                        insertionRate = 0.67,
                                        deletionRate = 0.33
                                )
                            },
                            CoreModuleType.MicroMutationOperator to {
                                MicroMutationOperator(
                                        environment,
                                        registerMutationRate = 0.5,
                                        operatorMutationRate = 0.5,
                                        constantMutationFunc = { v -> v + (Random().nextGaussian() * 1) }
                                )
                            }
                    )
            )

            environment.registerModules(container)

            // Find the best individual with these parameters.
            val model = Models.SteadyState(environment)

            val runner = Runners.DistributedRunner(environment, model, runs = 10)
            val result = runner.run()
            val simplifier = BaseProgramSimplifier<Double>()

            println("Results:")

            result.evaluations.forEachIndexed { run, res ->
                println("Run ${run + 1} (best fitness = ${res.best.fitness})")
                println(simplifier.simplify(res.best as BaseProgram<Double>))

                println("\nStats (last run only):\n")

                for ((k, v) in res.statistics.last().data) {
                    println("$k = $v")
                }
                println("")
            }

            val avgBestFitness = result.evaluations.map { eval -> eval.best.fitness }.sum() / result.evaluations.size
            println("Average best fitness (over 10 runs): $avgBestFitness")
        }
    }
}

