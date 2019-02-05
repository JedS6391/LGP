package nz.co.jedsimson.lgp.examples

import nz.co.jedsimson.lgp.core.environment.*
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.config.ConfigurationLoader
import nz.co.jedsimson.lgp.core.environment.constants.GenericConstantLoader
import nz.co.jedsimson.lgp.core.environment.dataset.*
import nz.co.jedsimson.lgp.core.environment.operations.DefaultOperationLoader
import nz.co.jedsimson.lgp.core.evolution.*
import nz.co.jedsimson.lgp.core.evolution.fitness.*
import nz.co.jedsimson.lgp.core.evolution.model.Models
import nz.co.jedsimson.lgp.core.evolution.operators.*
import nz.co.jedsimson.lgp.core.evolution.training.DistributedTrainer
import nz.co.jedsimson.lgp.core.evolution.training.TrainingResult
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.lib.*

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class IrisSolution(
        override val problem: String,
        val result: TrainingResult<Double, Outputs.Single<Double>>
) : Solution<Double>

class IrisProblem(val datasetStream: InputStream) : Problem<Double, Outputs.Single<Double>>() {
    override val name = "Iris Classification."

    override val description = Description(
            "Classify each instance into one of three species based on four features.\n" +
                    "\tfeatures: [sepal length, sepal width, petal length, petal width]" +
                    "\tclasses: [Iris-setosa, Iris-versicolor, Iris-virginica]" +
                    "\tnotes: There are 50 samples from each of the 3 species (150 samples in total)."
    )

    override val configLoader = object : ConfigurationLoader {
        override val information = ModuleInformation("Overrides default configuration for this problem.")

        override fun load(): Configuration {
            val config = Configuration()

            config.initialMinimumProgramLength = 10
            config.initialMaximumProgramLength = 30
            config.minimumProgramLength = 10
            config.maximumProgramLength = 200
            config.operations = listOf(
                    "nz.co.jedsimson.lgp.lib.operations.Addition",
                    "nz.co.jedsimson.lgp.lib.operations.Subtraction",
                    "nz.co.jedsimson.lgp.lib.operations.Multiplication",
                    "nz.co.jedsimson.lgp.lib.operations.Division",
                    "nz.co.jedsimson.lgp.lib.operations.IfGreater",
                    "nz.co.jedsimson.lgp.lib.operations.IfLessThanOrEqualTo"
            )
            config.constantsRate = 0.5
            config.constants = listOf("0.0", "1.0", "2.0", "3.0", "4.0", "5.0", "6.0", "7.0", "8.0", "9.0")
            config.numCalculationRegisters = 2
            config.populationSize = 2000
            config.generations = 1000
            config.numFeatures = 4
            config.microMutationRate = 0.25
            config.macroMutationRate = 0.75
            config.crossoverRate = 0.75
            config.branchInitialisationRate = 0.1
            config.numOffspring = 10

            return config
        }
    }

    private val config = this.configLoader.load()

    override val constantLoader = GenericConstantLoader(
            constants = config.constants,
            parseFunction = String::toDouble
    )

    val targetLabels = setOf("Iris-setosa", "Iris-versicolor", "Iris-virginica")
    val featureIndices = 0..3
    val targetIndex = 4

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
                val target = row[targetIndex]

                Targets.Single(targetLabels.indexOf(target).toDouble())
            }
    )

    override val operationLoader = DefaultOperationLoader<Double>(
            operationNames = config.operations
    )

    override val defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)

    override val fitnessFunctionProvider = {
        FitnessFunctions.thresholdCE(0.5)
    }

    override val registeredModules = ModuleContainer<Double, Outputs.Single<Double>>(
            modules = mutableMapOf(
                    CoreModuleType.InstructionGenerator to { environment ->
                        BaseInstructionGenerator(environment)
                    },
                    CoreModuleType.ProgramGenerator to { environment ->
                        BaseProgramGenerator(
                            environment,
                            sentinelTrueValue = 1.0,
                            outputRegisterIndices = listOf(0),
                            outputResolver = BaseProgramOutputResolvers.singleOutput()
                        )
                    },
                    CoreModuleType.SelectionOperator to { environment ->
                        TournamentSelection(environment, tournamentSize = 4)
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
                            operatorMutationRate = 0.0,
                            constantMutationFunc = ConstantMutationFunctions.randomGaussianNoise(
                                environment.randomState
                            )
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
                this.fitnessFunctionProvider
        )

        this.environment.registerModules(this.registeredModules)
    }

    override fun initialiseModel() {
        this.model = Models.SteadyState(this.environment)
    }

    override fun solve(): IrisSolution {
        try {
            val runner = DistributedTrainer(environment, model, runs = 5)
            val result = runner.train(this.datasetLoader.load())

            return IrisSolution(this.name, result)
        } catch (ex: UninitializedPropertyAccessException) {
            // The initialisation routines haven't been run.
            throw ProblemNotInitialisedException(
                    "The initialisation routines for this problem must be run before it can be solved."
            )
        }
    }
}

class Iris {
    companion object Main {

        private val datasetStream = this::class.java.classLoader.getResourceAsStream("datasets/iris.csv")

        @JvmStatic fun main(args: Array<String>) {
            val problem = IrisProblem(datasetStream)
            problem.initialiseEnvironment()
            problem.initialiseModel()
            val solution = problem.solve()
            val simplifier = BaseProgramSimplifier<Double, Outputs.Single<Double>>()

            solution.result.evaluations.forEachIndexed { run, res ->
                println("Run ${run + 1} (best fitness = ${res.best.fitness})")
                println(simplifier.simplify(res.best as BaseProgram<Double, Outputs.Single<Double>>))
                println("\nStats (last run only):\n")

                for ((k, v) in res.statistics.last().data) {
                    println("$k = $v")
                }
                println("")
            }
        }
    }
}

