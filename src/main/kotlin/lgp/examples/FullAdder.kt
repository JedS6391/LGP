package lgp.examples

import lgp.core.environment.CoreModuleType
import lgp.core.environment.DefaultValueProviders
import lgp.core.environment.Environment
import lgp.core.environment.ModuleContainer
import lgp.core.environment.config.Configuration
import lgp.core.environment.config.ConfigurationLoader
import lgp.core.environment.constants.DoubleConstantLoader
import lgp.core.environment.dataset.*
import lgp.core.environment.operations.DefaultOperationLoader
import lgp.core.evolution.*
import lgp.core.evolution.fitness.*
import lgp.core.evolution.model.Models
import lgp.core.evolution.operators.*
import lgp.core.evolution.training.SequentialTrainer
import lgp.core.evolution.training.TrainingResult
import lgp.core.modules.ModuleInformation
import lgp.lib.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

val match: MultipleOutputFitnessFunction<Double> = object : MultipleOutputFitnessFunction<Double>() {

    override fun fitness(outputs: List<Outputs.Multiple<Double>>, cases: List<FitnessCase<Double>>): Double {
        val mismatches = cases.zip(outputs).filter { (case, actual) ->
            val expected = (case.target as Targets.Multiple<Double>).values

            actual.values != expected
        }.count()

        return mismatches.toDouble()
    }
}

/**
 * Defines what a solution for this problem looks like.
 */
class FullAdderExperimentSolution(
    override val problem: String,
    val result: TrainingResult<Double>,
    val dataset: Dataset<Double>
) : Solution<Double>

/**
 * Defines the problem.
 */
