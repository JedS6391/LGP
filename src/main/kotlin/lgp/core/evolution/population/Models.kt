package lgp.core.evolution.population

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import lgp.core.environment.CoreModuleType
import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.fitness.Evaluation
import lgp.core.evolution.fitness.FitnessEvaluator
import lgp.core.modules.ModuleInformation
import java.util.*
import kotlin.concurrent.thread
import kotlin.streams.toList

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
    class SteadyState<TProgram>(environment: Environment<TProgram>) : EvolutionModel<TProgram>(environment) {

        private val select: SelectionOperator<TProgram> = this.environment.registeredModule(
                CoreModuleType.SelectionOperator
        )

        private val combine: RecombinationOperator<TProgram> = this.environment.registeredModule(
                CoreModuleType.RecombinationOperator
        )

        private val microMutate: MutationOperator<TProgram> = this.environment.registeredModule(
                CoreModuleType.MicroMutationOperator
        )

        private val macroMutate: MutationOperator<TProgram> = this.environment.registeredModule(
                CoreModuleType.MacroMutationOperator
        )

        private val fitnessEvaluator: FitnessEvaluator<TProgram> = FitnessEvaluator()

        lateinit var individuals: MutableList<Program<TProgram>>

        lateinit var bestProgram: Program<TProgram>

        private fun initialise() {
            val programGenerator: ProgramGenerator<TProgram> = this.environment.registeredModule(CoreModuleType.ProgramGenerator)

            this.individuals = programGenerator.next()
                    .take(this.environment.config.populationSize)
                    .toMutableList()
        }

        override fun train(dataset: Dataset<TProgram>): EvolutionResult<TProgram> {
            val rg = Random()

            // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
            // 1. Initialise a population of random programs
            this.initialise()

            // Determine the initial fitness of the individuals in the population
            val initialEvaluations = this.individuals.pmap { individual ->
                this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
            }.toList()

            var best = initialEvaluations.sortedBy(Evaluation<TProgram>::fitness).first()
            this.bestProgram = best.individual

            val statistics = mutableListOf<EvolutionStatistics>()

            (0..this.environment.config.generations - 1).forEach { gen ->
                // Stop early whenever we can.
                // TODO: Make this configurable based on some threshold.
                if (best.fitness == 0.0) {
                    // Make sure to add at least one set of statistics.
                    statistics.add(this.statistics(gen, best))

                    this.bestProgram = best.individual

                    return EvolutionResult(best.individual, this.individuals, statistics)
                }

                val children = this.select.select(this.individuals)

                children.pairwise().pmap { (mother, father) ->
                    // Combine mother and father with some prob.
                    if (rg.nextDouble() < this.environment.config.crossoverRate) {
                        this.combine.combine(mother, father)
                    }

                    // Mutate mother or father (or both) with some prob.
                    if (rg.nextDouble() < this.environment.config.microMutationRate) {
                        this.microMutate.mutate(mother)
                    } else if (rg.nextDouble() < this.environment.config.macroMutationRate) {
                        this.macroMutate.mutate(mother)
                    }

                    if (rg.nextDouble() < this.environment.config.microMutationRate) {
                        this.microMutate.mutate(father)
                    } else if (rg.nextDouble() < this.environment.config.macroMutationRate) {
                        this.macroMutate.mutate(father)
                    }
                }

                // TODO: Do validation step
                val evaluations = children.pmap { individual ->
                    this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
                }.sortedBy(Evaluation<TProgram>::fitness)

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

        private fun statistics(generation: Int, best: Evaluation<TProgram>): EvolutionStatistics {
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
            val standardDeviation = this.individuals.map(Program<TProgram>::fitness).standardDeviation(meanFitness)

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

        override fun test(dataset: Dataset<TProgram>): TestResult<TProgram> {
            this.bestProgram.findEffectiveProgram()

            val outputs = dataset.inputs.map { features ->
                // Reset registers.
                this.bestProgram.registers.reset()

                // Load the features
                this.bestProgram.registers.writeInstance(features)

                // Run the program...
                this.bestProgram.execute()

                // Collect output
                this.bestProgram.registers.read(this.bestProgram.outputRegisterIndex)
            }

            return TestResult(
                    predicted = outputs,
                    expected = dataset.outputs
            )
        }

        override val information = ModuleInformation("Algorithm 2.1 (LGP Algorithm)")

        override fun copy(): SteadyState<TProgram> {
            return SteadyState(this.environment)
        }
    }

    class IslandMigration<TProgram>(
            environment: Environment<TProgram>,
            val options: IslandMigrationOptions
    ) : EvolutionModel<TProgram>(environment) {

        lateinit var islands: IslandGrid<TProgram>

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

        class IslandGrid<TProgram> {
            val islands: Array<Array<Island<TProgram>>?>
            val numIslands: Int

            constructor(numIslands: Int, environment: Environment<TProgram>, dataset: Dataset<TProgram>) {
                this.numIslands = numIslands

                // Compute grid dimensions
                var rows = Math.floor(Math.sqrt(this.numIslands.toDouble()))
                while ((this.numIslands % rows).toInt() != 0)
                    rows -= 1

                val columns = (this.numIslands / rows).toInt()

                this.islands = arrayOfNulls(rows.toInt())

                for (row in 0..rows.toInt() - 1) {
                    this.islands[row] = Array(columns, { Island(environment, dataset) })
                }
            }

            operator fun get(i: Int): Array<Island<TProgram>> {
                return this.islands[i]!!
            }

            operator fun get(i: Int, j: Int): Island<TProgram> {
                return this[i][j]
            }

            fun rows() = this.islands.size


            fun columns() = this[0].size
        }

        class Island<TProgram>(val environment: Environment<TProgram>, val dataset: Dataset<TProgram>) {

            lateinit var individuals: MutableList<Program<TProgram>>

            private val select: SelectionOperator<TProgram> = this.environment.registeredModule(
                    CoreModuleType.SelectionOperator
            )

            private val combine: RecombinationOperator<TProgram> = this.environment.registeredModule(
                    CoreModuleType.RecombinationOperator
            )

            private val microMutate: MutationOperator<TProgram> = this.environment.registeredModule(
                    CoreModuleType.MicroMutationOperator
            )

            private val macroMutate: MutationOperator<TProgram> = this.environment.registeredModule(
                    CoreModuleType.MacroMutationOperator
            )

            private val fitnessEvaluator: FitnessEvaluator<TProgram> = FitnessEvaluator()

            var bestIndividual: Program<TProgram>? = null

            val random = Random()

            private fun initialise() {
                val programGenerator: ProgramGenerator<TProgram> = this.environment.registeredModule(CoreModuleType.ProgramGenerator)

                this.individuals = programGenerator.next()
                        .take(this.environment.config.populationSize)
                        .toMutableList()
            }

             fun evolve(numGenerations: Int) {
                // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
                // 1. Initialise a population of random programs
                this.initialise()

                // Determine the initial fitness of the individuals in the population
                val initialEvaluations = this.individuals.map { individual ->
                    this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
                }.toList()

                var best = initialEvaluations.sortedBy(Evaluation<TProgram>::fitness).first()
                this.bestIndividual = best.individual

                (0..numGenerations - 1).forEach { gen ->
                    val children = this.select.select(this.individuals)

                    children.pairwise().map { (mother, father) ->
                        // Combine mother and father with some prob.
                        if (random.nextDouble() < this.environment.config.crossoverRate) {
                            this.combine.combine(mother, father)
                        }

                        // Mutate mother or father (or both) with some prob.
                        if (random.nextDouble() < this.environment.config.microMutationRate) {
                            this.microMutate.mutate(mother)
                        } else if (random.nextDouble() < this.environment.config.macroMutationRate) {
                            this.macroMutate.mutate(mother)
                        }

                        if (random.nextDouble() < this.environment.config.microMutationRate) {
                            this.microMutate.mutate(father)
                        } else if (random.nextDouble() < this.environment.config.macroMutationRate) {
                            this.macroMutate.mutate(father)
                        }
                    }

                    // TODO: Do validation step
                    val evaluations = children.map { individual ->
                        this.fitnessEvaluator.evaluate(individual, dataset, this.environment)
                    }.sortedBy(Evaluation<TProgram>::fitness)

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

        override fun train(dataset: Dataset<TProgram>): EvolutionResult<TProgram> {
            val random = Random()

            // Create a grid of islands. The grids are populated so that each island has a set of
            // neighbours with which individuals can migrate to.
            this.islands = IslandGrid(
                    numIslands = this@IslandMigration.options.numIslands,
                    environment = this.environment,
                    dataset = dataset
            )

            println("Initialised grid of sub-populations: dimensions = (${this.islands.rows()}, ${this.islands.columns()})")

            // We've now got a grid of islands ready to start evolving. For each island we will run the
            // evolution process for a set number of generations before stopping to do migration.
            var generation = 0

            while (generation < this@IslandMigration.environment.config.generations) {

                println("Running evolution for ${this@IslandMigration.options.migrationInterval} generations: generation = $generation")

                (0..this@IslandMigration.islands.rows() - 1).map { row ->
                    (0..this@IslandMigration.islands.columns() - 1).map { col ->
                        val island = this@IslandMigration.islands[row][col]

                        thread {
                            island.evolve(this@IslandMigration.options.migrationInterval)

                            return@thread
                        }
                    }
                }.forEach { jobs -> jobs.forEach { it.join() } }

                // We've just run a set number of generations so we need to migrate a set
                // amount of individuals between the islands and update the generation count.
                (0..this@IslandMigration.options.migrationSize - 1).forEach {
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
                        x in (0..this@IslandMigration.islands.rows() - 1) &&
                                y in (0..this@IslandMigration.islands.columns() - 1) &&
                                x != i && y != j
                    }

                    val neighbourCoords = random.choice(neighbours)
                    val neighbour = this@IslandMigration.islands[neighbourCoords.first][neighbourCoords.second]

                    // Do migration: in our case we simply replace the worst (least fit) individual in the population
                    // with the best individual from another population. We also sort the individuals in an island
                    // such that the least fit individual is last.

                    val sortedNeighbourIndividuals = neighbour.individuals.sortedBy { it.fitness }
                    val sortedIslandIndividuals = island.individuals.sortedBy { it.fitness }

                    val toRemove = sortedNeighbourIndividuals.last()
                    val toCopy = sortedIslandIndividuals.first()

                    // Double check that the individual to be removed is worse (or at least no better) than the migrant.
                    assert(toRemove.fitness >= toCopy.fitness)

                    println("Removing individual from neighbour (fitness = ${toRemove.fitness}) and replacing with " +
                            "individual from random island (fitness = ${toCopy.fitness}")

                    neighbour.individuals.removeAt(neighbour.individuals.lastIndex)
                    neighbour.individuals.add(toCopy)
                }

                generation += this@IslandMigration.options.migrationInterval
            }

            // We've reached the maximum number of generations, so choose the best individual from
            // all of the islands as our overall best.
            val best = (0..this@IslandMigration.islands.rows() - 1).map { row ->
                (0..this@IslandMigration.islands.columns() - 1).map { col ->
                    val island = this@IslandMigration.islands[row][col]

                    island.bestIndividual!!
                }
            }.flatten().sortedBy(Program<TProgram>::fitness).first()

            val individuals = (0..this@IslandMigration.islands.rows() - 1).map { row ->
                (0..this@IslandMigration.islands.columns() - 1).map { col ->
                    val island = this@IslandMigration.islands[row][col]

                    island.individuals
                }.flatten()
            }.flatten()

            return EvolutionResult(best, individuals, mutableListOf())
        }

        override fun test(dataset: Dataset<TProgram>): TestResult<TProgram> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun copy(): EvolutionModel<TProgram> {
            return IslandMigration(this.environment, this.options)
        }


    }
}

// Extension methods for various functionality that is nice to have.
fun List<Double>.standardDeviation(mean: Double = this.average()): Double {
    val variance = this.map { x -> Math.pow(x - mean, 2.0) }.sum()
    val stdDev = Math.pow((variance / this.size), 0.5)

    return stdDev
}

fun <T, R> List<T>.pmap(transform: (T) -> R) : List<R> {
    return this.parallelStream().map(transform).toList()
}

fun <T> List<T>.pairwise(): List<Pair<T, T>> {
    return (0..this.size - 1 step 2).map { idx ->
        Pair(this[idx], this[idx + 1])
    }
}