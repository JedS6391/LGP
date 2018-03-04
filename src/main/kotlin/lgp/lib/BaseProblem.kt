package lgp.lib

import lgp.core.environment.*
import lgp.core.environment.config.Configuration
import lgp.core.environment.config.ConfigurationLoader
import lgp.core.environment.config.JsonConfigurationLoader
import lgp.core.environment.constants.ConstantLoader
import lgp.core.environment.dataset.Dataset
import lgp.core.environment.operations.DefaultOperationLoader
import lgp.core.evolution.*
import lgp.core.evolution.fitness.FitnessCase
import lgp.core.evolution.fitness.FitnessFunction
import lgp.core.evolution.fitness.FitnessFunctions
import lgp.core.evolution.population.*
import lgp.core.modules.ModuleInformation

/**
 * Parameters that can be given to configure a ``BaseProblem``.
 */
data class BaseProblemParameters(
        val name: String,
        val description: Description,
        val configFilename: String? = null,
        val config: Configuration? = null,
        val constants: List<Double> = listOf(-1.0, 0.0, 1.0),
        val operationClassNames: List<String> = listOf(
                "lgp.lib.operations.Addition",
                "lgp.lib.operations.Subtraction",
                "lgp.lib.operations.Multiplication",
                "lgp.lib.operations.Division"
        ),
        val defaultRegisterValue: Double = 1.0,
        val fitnessFunction: FitnessFunction<Double> = FitnessFunctions.MSE,
        val tournamentSize: Int = 20,
        val maximumSegmentLength: Int = 6,
        val maximumCrossoverDistance: Int = 5,
        val maximumSegmentLengthDifference: Int = 3,
        val macroMutationInsertionRate: Double = 0.67,
        val macroMutationDeletionRate: Double = 0.33,
        val microRegisterMutationRate: Double = 0.4,
        val microOperationMutationRate: Double = 0.4,
        val randomStateSeed: Long? = null,
        val runs: Int = 10
)

/**
 * Exception given when there is some problem with the setup of a ``BaseProblem``.
 */
class BaseProblemException(message: String) : Exception(message)

/**
 * Encapsulates the information given from a call to [BaseProblem.test].
 */
data class BaseProblemTestResult(val testResult: TestResult<Double>, val testFitness: Double)

/**
 * A basic skeleton for a problem setup with commonly used components and modules.
 *
 * This class is supposed to streamline the process of setting up a problem by providing
 * a set of reasonable defaults and a base set of parameters that can be tweaked.
 */
class BaseProblem(val params: BaseProblemParameters) : Problem<Double>() {
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

    override val fitnessFunction = params.fitnessFunction

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
                        TournamentSelection(environment, tournamentSize = params.tournamentSize)
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
                                constantMutationFunc = ConstantMutationFunctions.randomGaussianNoise(environment)
                        )
                    }
            )
    )

    var bestTrainingModel: EvolutionModel<Double>? = null

    override fun initialiseEnvironment() {
        this.environment = Environment(
                this.configLoader,
                this.constantLoader,
                this.operationLoader,
                this.defaultValueProvider,
                this.fitnessFunction,
                randomStateSeed = this.params.randomStateSeed
        )

        this.environment.registerModules(this.registeredModules)
    }

    override fun initialiseModel() {
        this.model = Models.SteadyState(this.environment)
    }

    override fun solve(): Solution<Double> {
        throw BaseProblemException(
                "BaseProblem can't be called directly to solve a problem. " +
                "Use the train and test methods to train and test the model."
        )
    }

    fun train(dataset: Dataset<Double>): TrainingResult<Double> {
        try {
            val trainer = Trainers.DistributedTrainer(environment, model, runs = this.params.runs)
            val trainingResult = trainer.train(dataset)

            // Choose the best model during training and save it so that it can be applied
            // during testing.
            this.bestTrainingModel = trainingResult.evaluations.zip(trainingResult.models)
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

    fun test(dataset: Dataset<Double>): BaseProblemTestResult {
        if (this.bestTrainingModel == null) {
            throw BaseProblemException(
                    "This problem has not had any models trained. The problem must be trained on a dataset " +
                    "before testing can be performed."
            )
        }

        val testResult = this.bestTrainingModel!!.test(dataset)

        val testFitness = this.fitnessFunction(
                testResult.predicted,
                dataset.inputs.zip(dataset.outputs).map { (features, target) ->
                    FitnessCase(features, target)
                }
        )

        return BaseProblemTestResult(testResult, testFitness)
    }
}
