package lgp.core.evolution.population

import lgp.core.environment.CoreModuleType
import lgp.core.environment.Environment
import lgp.core.environment.RegisteredModuleType
import lgp.core.evolution.fitness.Evaluation
import lgp.core.evolution.fitness.FitnessEvaluator
import lgp.core.modules.ModuleInformation
import java.util.*
import kotlin.streams.toList

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

    override fun evolve() {
        val rg = Random()

        // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
        // 1. Initialise a population of random programs
        this.initialise()

        // Determine the initial fitness of the individuals in the population
        val initialEvaluations = this.individuals.map { individual ->
            this.fitnessEvaluator.evaluate(individual, this.environment)
        }

        var best = initialEvaluations.sortedBy(Evaluation<T>::fitness).first()

        (0..this.environment.config.generations - 1).forEach { gen ->
            // Stop early whenever we can.
            // TODO: Make this configurable based on some threshold.
            if (best.fitness == 0.0)
                return

            val children = this.select.select(this.individuals)

            (0..children.size - 1 step 2).map { idx ->
                val mother = children[idx]
                val father = children[idx + 1]

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
            val evaluations = children.map { individual ->
                this.fitnessEvaluator.evaluate(individual, this.environment)
            }

            val bestChild = evaluations.sortedBy(Evaluation<T>::fitness).first()

            best = if (bestChild.fitness < best.fitness) bestChild else best

            // The children are copies of individuals in the population, so add the copies
            // to the population.
            this.individuals.addAll(children)

            this.printStatistics(gen, evaluations, best)
        }

        println("Best Program:")
        println(best.individual)

        best.individual.findEffectiveProgram()

        println("Effective Program:")
        for (instruction in best.individual.effectiveInstructions) {
            println(instruction)
        }
    }

    private fun printStatistics(generation: Int, evaluations: List<Evaluation<T>>, best: Evaluation<T>) {
        val averageFitness = (evaluations.map(Evaluation<T>::fitness).sum() / evaluations.size.toDouble())
        val bestFitness = best.fitness

        println("gen = $generation | avg. fitness = $averageFitness, best fitness = $bestFitness")
    }

    override val information = ModuleInformation("Algorithm 2.1 (LGP Algorithm)")
}