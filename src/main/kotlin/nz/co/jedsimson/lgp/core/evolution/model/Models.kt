package nz.co.jedsimson.lgp.core.evolution.model

import nz.co.jedsimson.lgp.core.environment.Environment
import nz.co.jedsimson.lgp.core.environment.dataset.Dataset
import nz.co.jedsimson.lgp.core.evolution.ExportableResult
import nz.co.jedsimson.lgp.core.evolution.fitness.Evaluation
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessEvaluator
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.evolution.operators.*
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.ProgramGenerator

import java.util.Random
import kotlin.concurrent.thread
import kotlin.streams.toList

/**
 * An [ExportableResult] implementation that represents evolution statistics for a particular run.
 *
 * Because evolution statistics are collected on a per-generation basis, this type of exportable result
 * collects data on each generation for each run.
 *
 * @param run The run this result relates to.
 * @param statistics Evolution statistics for a particular generation.
 */
class RunBasedExportableResult<T>(
    val run: Int,
    private val statistics: EvolutionStatistics
) : ExportableResult<T> {

    /**
     * Exports this result as a mapping of statistic names to statistic values.
     */
    override fun export(): List<Pair<String, String>> {
        val out = mutableListOf(
                Pair("run", this.run.toString())
        )

        out.addAll(statistics.data.map { (name, value) ->
            Pair(name, value.toString())
        })

        return out
    }
}

/**
 * A collection of built-in evolution models.
 */
object Models {

