package lgp.core.evolution.population

import lgp.core.environment.CoreModuleType
import lgp.core.environment.Environment
import lgp.core.evolution.fitness.Evaluation
import lgp.core.evolution.fitness.FitnessEvaluator
import lgp.core.modules.ModuleInformation
import java.util.*
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
    class SteadyState<T>(environment: Environment<T>) : EvolutionModel<T>(environment) {

        private val select: SelectionOperator<T> = this.environment.registeredModule(
                CoreModuleType.SelectionOperator
        )

        private val combine: RecombinationOperator<T> = this.environment.registeredModule(
                CoreModuleType.RecombinationOperator
        )

        private val microMutate: MutationOperator<T> = this.environment.registeredModule(
                CoreModuleType.MicroMutationOperator
        )

        private val macroMutate: MutationOperator<T> = this.environment.registeredModule(
                CoreModuleType.MacroMutationOperator
        )

        private val fitnessEvaluator: FitnessEvaluator<T> = FitnessEvaluator()

        lateinit var individuals: MutableList<Program<T>>

        private fun initialise() {
            val programGenerator: ProgramGenerator<T> = this.environment.registeredModule(CoreModuleType.ProgramGenerator)

            this.individuals = programGenerator.next()
                    .take(this.environment.config.populationSize)
                    .toMutableList()
        }

        override fun evolve(): EvolutionResult<T> {
            val rg = Random()

            // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
            // 1. Initialise a population of random programs
            this.initialise()

            // Determine the initial fitness of the individuals in the population
            val initialEvaluations = this.individuals.pmap { individual ->
                this.fitnessEvaluator.evaluate(individual, this.environment)
            }.toList()

            var best = initialEvaluations.sortedBy(Evaluation<T>::fitness).first()

            val statistics = mutableListOf<EvolutionStatistics>()

            (0..this.environment.config.generations - 1).forEach { gen ->
                // Stop early whenever we can.
                // TODO: Make this configurable based on some threshold.
                if (best.fitness == 0.0)
                    return EvolutionResult(best.individual, this.individuals, statistics)

                val children = this.select.select(this.individuals)

                children.pairwise().pmap { (mother, father) ->
                    // Combine mother and father with some prob.
                    if (rg.nextGaussian() < this.environment.config.crossoverRate) {
                        this.combine.combine(mother, father)
                    }

                    // Mutate mother or father (or both) with some prob.
                    if (rg.nextGaussian() < this.environment.config.microMutationRate) {
                        this.microMutate.mutate(mother)
                    } else if (rg.nextGaussian() < this.environment.config.macroMutationRate) {
                        this.macroMutate.mutate(mother)
                    }

                    if (rg.nextGaussian() < this.environment.config.microMutationRate) {
                        this.microMutate.mutate(father)
                    } else if (rg.nextGaussian() < this.environment.config.macroMutationRate) {
                        this.macroMutate.mutate(father)
                    }
                }

                // TODO: Do validation step
                val evaluations = children.pmap { individual ->
                    this.fitnessEvaluator.evaluate(individual, this.environment)
                }

                val bestChild = evaluations.sortedBy(Evaluation<T>::fitness).first()

                best = if (bestChild.fitness < best.fitness) bestChild else best

                // The children are copies of individuals in the population, so add the copies
                // to the population.
                this.individuals.addAll(children)

                statistics.add(this.statistics(gen, best))
            }

            return EvolutionResult(best.individual, this.individuals, statistics)
        }

        private fun statistics(generation: Int, best: Evaluation<T>): EvolutionStatistics {
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
            val standardDeviation = this.individuals.map(Program<T>::fitness).standardDeviation(meanFitness)

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

        override val information = ModuleInformation("Algorithm 2.1 (LGP Algorithm)")

        override fun copy(): SteadyState<T> {
            return SteadyState(this.environment)
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