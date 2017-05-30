package lgp.examples

import lgp.core.environment.*
import lgp.core.environment.config.Config
import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.constants.GenericConstantLoader
import lgp.core.environment.dataset.*
import lgp.core.environment.operations.DefaultOperationLoader
import lgp.core.evolution.*
import lgp.core.evolution.fitness.FitnessFunction
import lgp.core.evolution.fitness.FitnessFunctions
import lgp.core.evolution.population.*
import lgp.core.modules.ModuleInformation
import lgp.lib.BaseInstructionGenerator
import lgp.lib.BaseProgram
import lgp.lib.BaseProgramGenerator
import lgp.lib.BaseProgramSimplifier
import java.util.*

data class SinPolySolution(
        override val problem: String,
        val result: TrainingResult<Double>
) : Solution<Double>

class SinPolyProblem : Problem<Double>() {
    override val name = "SinPoly."

    override val description = Description("f(x) = sin(x) * x + 5\n\trange = Uniform[-5:5]")

    override val configLoader = object : ConfigLoader {
        override val information = ModuleInformation("Overrides default config for this problem.")

        override fun load(): Config {
            val config = Config()

            config.initialMinimumProgramLength = 30
            config.initialMaximumProgramLength = 60
            config.minimumProgramLength = 30
            config.maximumProgramLength = 400
            config.operations = listOf(
                    "lgp.lib.operations.Addition",
                    "lgp.lib.operations.Subtraction",
                    "lgp.lib.operations.Multiplication",
                    "lgp.lib.operations.Division",
                    "lgp.lib.operations.Exponent"
            )
            config.constantsRate = 0.5
            config.constants = listOf(
                    "1.0", "2.0", "3.0", "4.0", "5.0", "6.0", "7.0", "8.0", "9.0"
            )
            config.numCalculationRegisters = 4
            config.populationSize = 1000
            config.generations = 1000
            config.numFeatures = 1
            config.microMutationRate = 0.25
            config.macroMutationRate = 0.75
            config.numOffspring = 4
            config.crossoverRate = 0.7

            return config
        }
    }

    private val config = this.configLoader.load()

    override val constantLoader = GenericConstantLoader(
            constants = config.constants,
            parseFunction = String::toDouble
    )

     val datasetLoader = object : DatasetLoader<Double> {
        // f(x) = sin(x) * x + 5
        val func = { x: Double -> Math.sin(x) * x + 5.0 }
        val gen = UniformlyDistributedGenerator()

        override val information = ModuleInformation("Generates uniformly distributed samples in the range [-5:5].")

        override fun load(): Dataset<Double> {
            val xs = gen.generate(100, -5.0, 5.0).map { v ->
                Sample(
                    listOf(Feature(name = "x", value = v))
                )
            }.toList()

            val ys = xs.map { x ->
                this.func(x.feature("x").value)
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

    override val fitnessFunction: FitnessFunction<Double> = FitnessFunctions.SSE()

    override val registeredModules = ModuleContainer(
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
                                registerMutationRate = 0.3,
                                operatorMutationRate = 0.4,
                                constantMutationFunc = { v -> v + (Random().nextGaussian() * 1) }
                        )
                    }
            )
    )

    override fun initialiseEnvironment() {
        this.environment = Environment(
                this.configLoader,
                this.constantLoader,
                this.operationLoader,
                this.defaultValueProvider,
                this.fitnessFunction
        )

        this.environment.registerModules(this.registeredModules)
    }

    override fun initialiseModel() {
        this.model = Models.SteadyState(this.environment)
    }

    override fun solve(): SinPolySolution {
        try {
            val runner = Trainers.DistributedTrainer(environment, model, runs = 10)
            val result = runner.train(this.datasetLoader.load())

            return SinPolySolution(this.name, result)
        } catch (ex: UninitializedPropertyAccessException) {
            // The initialisation routines haven't been run.
            throw ProblemNotInitialisedException(
                    "The initialisation routines for this problem must be run before it can be solved."
            )
        }
    }
}

class SinPoly {
    companion object Main {
        @JvmStatic fun main(args: Array<String>) {
            val problem = SinPolyProblem()
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