    /**
     * A model for evolution using a steady-state algorithm.
     *
     * For more information, see Algorithm 2.1 (LGP Algorithm) from Linear Genetic Programming
     * (Brameier, M., Banzhaf, W. 2001).
     */
    class SteadyState<TProgram, TOutput : Output<TProgram>>(
        environment: Environment<TProgram, TOutput>
    ) : EvolutionModel<TProgram, TOutput>(environment) {

        private val select: SelectionOperator<TProgram, TOutput> = this.environment.registeredModule(
                CoreModuleType.SelectionOperator
        )

        private val combine: RecombinationOperator<TProgram, TOutput> = this.environment.registeredModule(
                CoreModuleType.RecombinationOperator
        )

        private val microMutate: MutationOperator<TProgram, TOutput> = this.environment.registeredModule(
                CoreModuleType.MicroMutationOperator
        )

        private val macroMutate: MutationOperator<TProgram, TOutput> = this.environment.registeredModule(
                CoreModuleType.MacroMutationOperator
        )

        private val fitnessEvaluator: FitnessEvaluator<TProgram, TOutput> = FitnessEvaluator()

        lateinit var individuals: MutableList<Program<TProgram, TOutput>>

        lateinit var bestProgram: Program<TProgram, TOutput>

        private fun initialise() {
            val programGenerator: ProgramGenerator<TProgram, TOutput> = this.environment.registeredModule(CoreModuleType.ProgramGenerator)

            this.individuals = programGenerator.next()
                    .take(this.environment.configuration.populationSize)
                    .toMutableList()
        }

        override fun train(dataset: Dataset<TProgram>): EvolutionResult<TProgram, TOutput> {
            val rg = this.environment.randomState

            // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
            // 1. Initialise a population of random programs
            this.initialise()

            // Determine the initial fitness of the individuals in the population
            val initialEvaluations = this.individuals.map { individual ->
                this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
            }.toList()

            var best = initialEvaluations.sortedBy(Evaluation<TProgram, TOutput>::fitness).first()
            this.bestProgram = best.individual

            val statistics = mutableListOf<EvolutionStatistics>()

            (0 until this.environment.configuration.generations).forEach { gen ->
                // Stop early whenever we can.
                if (best.fitness <= this.environment.configuration.stoppingCriterion) {
                    // Make sure to add at least one set of statistics.
                    statistics.add(this.statistics(gen, best))

                    this.bestProgram = best.individual

                    return EvolutionResult(best.individual, this.individuals, statistics)
                }

                val children = this.select.select(this.individuals)

                children.pairwise().map { (mother, father) ->
                    // Combine mother and father with some prob.
                    if (rg.nextDouble() < this.environment.configuration.crossoverRate) {
                        this.combine.combine(mother, father)
                    }

                    // Mutate mother or father (or both) with some prob.
                    if (rg.nextDouble() < this.environment.configuration.microMutationRate) {
                        this.microMutate.mutate(mother)
                    } else if (rg.nextDouble() < this.environment.configuration.macroMutationRate) {
                        this.macroMutate.mutate(mother)
                    }

                    if (rg.nextDouble() < this.environment.configuration.microMutationRate) {
                        this.microMutate.mutate(father)
                    } else if (rg.nextDouble() < this.environment.configuration.macroMutationRate) {
                        this.macroMutate.mutate(father)
                    }
                }

                // TODO: Do validation step
                val evaluations = children.map { individual ->
                    this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
                }.sortedBy(Evaluation<TProgram, TOutput>::fitness)

                val bestChild = evaluations.first()

                best = if (bestChild.fitness < best.fitness) bestChild else best
                this.bestProgram = best.individual

                // The children are copies of individuals in the population, so add the copies
                // to the population.
                this.individuals.addAll(children)

                statistics.add(this.statistics(gen, best))
            }

            this.bestProgram = best.individual

            return EvolutionResult(best.individual, this.individuals, statistics)
        }

        private fun statistics(generation: Int, best: Evaluation<TProgram, TOutput>): EvolutionStatistics {
            var meanFitness = 0.0
            var meanProgramLength = 0.0
            var meanEffectiveProgramLength = 0.0
            val bestFitness = best.fitness

            this.individuals.forEach { individual ->
                meanFitness += individual.fitness
                meanProgramLength += individual.instructions.size
                meanEffectiveProgramLength += individual.effectiveInstructions.size
            }

            meanFitness /= this.individuals.size
            meanProgramLength /= this.individuals.size
            meanEffectiveProgramLength /= this.individuals.size

            // Use the mean that we've already calculated.
            val standardDeviation = this.individuals.map(Program<TProgram, TOutput>::fitness).standardDeviation(meanFitness)

            return EvolutionStatistics(
                    data = mapOf(
                            "generation" to generation,
                            "bestFitness" to bestFitness,
                            "meanFitness" to meanFitness,
                            "standardDeviationFitness" to standardDeviation,
                            "meanProgramLength" to meanProgramLength,
                            "meanEffectiveProgramLength" to meanEffectiveProgramLength
                    )
            )
        }

        override fun test(dataset: Dataset<TProgram>): TestResult<TProgram, TOutput> {
            this.bestProgram.findEffectiveProgram()

            val outputs = dataset.inputs.map { features ->
                // Reset registers.
                this.bestProgram.registers.reset()

                // Load the features
                this.bestProgram.registers.writeInstance(features)

                // Run the program...
                this.bestProgram.execute()

                // ... and gather a result from the programs specified output registers.
                this.bestProgram.output()
            }

            return TestResult(
                    predicted = outputs,
                    expected = dataset.outputs
            )
        }

        override val information = ModuleInformation("Algorithm 2.1 (LGP Algorithm)")

        override fun copy(): SteadyState<TProgram, TOutput> {
            return SteadyState(this.environment)
        }

        override fun deepCopy(): EvolutionModel<TProgram, TOutput> {
            return SteadyState(this.environment.copy())
        }
    }

