package lgp.examples

import kotlinx.coroutines.runBlocking
import lgp.core.environment.*
import lgp.core.environment.config.Configuration
import lgp.core.environment.config.ConfigurationLoader
import lgp.core.environment.constants.GenericConstantLoader
import lgp.core.environment.dataset.*
import lgp.core.environment.operations.DefaultOperationLoader
import lgp.core.evolution.*
import lgp.core.evolution.fitness.FitnessFunction
import lgp.core.evolution.fitness.FitnessFunctions
import lgp.core.evolution.fitness.SingleOutputFitnessContext
import lgp.core.evolution.model.Models
import lgp.core.evolution.operators.*
import lgp.core.evolution.training.DistributedTrainer
import lgp.core.evolution.training.DistributedTrainingJob
import lgp.core.evolution.training.SequentialTrainer
import lgp.core.evolution.training.TrainingResult
import lgp.core.modules.ModuleInformation
import lgp.lib.BaseInstructionGenerator
import lgp.lib.BaseProgram
import lgp.lib.BaseProgramGenerator
import lgp.lib.BaseProgramSimplifier

/*
 * An example of setting up an environment to use LGP to find programs for the function `x^2 + 2x + 2`.
 *
 * This example serves as a good way to learn how to use the system and to ensure that everything
 * is working correctly, as some percentage of the time, perfect individuals should be found.
 */

// A solution for this problem consists of the problem's name and a result from
// running the problem with a `Trainer` impl.
data class SimpleFunctionSolution(
        override val problem: String,
        val result: TrainingResult<Double>
) : Solution<Double>

// Define the problem and the necessary components to solve it.
class SimpleFunctionProblem : Problem<Double>() {
    override val name = "Simple Quadratic."

    override val description = Description("f(x) = x^2 + 2x + 2\n\trange = [-10:10:0.5]")

    override val configLoader = object : ConfigurationLoader {
        override val information = ModuleInformation("Overrides default configuration for this problem.")

        override fun load(): Configuration {
            val config = Configuration()

            config.initialMinimumProgramLength = 10
            config.initialMaximumProgramLength = 30
            config.minimumProgramLength = 10
            config.maximumProgramLength = 200
            config.operations = listOf(
                    "lgp.lib.operations.Addition",
                    "lgp.lib.operations.Subtraction",
                    "lgp.lib.operations.Multiplication"
            )
            config.constantsRate = 0.5
            config.constants = listOf("0.0", "1.0", "2.0")
            config.numCalculationRegisters = 4
            config.populationSize = 500
            config.generations = 1000
            config.numFeatures = 1
            config.microMutationRate = 0.4
            config.macroMutationRate = 0.6
            config.numOffspring = 10

            return config
        }
    }

    private val config = this.configLoader.load()

    override val constantLoader = GenericConstantLoader(
            constants = config.constants,
            parseFunction = String::toDouble
    )

    val datasetLoader = object : DatasetLoader<Double> {
        // x^2 + 2x + 2
        val func = { x: Double -> (x * x) + (2 * x) + 2 }
        val gen = SequenceGenerator()

        override val information = ModuleInformation("Generates samples in the range [-10:10:0.5].")

        override fun load(): Dataset<Double> {
            val xs = gen.generate(-10.0, 10.0, 0.5, inclusive = true).map { x ->
                Sample(
                    listOf(Feature(name = "x", value = x))
                )
            }

            val ys = xs.map { x ->
                this.func(x.features[0].value)
            }

            return Dataset(
                    xs.toList(),
                    ys.toList()
            )
        }
    }

    override val operationLoader = DefaultOperationLoader<Double>(
            operationNames = config.operations
    )