class FullAdderExperiment(
    val datasetStream: InputStream
) : Problem<Double>() {

    // 1. Give the problem a name and simple description.
    override val name = "Custom Fitness Functions Experiment"
    override val description = Description(
        "An example custom fitness functions problem definition for the LGP tutorial."
    )

    // 2. Define the necessary dependencies to build a problem.
    override val configLoader = object : ConfigurationLoader {
        override val information = ModuleInformation("Overrides default configuration for this problem.")

        override fun load(): Configuration {
            val config = Configuration()

            config.initialMinimumProgramLength = 10
            config.initialMaximumProgramLength = 100
            config.minimumProgramLength = 5
            config.maximumProgramLength = 500
            config.operations = listOf(
                "lgp.lib.operations.And",
                "lgp.lib.operations.Or",
                "lgp.lib.operations.Xor"
            )
            config.constantsRate = 0.0
            config.numCalculationRegisters = 5
            config.populationSize = 10000
            config.generations = 1000
            config.numFeatures = 3
            config.microMutationRate = 0.8
            config.macroMutationRate = 0.8
            config.crossoverRate = 0.8
            config.branchInitialisationRate = 0.0
            config.numOffspring = 200

            return config
        }
    }
    // To prevent reloading configuration in this module.
    private val configuration = this.configLoader.load()
    // Load constants from the configuration as double values.
    override val constantLoader = DoubleConstantLoader(constants = this.configuration.constants)
    // Load operations from the configuration (operations are resolved using their class name).
    override val operationLoader = DefaultOperationLoader<Double>(operationNames = this.configuration.operations)
    // Set the default value of any registers to 1.0.
    override val defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)
    // Use the mean-squared error fitness function.
    override val fitnessFunctionProvider = { match as FitnessFunction<Double, Output<Double>> }
    // Define the modules to be used for the core evolutionary operations.
    override val registeredModules = ModuleContainer<Double>(modules = mutableMapOf(
        // Generate instructions using the built-in instruction generator.
        CoreModuleType.InstructionGenerator to { environment ->
            BaseInstructionGenerator(environment)
        },
        // Generate programs using the built-in programs generator.
        CoreModuleType.ProgramGenerator to { environment ->
            BaseProgramGenerator(
                environment,
                sentinelTrueValue = 1.0, // Determines the value that represents a boolean "true".
                outputRegisterIndices = listOf(6, 7) // Two program outputs
            )
        },
        // Perform selection using the built-in tournament selection.
        CoreModuleType.SelectionOperator to { environment ->
            TournamentSelection(environment, tournamentSize = 2)
        },
        // Combine individuals using the linear crossover operator.
        CoreModuleType.RecombinationOperator to { environment ->
            LinearCrossover(
                environment,
                maximumSegmentLength = 6,
                maximumCrossoverDistance = 5,
                maximumSegmentLengthDifference = 3
            )
        },
        // Use the built-in macro-mutation operator.
        CoreModuleType.MacroMutationOperator to { environment ->
            MacroMutationOperator(
                environment,
                insertionRate = 0.67,
                deletionRate = 0.33
            )
        },
        // Use the built-in micro-mutation operator.
        CoreModuleType.MicroMutationOperator to { environment ->
            MicroMutationOperator(
                environment,
                registerMutationRate = 0.5,
                operatorMutationRate = 0.5,
                constantMutationFunc = ConstantMutationFunctions.randomGaussianNoise(this.environment)
            )
        },
        // Use the multiple-output fitness context -- meaning that program fitness will be evaluated
        // using multiple program outputs and the fitness function specified earlier in this definition.
        CoreModuleType.FitnessContext to { environment ->
            MultipleOutputFitnessContext(environment)
        }
    )
    )

    // 3. Describe how to initialise the problem's environment.
    override fun initialiseEnvironment() {
        this.environment = Environment(
            this.configLoader,
            this.constantLoader,
            this.operationLoader,
            this.defaultValueProvider,
            this.fitnessFunctionProvider,
            // Collect results and output them to the file "result.csv".
            ResultAggregators.BufferedResultAggregator(
                ResultOutputProviders.CsvResultOutputProvider(
                    filename = "results.csv"
                )
            )
        )

        this.environment.registerModules(this.registeredModules)
    }

    // 4. Specify which evolution model should be used to solve the problem.
    override fun initialiseModel() {
        this.model = Models.SteadyState(this.environment)
    }

    // 5. Describe the steps required to solve the problem using the definition given above.
    override fun solve(): FullAdderExperimentSolution {
        // Indices of the feature variables
        // a, b, c_in
        val featureIndices = 0 until 3
        // Indices of the target variables.
        // c_out, s
        val targetIndices = 3 until 5

        // Load the data set
        val datasetLoader = CsvDatasetLoader(
            reader = BufferedReader(
                // Load from the resource file.
                InputStreamReader(this.datasetStream)
            ),
            featureParseFunction = { header: Header, row: Row ->
                val features = row.zip(header)
                    .slice(featureIndices)
                    .map { (featureValue, featureName) ->

                        Feature(
                            name = featureName,
                            value = featureValue.toDouble()
                        )
                    }

                Sample(features)
            },
            targetParseFunction = { _: Header, row: Row ->
                val targets = row.slice(targetIndices).map { target -> target.toDouble() }

                Targets.Multiple(targets)
            }
        )

        val dataset = datasetLoader.load()

        // Print details about the data set and configuration before beginning the evolutionary process.
        println("\nDataset details:")
        println("numFeatures = ${dataset.numFeatures()}, numSamples = ${dataset.numSamples()}")
        println()
        println(this.configuration)

        // Train using the built-in sequential trainer.
        val trainer = SequentialTrainer(
            this.environment,
            this.model,
            runs = this.configuration.numberOfRuns
        )

        // Return a solution the problem.
        return FullAdderExperimentSolution(
            problem = this.name,
            result = trainer.train(dataset),
            dataset = dataset
        )
    }
}

class FullAdder {
    companion object Main {

        private val datasetStream = this::class.java.classLoader.getResourceAsStream("datasets/full-adder.csv")

        @JvmStatic fun main(args: Array<String>) {
            val problem = FullAdderExperiment(datasetStream)
            problem.initialiseEnvironment()
            problem.initialiseModel()
            val solution = problem.solve()
            val simplifier = BaseProgramSimplifier<Double>()
            val programTranslator = BaseProgramTranslator<Double>(includeMainFunction = true)

            solution.result.evaluations.forEachIndexed { run, res ->
                println("Run ${run + 1} (best fitness = ${res.best.fitness})")
                println(simplifier.simplify(res.best as BaseProgram<Double>))
                println("\nStats (last run only):\n")

                for ((k, v) in res.statistics.last().data) {
                    println("$k = $v")
                }
                println("")

                println(programTranslator.translate(res.best))
            }
        }
    }
}