    /**
     * A model for evolution using a steady-state algorithm. The evaluation and mutation processes are
     * parallelised in a master-slave based technique.
     */
    class MasterSlave<TProgram, TOutput : Output<TProgram>>(
        environment: Environment<TProgram, TOutput>
    ) : EvolutionModel<TProgram, TOutput>(environment) {

        private val select: SelectionOperator<TProgram, TOutput> = this.environment.registeredModule(
                CoreModuleType.SelectionOperator
        )

        private val combine: RecombinationOperator<TProgram, TOutput> = this.environment.registeredModule(
                CoreModuleType.RecombinationOperator
        )

        private val microMutate: MutationOperator<TProgram, TOutput> = this.environment.registeredModule(
                CoreModuleType.MicroMutationOperator
        )

        private val macroMutate: MutationOperator<TProgram, TOutput> = this.environment.registeredModule(
                CoreModuleType.MacroMutationOperator
        )

        private val fitnessEvaluator: FitnessEvaluator<TProgram, TOutput> = FitnessEvaluator()

        lateinit var individuals: MutableList<Program<TProgram, TOutput>>

        lateinit var bestProgram: Program<TProgram, TOutput>

        private fun initialise() {
            val programGenerator: ProgramGenerator<TProgram, TOutput> = this.environment.registeredModule(
                CoreModuleType.ProgramGenerator
            )

            this.individuals = programGenerator.next()
                    .take(this.environment.configuration.populationSize)
                    .toMutableList()
        }

        override fun train(dataset: Dataset<TProgram>): EvolutionResult<TProgram, TOutput> {
            val rg = this.environment.randomState

            // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
            // 1. Initialise a population of random programs
            this.initialise()

            // Determine the initial fitness of the individuals in the population
            val initialEvaluations = this.individuals.pmap { individual ->
                this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
            }.toList()

            var best = initialEvaluations.sortedBy(Evaluation<TProgram, TOutput>::fitness).first()
            this.bestProgram = best.individual

            val statistics = mutableListOf<EvolutionStatistics>()

            (0 until this.environment.configuration.generations).forEach { gen ->
                // Stop early whenever we can.
                if (best.fitness <= this.environment.configuration.stoppingCriterion) {
                    // Make sure to add at least one set of statistics.
                    statistics.add(this.statistics(gen, best))

                    this.bestProgram = best.individual

                    return EvolutionResult(best.individual, this.individuals, statistics)
                }

                val children = this.select.select(this.individuals)

                children.pairwise().map { (mother, father) ->
                    // Combine mother and father with some prob.
                    if (rg.nextDouble() < this.environment.configuration.crossoverRate) {
                        this.combine.combine(mother, father)
                    }

                    // Mutate mother or father (or both) with some prob.
                    if (rg.nextDouble() < this.environment.configuration.microMutationRate) {
                        this.microMutate.mutate(mother)
                    } else if (rg.nextDouble() < this.environment.configuration.macroMutationRate) {
                        this.macroMutate.mutate(mother)
                    }

                    if (rg.nextDouble() < this.environment.configuration.microMutationRate) {
                        this.microMutate.mutate(father)
                    } else if (rg.nextDouble() < this.environment.configuration.macroMutationRate) {
                        this.macroMutate.mutate(father)
                    }
                }

                // TODO: Do validation step
                val evaluations = children.pmap { individual ->
                    this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
                }.sortedBy(Evaluation<TProgram, TOutput>::fitness)

                val bestChild = evaluations.first()

                best = if (bestChild.fitness < best.fitness) bestChild else best
                this.bestProgram = best.individual

                // The children are copies of individuals in the population, so add the copies
                // to the population.
                this.individuals.addAll(children)

                statistics.add(this.statistics(gen, best))
            }

            this.bestProgram = best.individual

            return EvolutionResult(best.individual, this.individuals, statistics)
        }

        private fun statistics(generation: Int, best: Evaluation<TProgram, TOutput>): EvolutionStatistics {
            var meanFitness = 0.0
            var meanProgramLength = 0.0
            var meanEffectiveProgramLength = 0.0
            val bestFitness = best.fitness

            this.individuals.forEach { individual ->
                meanFitness += individual.fitness
                meanProgramLength += individual.instructions.size
                meanEffectiveProgramLength += individual.effectiveInstructions.size
            }

            meanFitness /= this.individuals.size
            meanProgramLength /= this.individuals.size
            meanEffectiveProgramLength /= this.individuals.size

            // Use the mean that we've already calculated.
            val standardDeviation = this.individuals.map(
                Program<TProgram, TOutput>::fitness
            ).standardDeviation(meanFitness)

            return EvolutionStatistics(
                    data = mapOf(
                            "generation" to generation,
                            "bestFitness" to bestFitness,
                            "meanFitness" to meanFitness,
                            "standardDeviationFitness" to standardDeviation,
                            "meanProgramLength" to meanProgramLength,
                            "meanEffectiveProgramLength" to meanEffectiveProgramLength
                    )
            )
        }

        override fun test(dataset: Dataset<TProgram>): TestResult<TProgram, TOutput> {
            this.bestProgram.findEffectiveProgram()

            val outputs = dataset.inputs.map { features ->
                // Reset registers.
                this.bestProgram.registers.reset()

                // Load the features
                this.bestProgram.registers.writeInstance(features)

                // Run the program...
                this.bestProgram.execute()

                // ... and gather a result from the programs specified output registers.
                this.bestProgram.output()
            }

            return TestResult(
                predicted = outputs,
                expected = dataset.outputs
            )
        }

        override val information = ModuleInformation("Algorithm 2.1 (LGP Algorithm)")

        override fun copy(): MasterSlave<TProgram, TOutput> {
            return MasterSlave(this.environment)
        }

        override fun deepCopy(): EvolutionModel<TProgram, TOutput> {
            return MasterSlave(this.environment.copy())
        }
    }

