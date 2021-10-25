package nz.co.jedsimson.lgp.lib.base

import nz.co.jedsimson.lgp.core.environment.*
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.config.ConfigurationLoader
import nz.co.jedsimson.lgp.core.environment.config.JsonConfigurationLoader
import nz.co.jedsimson.lgp.core.environment.constants.ConstantLoader
import nz.co.jedsimson.lgp.core.environment.dataset.Dataset
import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.environment.operations.DefaultOperationLoader
import nz.co.jedsimson.lgp.core.evolution.*
import nz.co.jedsimson.lgp.core.evolution.fitness.*
import nz.co.jedsimson.lgp.core.evolution.model.EvolutionModel
import nz.co.jedsimson.lgp.core.evolution.model.SteadyState
import nz.co.jedsimson.lgp.core.evolution.model.TestResult
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.ConstantMutationFunctions
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.MicroMutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover.LinearCrossover
import nz.co.jedsimson.lgp.core.evolution.operators.selection.TournamentSelection
import nz.co.jedsimson.lgp.core.evolution.training.DistributedTrainer
import nz.co.jedsimson.lgp.core.evolution.training.TrainingResult
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.modules.ModuleContainer
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.lib.generators.RandomInstructionGenerator
import nz.co.jedsimson.lgp.lib.generators.RandomProgramGenerator

/**
 * Parameters that can be given to configure a [BaseProblem].
 */
data class BaseProblemParameters(
    /**
     * The problem name.
     */
    val name: String,

    /**
     * A description of the problem.
     */
    val description: Description,

    /**
     * An optional JSON file name to read configuration from.
     */
    val configFilename: String? = null,

    /**
     * An optional [Configuration] instance to use.
     */
    val config: Configuration? = null,

    /**
     * A set of constants for the problem (default is `[-1.0, 0.0, 1.0]`).
     */
    val constants: List<Double> = listOf(-1.0, 0.0, 1.0),

    /**
     * A set of operations for the problem.
     *
     * Default operations provided are:
     * - [nz.co.jedsimson.lgp.lib.operations.Addition]
     * - [nz.co.jedsimson.lgp.lib.operations.Subtraction]
     * - [nz.co.jedsimson.lgp.lib.operations.Multiplication]
     * - [nz.co.jedsimson.lgp.lib.operations.Division].
     */
    val operationClassNames: List<String> = listOf(
        "nz.co.jedsimson.lgp.lib.operations.Addition",
        "nz.co.jedsimson.lgp.lib.operations.Subtraction",
        "nz.co.jedsimson.lgp.lib.operations.Multiplication",
        "nz.co.jedsimson.lgp.lib.operations.Division"
    ),

    /**
     * The default value to use for registers (default is `1.0`).
     */
    val defaultRegisterValue: Double = 1.0,

    /**
     * The fitness function to use (default is mean-squared error).
     */
    val fitnessFunction: SingleOutputFitnessFunction<Double> = FitnessFunctions.MSE,

    /**
     * The size of tournaments during evolution (default is `20`).
     */
    val tournamentSize: Int = 20,

    /**
     * The number of offspring to select during each generation (default is `10`).
     */
    val numberOfOffspring: Int = 10,

    /**
     * The maximum segment length for linear crossover (default is `6`).
     */
    val maximumSegmentLength: Int = 6,

    /**
     * The maximum crossover distance for linear crossover (default is `5`).
     */
    val maximumCrossoverDistance: Int = 5,

    /**
     * The maximum segment length difference for linear crossover (default is `3`).
     */
    val maximumSegmentLengthDifference: Int = 3,

    /**
     * The macro-mutation insertion rate (default is `0.67`).
     */
    val macroMutationInsertionRate: Double = 0.67,

    /**
     * The macro-mutation deletion rate (default is `0.33`).
     */
    val macroMutationDeletionRate: Double = 0.33,

    /**
     * The register micro-mutation rate (default is `0.4`).
     */
    val microRegisterMutationRate: Double = 0.4,

    /**
     * The operation micro-mutation rate (default is `0.4`).
     */
    val microOperationMutationRate: Double = 0.4,

    /**
     * An optional seed for the random state.
     */
    val randomStateSeed: Long? = null,

    /**
     * The number of runs to perform (default is `10`).
     */
    val runs: Int = 10
)

/**
 * Exception given when there is some problem with the setup of a [BaseProblem].
 */
class BaseProblemException(message: String) : Exception(message)

/**
 * Encapsulates the information given from a call to [BaseProblem.test].
 */
data class BaseProblemTestResult(val testResult: TestResult<Double, Outputs.Single<Double>>, val testFitness: Double)

/**
 * A basic skeleton for a problem setup with commonly used components and modules.
 *
 * This class is supposed to streamline the process of setting up a problem by providing
 * a set of reasonable defaults and a base set of parameters that can be tweaked.
 */
class BaseProblem(val params: BaseProblemParameters) : Problem<Double, Outputs.Single<Double>, Targets.Single<Double>>() {
    // Unpack values from parameters.
    override val name = params.name