    override val defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)

    override val fitnessFunction: FitnessFunction<Double> = FitnessFunctions.MSE

    override val registeredModules = ModuleContainer<Double>(
            modules = mutableMapOf(
                    CoreModuleType.InstructionGenerator to { environment ->
                        BaseInstructionGenerator(environment)
                    },
                    CoreModuleType.ProgramGenerator to { environment ->
                        BaseProgramGenerator(
                                environment,
                                sentinelTrueValue = 1.0,
                                outputRegisterIndex = 0
                        )
                    },
                    CoreModuleType.SelectionOperator to { environment ->
                        TournamentSelection(environment, tournamentSize = 2)
                    },
                    CoreModuleType.RecombinationOperator to { environment ->
                        LinearCrossover(
                                environment,
                                maximumSegmentLength = 6,
                                maximumCrossoverDistance = 5,
                                maximumSegmentLengthDifference = 3
                        )
                    },
                    CoreModuleType.MacroMutationOperator to { environment ->
                        MacroMutationOperator(
                                environment,
                                insertionRate = 0.67,
                                deletionRate = 0.33
                        )
                    },
                    CoreModuleType.MicroMutationOperator to { environment ->
                        MicroMutationOperator(
                                environment,
                                registerMutationRate = 0.5,
                                operatorMutationRate = 0.5,
                                // Use identity func. since the probabilities
                                // of other micro mutations mean that we aren't
                                // modifying constants.
                                constantMutationFunc = ConstantMutationFunctions.identity()
                        )
                    },
                    CoreModuleType.FitnessContext to { environment ->
                        SingleOutputFitnessContext(environment)
                    }
            )
    )

    override fun initialiseEnvironment() {
        this.environment = Environment(
                this.configLoader,
                this.constantLoader,
                this.operationLoader,
                this.defaultValueProvider,
                this.fitnessFunction,
                ResultAggregators.BufferedResultAggregator(
                        ResultOutputProviders.CsvResultOutputProvider("results.csv")
                )
        )

        this.environment.registerModules(this.registeredModules)
    }

    override fun initialiseModel() {
        this.model = Models.SteadyState(this.environment)
    }

    override fun solve(): SimpleFunctionSolution {
        try {
            /*
            // This is an example of training sequentially in an asynchronous manner.
            val runner = SequentialTrainer(environment, model, runs = 2)

            return runBlocking {
                val job = runner.trainAsync(
                    this@SimpleFunctionProblem.datasetLoader.load()
                )

                job.subscribeToUpdates { println("training progress = ${it.progress}%") }

                val result = job.result()

                SimpleFunctionSolution(this@SimpleFunctionProblem.name, result)
            }
            */

            val runner = DistributedTrainer(environment, model, runs = 2)

            return runBlocking {
                val job = runner.trainAsync(
                    this@SimpleFunctionProblem.datasetLoader.load()
                )

                job.subscribeToUpdates { println("training progress = ${it.progress}") }

                val result = job.result()

                SimpleFunctionSolution(this@SimpleFunctionProblem.name, result)
            }

        } catch (ex: UninitializedPropertyAccessException) {
            // The initialisation routines haven't been run.
            throw ProblemNotInitialisedException(
                    "The initialisation routines for this problem must be run before it can be solved."
            )
        }
    }
}

class SimpleFunction {
    companion object Main {
        @JvmStatic fun main(args: Array<String>) {
            // Create a new problem instance, initialise it, and then solve it.
            val problem = SimpleFunctionProblem()
            problem.initialiseEnvironment()
            problem.initialiseModel()
            val solution = problem.solve()
            val simplifier = BaseProgramSimplifier<Double>()

            println("Results:")

            solution.result.evaluations.forEachIndexed { run, res ->
                println("Run ${run + 1} (best fitness = ${res.best.fitness})")
                println(simplifier.simplify(res.best as BaseProgram<Double>))

                println("\nStats (last run only):\n")

                for ((k, v) in res.statistics.last().data) {
                    println("$k = $v")
                }
                println("")
            }

            val avgBestFitness = solution.result.evaluations.map { eval ->
                eval.best.fitness
            }.sum() / solution.result.evaluations.size

            println("Average best fitness: $avgBestFitness")
        }
    }
}