    /**
     * A model for evolution using a island-migration algorithm.
     *
     * In an island-migration algorithm, the population is split into a number of islands,
     * and a fixed number of solutions migrate between the islands with some interval.
     *
     * This is done in an attempt to promote diversity in the population to prevent early
     * convergence on local optima.
     *
     * @param environment The environment that evolution is taking place in.
     * @param options Determines the configuration for the algorithm. See [IslandMigrationOptions] for more.
     */
    class IslandMigration<TProgram, TOutput : Output<TProgram>>(
            environment: Environment<TProgram, TOutput>,
            private val options: IslandMigrationOptions
    ) : EvolutionModel<TProgram, TOutput>(environment) {

        private val fitnessEvaluator: FitnessEvaluator<TProgram, TOutput> = FitnessEvaluator()

        private lateinit var islands: IslandGrid<TProgram, TOutput>

        /**
         * Controls the configuration of evolution when using an [IslandMigration] model.
         *
         * @property numIslands Determines the number of islands the population should be split into. At least 4 islands should be given.
         * @property migrationInterval Sets the interval that solutions are migrated with (i.e. how many generations).
         * @property migrationSize Controls how many solutions migrate between islands at each interval.
         */
        class IslandMigrationOptions {
            val numIslands: Int
            val migrationInterval: Int
            val migrationSize: Int

            constructor(numIslands: Int, migrationInterval: Int, migrationSize: Int) {
                if (numIslands < 4) {
                    throw Exception("At least 4 islands must be given.")
                }

                this.numIslands = numIslands
                this.migrationInterval = migrationInterval
                this.migrationSize = migrationSize
            }
        }

        /**
         * A collection of [Island] instances arranged in a grid.
         *
         * This encapsulation facilitates access to the individual islands when arranged in a grid
         * without having to worry about the grid arrangement.
         *
         * @suppress
         */
        private class IslandGrid<TProgram, TOutput : Output<TProgram>> {
            val islands: Array<Array<Island<TProgram, TOutput>>?>
            val numIslands: Int

            constructor(numIslands: Int, environment: Environment<TProgram, TOutput>, dataset: Dataset<TProgram>) {
                this.numIslands = numIslands

                // Compute grid dimensions and construct the grid of islands.
                // Each island has a reference to the environment and data set.
                var rows = Math.floor(Math.sqrt(this.numIslands.toDouble()))
                while ((this.numIslands % rows).toInt() != 0)
                    rows -= 1

                val columns = (this.numIslands / rows).toInt()

                this.islands = arrayOfNulls(rows.toInt())

                for (row in 0 until rows.toInt()) {
                    this.islands[row] = Array(columns) { Island(environment, dataset) }
                }
            }

            /**
             * Allows a row of islands to be retrieved using the `grid[i]` syntax.
             */
            operator fun get(i: Int): Array<Island<TProgram, TOutput>> {
                return this.islands[i]!!
            }

            /**
             * Allows a particular island to be retrieved using the `grid[i][j]` syntax.
             */
            operator fun get(i: Int, j: Int): Island<TProgram, TOutput> {
                return this[i][j]
            }

            /**
             * Number of rows in the grid.
             */
            fun rows() = this.islands.size

            /**
             * Number of columns in the grid.
             */
            fun columns() = this[0].size
        }

        /**
         * An individual island.
         *
         * Each island has its own population and is essentially just an implementation of the
         * [SteadyState] algorithm. The main difference is that an island can be evolved for a
         * set number of generations at a time, each time starting from the state that the previous
         * call left it in.
         *
         * @suppress
         */
        class Island<TProgram, TOutput : Output<TProgram>> {

            val environment: Environment<TProgram, TOutput>
            val dataset: Dataset<TProgram>

            constructor(environment: Environment<TProgram, TOutput>, dataset: Dataset<TProgram>) {
                this.environment = environment
                this.dataset = dataset
                this.select = this.environment.registeredModule(
                        CoreModuleType.SelectionOperator
                )
                this.combine = this.environment.registeredModule(
                        CoreModuleType.RecombinationOperator
                )
                this.microMutate = this.environment.registeredModule(
                        CoreModuleType.MicroMutationOperator
                )
                this.macroMutate = this.environment.registeredModule(
                        CoreModuleType.MacroMutationOperator
                )
                this.fitnessEvaluator = FitnessEvaluator()
                this.random = this.environment.randomState

                this.initialise()
            }

            lateinit var individuals: MutableList<Program<TProgram, TOutput>>

            private val select: SelectionOperator<TProgram, TOutput>

            private val combine: RecombinationOperator<TProgram, TOutput>

            private val microMutate: MutationOperator<TProgram, TOutput>

            private val macroMutate: MutationOperator<TProgram, TOutput>

            private val fitnessEvaluator: FitnessEvaluator<TProgram, TOutput>

            var bestIndividual: Program<TProgram, TOutput>? = null

            val random: Random

            private fun initialise() {
                val programGenerator: ProgramGenerator<TProgram, TOutput> = this.environment.registeredModule(
                    CoreModuleType.ProgramGenerator
                )

                this.individuals = programGenerator.next()
                        .take(this.environment.configuration.populationSize)
                        .toMutableList()
            }

            /**
             * Evolves the population for [numGenerations] generations.
             */
            fun evolve(numGenerations: Int) {
                // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
                // 1. Initialise a population of random programs
                //this.initialise()

                // Determine the initial fitness of the individuals in the population
                val initialEvaluations = this.individuals.map { individual ->
                    this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
                }.toList()

                var best = initialEvaluations.sortedBy(Evaluation<TProgram, TOutput>::fitness).first()
                this.bestIndividual = best.individual

                (0 until numGenerations).forEach { _ ->
                    val children = this.select.select(this.individuals)

                    children.pairwise().map { (mother, father) ->
                        // Combine mother and father with some prob.
                        if (random.nextDouble() < this.environment.configuration.crossoverRate) {
                            this.combine.combine(mother, father)
                        }

                        // Mutate mother or father (or both) with some prob.
                        if (random.nextDouble() < this.environment.configuration.microMutationRate) {
                            this.microMutate.mutate(mother)
                        } else if (random.nextDouble() < this.environment.configuration.macroMutationRate) {
                            this.macroMutate.mutate(mother)
                        }

                        if (random.nextDouble() < this.environment.configuration.microMutationRate) {
                            this.microMutate.mutate(father)
                        } else if (random.nextDouble() < this.environment.configuration.macroMutationRate) {
                            this.macroMutate.mutate(father)
                        }
                    }

                    // TODO: Do validation step
                    val evaluations = children.map { individual ->
                        this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
                    }.sortedBy(Evaluation<TProgram, TOutput>::fitness)

                    val bestChild = evaluations.first()

                    best = if (bestChild.fitness < best.fitness) bestChild else best
                    this.bestIndividual = best.individual

                    // The children are copies of individuals in the population, so add the copies
                    // to the population.
                    this.individuals.addAll(children)
                }

                this.bestIndividual = best.individual
            }
        }

        override val information: ModuleInformation
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

        override fun train(dataset: Dataset<TProgram>): EvolutionResult<TProgram, TOutput> {
            val random = this.environment.randomState

            // Create a grid of islands. The grids are populated so that each island has a set of
            // neighbours with which individuals can migrate to.
            this.islands = IslandGrid(
                    numIslands = this@IslandMigration.options.numIslands,
                    environment = this.environment,
                    dataset = dataset
            )

            var bestIndividuals = mutableListOf<Program<TProgram, TOutput>>()

            (0 until this@IslandMigration.islands.rows()).map { row ->
                (0 until this@IslandMigration.islands.columns()).map { col ->
                    val island = this@IslandMigration.islands[row][col]

                    val sortedIslandIndividuals = island.individuals.sortedBy { it.fitness }

                    val best = sortedIslandIndividuals.first()

                    bestIndividuals.add(best)
                }
            }

            var best = bestIndividuals.sortedBy(Program<TProgram, TOutput>::fitness).first()

            var individuals = mutableListOf<Program<TProgram, TOutput>>()

            (0 until this@IslandMigration.islands.rows()).map { row ->
                (0 until this@IslandMigration.islands.columns()).map { col ->
                    val island = this@IslandMigration.islands[row][col]

                    individuals.addAll(island.individuals)
                }
            }

            // We've now got a grid of islands ready to start evolving. For each island we will run the
            // evolution process for a set number of generations before stopping to do migration.
            var generation = 0

            val statistics = mutableListOf<EvolutionStatistics>()

            while (generation < this@IslandMigration.environment.configuration.generations) {
                // Stop early whenever we can.
                if (best.fitness <= this.environment.configuration.stoppingCriterion) {
                    // Make sure to add at least one set of statistics.
                    statistics.add(this.statistics(generation, this.fitnessEvaluator.evaluate(best, dataset, this.environment)))

                    return EvolutionResult(best, individuals, statistics)
                }

                (0 until this@IslandMigration.islands.rows()).map { row ->
                    (0 until this@IslandMigration.islands.columns()).map { col ->
                        val island = this@IslandMigration.islands[row][col]

                        thread {
                            island.evolve(this@IslandMigration.options.migrationInterval)

                            return@thread
                        }
                    }
                }.forEach { jobs -> jobs.forEach { it.join() } }

                // We've just run a set number of generations so we need to migrate a set
                // amount of individuals between the islands and update the generation count.
                (0 until this@IslandMigration.options.migrationSize).forEach { _ ->
                    // Choose a random island
                    val i = random.randInt(0, this@IslandMigration.islands.rows() - 1)
                    val j = random.randInt(0, this@IslandMigration.islands.columns() - 1)

                    val island = this@IslandMigration.islands[i][j]

                    // Choose a random neighbour of this island
                    val neighbours = listOf(i - 1, i, i + 1).map { x ->
                        listOf(j - 1, j, j + 1).map { y ->
                            Pair(x, y)
                        }
                    }.flatten().filter { (x, y) ->
                        x in (0 until this@IslandMigration.islands.rows()) &&
                                y in (0 until this@IslandMigration.islands.columns()) &&
                                (x != i || y != j)
                    }.toList()

                    val neighbourCoords = random.choice(neighbours)
                    val neighbour = this@IslandMigration.islands[neighbourCoords.first][neighbourCoords.second]

                    // Do migration: in our case we simply replace the worst (least fit) individual in the population
                    // with the best individual from another population. We also sort the individuals in an island
                    // such that the least fit individual is last.
                    val sortedNeighbourIndividuals = neighbour.individuals.sortedBy(Program<TProgram, TOutput>::fitness)
                    val sortedIslandIndividuals = island.individuals.sortedBy(Program<TProgram, TOutput>::fitness)

                    val toRemove = sortedNeighbourIndividuals.last()
                    val toCopy = sortedIslandIndividuals.first()

                    // Double check that the individual to be removed is worse (or at least no better) than the migrant.
                    assert(toRemove.fitness >= toCopy.fitness)

                    neighbour.individuals.remove(toRemove)
                    neighbour.individuals.add(toCopy.copy())
                }

                generation += this@IslandMigration.options.migrationInterval

                individuals = mutableListOf<Program<TProgram, TOutput>>()

                (0 until this@IslandMigration.islands.rows()).map { row ->
                    (0 until this@IslandMigration.islands.columns()).map { col ->
                        val island = this@IslandMigration.islands[row][col]

                        individuals.addAll(island.individuals)
                    }
                }

                bestIndividuals = mutableListOf<Program<TProgram, TOutput>>()

                (0 until this@IslandMigration.islands.rows()).map { row ->
                    (0 until this@IslandMigration.islands.columns()).map { col ->
                        val island = this@IslandMigration.islands[row][col]

                        val sortedIslandIndividuals = island.individuals.sortedBy { it.fitness }

                        val best = sortedIslandIndividuals.first()

                        bestIndividuals.add(best)
                    }
                }

                best = bestIndividuals.sortedBy(Program<TProgram, TOutput>::fitness).first()

                statistics.add(this.statistics(generation, this.fitnessEvaluator.evaluate(best, dataset, this.environment)))

            }

            // We've reached the maximum number of generations, so choose the best individual from
            // all of the islands as our overall best.

            return EvolutionResult(best, individuals, statistics)
        }

        private fun statistics(generation: Int, best: Evaluation<TProgram, TOutput>): EvolutionStatistics {
            var meanFitness = 0.0
            var meanProgramLength = 0.0
            var meanEffectiveProgramLength = 0.0
            val bestFitness = best.fitness
            val individuals = mutableListOf<Program<TProgram, TOutput>>()

            (0 until this@IslandMigration.islands.rows()).map { row ->
                (0 until this@IslandMigration.islands.columns()).map { col ->
                    val island = this@IslandMigration.islands[row][col]

                    individuals.addAll(island.individuals)
                }
            }

            individuals.forEach { individual ->
                meanFitness += individual.fitness
                meanProgramLength += individual.instructions.size
                meanEffectiveProgramLength += individual.effectiveInstructions.size
            }

            meanFitness /= individuals.size
            meanProgramLength /= individuals.size
            meanEffectiveProgramLength /= individuals.size

            // Use the mean that we've already calculated.
            val standardDeviation = individuals.map(Program<TProgram, TOutput>::fitness).standardDeviation(meanFitness)

            return EvolutionStatistics(
                    data = mapOf(
                            "generation" to generation,
                            "bestFitness" to bestFitness,
                            "meanFitness" to meanFitness,
                            "standardDeviationFitness" to standardDeviation,
                            "meanProgramLength" to meanProgramLength,
                            "meanEffectiveProgramLength" to meanEffectiveProgramLength
                    )
            )
        }

        override fun test(dataset: Dataset<TProgram>): TestResult<TProgram, TOutput> {
            throw NotImplementedError("Testing the model has not been implemented for IslandMigration")
        }

        override fun copy(): EvolutionModel<TProgram, TOutput> {
            return IslandMigration(this.environment, this.options)
        }

        override fun deepCopy(): EvolutionModel<TProgram, TOutput> {
            return IslandMigration(this.environment.copy(), this.options)
        }
    }
}

// Extension methods for various functionality that is nice to have.
fun List<Double>.standardDeviation(mean: Double = this.average()): Double {
    val variance = this.map { x -> Math.pow(x - mean, 2.0) }.sum()

    return Math.pow((variance / this.size), 0.5)
}

fun <T, R> List<T>.pmap(transform: (T) -> R): List<R> {
    return this.parallelStream().map(transform).toList()
}

fun <T> List<T>.pairwise(): List<Pair<T, T>> {
    return (0 until this.size step 2).map { idx ->
        Pair(this[idx], this[idx + 1])
    }
}