    override val description = params.description

    override val configLoader = when {
        params.configFilename == null && params.config == null -> {
            throw BaseProblemException(
                "Either the filename of a JSON file with configuration information" +
                "or a custom configuration object needs to be provided."
            )
        }
        params.configFilename != null -> JsonConfigurationLoader(params.configFilename)
        else -> {
            // Fallback to default configuration
            object : ConfigurationLoader {
                override val information = ModuleInformation(
                    "Default configuration loader."
                )

                override fun load(): Configuration {
                    return params.config!!
                }
            }
        }
    }

    override val constantLoader = object : ConstantLoader<Double> {
        override val information = ModuleInformation(
            description = "A base constant loader that provides a list of doubles."
        )

        override fun load(): List<Double> {
            return params.constants
        }
    }

    override val operationLoader = DefaultOperationLoader<Double>(params.operationClassNames)

    override val defaultValueProvider = DefaultValueProviders.constantValueProvider(params.defaultRegisterValue)

    override val fitnessFunctionProvider = { params.fitnessFunction }

    override val registeredModules = ModuleContainer<Double, Outputs.Single<Double>, Targets.Single<Double>>(
        modules = mutableMapOf(
            CoreModuleType.InstructionGenerator to { environment ->
                RandomInstructionGenerator(environment)
            },
            CoreModuleType.ProgramGenerator to { environment ->
                RandomProgramGenerator(
                    environment,
                    sentinelTrueValue = 1.0,
                    outputRegisterIndices = listOf(0),
                    outputResolver = BaseProgramOutputResolvers.singleOutput()
                )
            },
            CoreModuleType.SelectionOperator to { environment ->
                TournamentSelection(
                    environment,
                    tournamentSize = params.tournamentSize,
                    numberOfOffspring = params.numberOfOffspring)
            },
            CoreModuleType.RecombinationOperator to { environment ->
                LinearCrossover(
                    environment,
                    maximumSegmentLength = params.maximumSegmentLength,
                    maximumCrossoverDistance = params.maximumCrossoverDistance,
                    maximumSegmentLengthDifference = params.maximumSegmentLengthDifference
                )
            },
            CoreModuleType.MacroMutationOperator to { environment ->
                MacroMutationOperator(
                    environment,
                    insertionRate = params.macroMutationInsertionRate,
                    deletionRate = params.macroMutationDeletionRate
                )
            },
            CoreModuleType.MicroMutationOperator to { environment ->
                MicroMutationOperator(
                    environment,
                    registerMutationRate = params.microRegisterMutationRate,
                    operatorMutationRate = params.microOperationMutationRate,
                    constantMutationFunc = ConstantMutationFunctions.randomGaussianNoise(
                        environment.randomState
                    )
                )
            }
        )
    )

    private var bestTrainingModel: EvolutionModel<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null

    override fun initialiseEnvironment() {
        val specification = EnvironmentSpecification(
            configurationLoader = this.configLoader,
            constantLoader = this.constantLoader,
            operationLoader = this.operationLoader,
            defaultValueProvider = this.defaultValueProvider,
            fitnessFunctionProvider = this.fitnessFunctionProvider,
            randomStateSeed = this.params.randomStateSeed
        )

        this.environment = Environment(specification)

        this.environment.registerModules(this.registeredModules)
    }

    override fun initialiseModel() {
        this.model = SteadyState(this.environment)
    }

    override fun solve(): Solution<Double> {
        throw BaseProblemException(
            "BaseProblem can't be called directly to solve a problem. " +
            "Use the train and test methods to train and test the model."
        )
    }

    fun train(dataset: Dataset<Double, Targets.Single<Double>>): TrainingResult<Double, Outputs.Single<Double>, Targets.Single<Double>> {
        try {
            val trainer = DistributedTrainer(environment, model, runs = this.params.runs)
            val trainingResult = trainer.train(dataset)

            // Choose the best model during training and save it so that it can be applied
            // during testing.
            this.bestTrainingModel = trainingResult.evaluations
                .zip(trainingResult.models)
                .sortedBy { (evaluation, _) -> evaluation.best.fitness }
                .map      { (_, model)      -> model }
                .first()

            return trainingResult
        } catch (ex: UninitializedPropertyAccessException) {
            // The initialisation routines haven't been run.
            throw ProblemNotInitialisedException(
                "The initialisation routines for this problem must be run before it can be solved."
            )
        }
    }

    fun test(dataset: Dataset<Double, Targets.Single<Double>>): BaseProblemTestResult {
        if (this.bestTrainingModel == null) {
            throw BaseProblemException(
                "This problem has not had any models trained. The problem must be trained on a dataset " +
                "before testing can be performed."
            )
        }

        val testResult = this.bestTrainingModel!!.test(dataset)

        val testFitness = this.fitnessFunctionProvider()(
            testResult.predicted,
            dataset.inputs.zip(dataset.outputs).map { (features, target) ->
                FitnessCase(features, target)
            }
        )

        return BaseProblemTestResult(testResult, testFitness)
    }
}
